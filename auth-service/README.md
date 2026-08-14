# auth-service

Service d'authentification d'Ojino. Il gère **l'identité et les sessions**, rien d'autre :
le profil scolaire (niveau, filière, matières, objectifs, disponibilités) appartient au
`user-service`.

## Le principe : on ne se reconnecte jamais

L'application n'est pas une banque. Quelqu'un crée son compte une fois et l'utilise,
comme sur Duolingo ou WhatsApp. Concrètement :

| | access token | refresh token |
|---|---|---|
| durée | 30 min | **1 an** (mobile) / **90 jours** (web) |
| stocké où | mémoire du client | keystore (mobile) / cookie `httpOnly` (web) |
| vérifié comment | signature seule, sans base | consulté en base, révocable |

Le refresh est **glissant** : chaque utilisation en émet un neuf avec une durée pleine.
Quelqu'un qui ouvre l'app ne serait-ce qu'une fois par an ne revoit jamais l'écran de
connexion. L'application rafraîchit toute seule, en silence, quand l'access token
approche de sa fin — l'utilisateur ne voit rien.

Le seul moment où l'on redemande une connexion est la détection d'un vol de token
(voir plus bas).

## Les quatre portes d'entrée

Google, Apple, SMS et email mènent toutes au **même compte**. Si quelqu'un s'inscrit par
Google avec `paul@example.com` puis revient par email, il retombe sur son compte : les
identités se rattachent au lieu de se dupliquer (`User.identities`).

Une règle de sécurité gouverne ce rattachement : il n'a lieu **que si le fournisseur
affirme avoir vérifié l'email**. Sans cette condition, n'importe qui pourrait se déclarer
propriétaire d'une adresse et s'emparer du compte correspondant.

## API

Base : `/api/v1/auth`. Toutes les routes acceptent l'en-tête `X-Client-Type` valant
`mobile` (défaut) ou `web`.

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/register` | Inscription email + mot de passe |
| POST | `/login` | Connexion email + mot de passe |
| POST | `/phone/start` | Envoi d'un code SMS |
| POST | `/phone/verify` | Validation du code (crée le compte si le numéro est inconnu) |
| POST | `/social` | Connexion Google ou Apple |
| POST | `/refresh` | Rafraîchissement silencieux |
| POST | `/logout` | Déconnexion de cet appareil |
| POST | `/logout-all` | Déconnexion partout — demande un access token |
| GET | `/me` | Le compte courant — demande un access token |

Toutes renvoient la même forme :

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "refreshToken": "…",
  "newAccount": true,
  "user": { "id": "…", "email": "…", "providers": ["GOOGLE"] }
}
```

`refreshToken` est **absent en web** (il est dans le cookie `httpOnly`, hors de portée
du JavaScript, donc d'une injection XSS).

`newAccount` vaut `true` quand le compte vient d'être créé : c'est le signal pour lancer
le parcours de création de profil (nom, prénom, âge, photo, niveau, filière, matières,
objectif, difficultés, disponibilités) auprès du `user-service`.

Les erreurs sortent en `application/problem+json` avec un champ `code` stable
(`invalid_credentials`, `otp_throttled`, `email_already_used`…) : les applications
testent ce code, jamais le message.

## Administration

Le back-office vit sous `/api/v1/admin` et exige `ROLE_ADMIN`. La règle est posée sur le
**préfixe**, dans la chaîne de filtres : une route admin ajoutée plus tard reste fermée
même si on oublie de l'annoter.

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/v1/admin/users?q=&page=&size=` | Recherche paginée (email, téléphone, nom) |
| GET | `/api/v1/admin/users/{id}` | Détail d'un compte |
| POST | `/api/v1/admin/users/{id}/disable` | Désactiver — coupe aussi les sessions |
| POST | `/api/v1/admin/users/{id}/enable` | Réactiver |
| PUT | `/api/v1/admin/users/{id}/roles` | Remplacer les rôles |
| GET | `/api/v1/admin/users/{id}/sessions` | Appareils connectés |
| DELETE | `/api/v1/admin/users/{id}/sessions` | Déconnecter partout |

Désactiver un compte **révoque ses sessions dans la foulée** : sans cela il resterait
utilisable jusqu'à l'expiration de son access token.

Deux garde-fous : un administrateur ne peut ni se désactiver, ni se retirer son propre
rôle. Une équipe qui se verrouille hors de son back-office n'a aucun moyen de revenir
sans intervention directe en base.

**Le premier administrateur** se déclare par `OJINO_ADMIN_EMAILS` : ces comptes reçoivent
`ADMIN` à la connexion. Sans cela personne ne pourrait jamais le devenir, puisqu'il faut
déjà l'être pour promouvoir quelqu'un. L'attribution est uniquement additive — retirer
une adresse de la liste ne retire pas le rôle, cela passe par l'API.

## Démarrer

Mongo doit tourner — le service refuse de démarrer sans lui.

```bash
docker run -d -p 27017:27017 --name ojino-mongo mongo:7
./mvnw spring-boot:run
```

Le service écoute sur **8081**.

Essai complet sans fournisseur SMS (`ojino.auth.otp.expose-code=true` renvoie le code
dans la réponse en développement) :

```bash
curl -X POST localhost:8081/api/v1/auth/phone/start \
  -H 'Content-Type: application/json' \
  -d '{"phone":"+237690000000"}'
# -> { "challengeId": "…", "devCode": "042913" }

curl -X POST localhost:8081/api/v1/auth/phone/verify \
  -H 'Content-Type: application/json' \
  -d '{"challengeId":"…","phone":"+237690000000","code":"042913"}'
```

## Configuration

Tout est sous `ojino.auth` dans `application.properties`. À renseigner avant la mise en
ligne :

| Variable d'environnement | Rôle |
|---|---|
| `OJINO_JWT_SECRET` | Secret de signature, **32 caractères minimum**. Obligatoire en production. |
| `MONGODB_URI` | Connexion Mongo. |
| `GOOGLE_CLIENT_IDS` | Client IDs Google, séparés par des virgules — un par plateforme (web, iOS, Android). |
| `APPLE_CLIENT_IDS` | Bundle ID (iOS) et Services ID (web). |
| `OJINO_ADMIN_EMAILS` | Adresses recevant `ROLE_ADMIN` à la connexion. |

Tant que la liste de client IDs d'un provider est vide, ce provider **refuse** les
connexions au lieu d'accepter n'importe quel token. C'est volontaire.

À basculer avant la production :

- `ojino.auth.otp.expose-code` → `false` (sinon le code SMS est renvoyé en clair)
- `ojino.auth.cookie.secure` → `true` (dès que le web est en HTTPS)
- `ojino.auth.cors.allowed-origins` → les vrais domaines

## Comment les autres microservices valident les tokens

Les access tokens sont signés en HS256 avec un secret symétrique. Un autre service
(`user-service`…) valide un token **sans appeler l'auth-service** : il lui suffit de
partager `OJINO_JWT_SECRET` et de se déclarer resource server.

Quand il y aura beaucoup de services, il faudra passer en RS256 avec un endpoint JWKS :
seule une clé publique circulera au lieu d'un secret partagé.

Claims présents : `sub` (id utilisateur), `roles`, et selon le compte `email`, `phone`,
`name`.

## Ce qui protège le service

- **Mots de passe** en BCrypt.
- **Refresh tokens et codes SMS** stockés uniquement sous forme d'empreinte SHA-256 :
  une fuite de la base ne permet pas d'usurper une session.
- **Rotation avec détection de vol** : un refresh token ne sert qu'une fois. S'il est
  rejoué, c'est qu'une copie circule — toute la famille de la session est révoquée.
- **Codes SMS** : 6 chiffres, 5 minutes, 5 essais, 60 secondes entre deux envois.
- **Énumération de comptes** : email inconnu et mot de passe faux renvoient la même
  erreur ; `/phone/start` ne dit jamais si le numéro a déjà un compte.
- **Tokens sociaux** : signature vérifiée via le JWKS du fournisseur, plus l'émetteur
  **et l'audience** — sans ce dernier contrôle, un token valide émis pour une autre
  application serait accepté.
- **CSRF** : le cookie de refresh est en `SameSite=Lax`, ce qui bloque les POST
  inter-sites. C'est ce qui permet de désactiver la protection CSRF sur cette API sans
  état.

## Tests

```bash
./mvnw test
```

62 tests, aucun ne demande de base de données : rotation et détection de vol, cycle de
vie des codes SMS, convergence des identités, signature des JWT, garde-fous du
back-office, câblage du contexte.

Un test de bout en bout avec un vrai Mongo reste à écrire (Testcontainers) — il
couvrirait ce que les doublures ne peuvent pas vérifier, notamment les index uniques.

## Reste à faire

- Vérification d'email (lien de confirmation) et réinitialisation de mot de passe
- Un vrai fournisseur SMS : implémenter `SmsSender`, l'implémentation actuelle écrit le
  code dans les logs
- Limitation de débit sur `/login` (pour l'instant seul l'OTP est limité)
- Endpoint de liste et révocation des sessions actives (les données sont déjà là :
  `userAgent`, `ipAddress`, `issuedAt`)
