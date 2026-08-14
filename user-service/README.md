# user-service

Profil scolaire de l'élève et parcours d'inscription. L'identité et les sessions
appartiennent à l'`auth-service` ; ce service ne gère que ce qui vient **après** la
création du compte.

## Le parcours en 8 étapes

Quand l'`auth-service` renvoie `newAccount: true`, l'application lance ce wizard :

| # | Étape | Contenu |
|---|---|---|
| 1 | `IDENTITY` | Nom, prénom, date de naissance |
| 2 | `PHOTO` | Photo de profil — **toujours passable** |
| 3 | `LEVEL` | Système scolaire et classe |
| 4 | `TRACK` | Filière — **sautée** si le niveau n'en a pas |
| 5 | `SUBJECTS` | Matières suivies |
| 6 | `GOAL` | Objectif |
| 7 | `DIFFICULTIES` | Matières qui coincent — une liste vide est valable |
| 8 | `AVAILABILITY` | Jours et créneaux de travail |

Trois principes gouvernent ce parcours :

**Il est piloté par le serveur.** Chaque étape renvoie l'état complet, avec `nextStep`.
Les applications n'ont aucune séquence à coder : elles suivent ce que le serveur indique.
Modifier le parcours ne demande donc pas de republier sur les stores.

**Il est reprenable.** Chaque étape s'enregistre seule. Quelqu'un qui ferme l'app au
milieu retrouve exactement où il en était, et peut revenir modifier une étape déjà
validée sans repasser par les autres.

**Il reste cohérent.** Changer de niveau efface filière, matières et difficultés, et
rouvre les étapes correspondantes — garder des matières de Terminale sur un profil de 5e
produirait un profil incohérent.

## Le niveau suggéré par l'âge

Exposé par `content-service` :
`GET :8083/api/v1/reference/systems/CM-FR/levels?age=15` renvoie **tous** les niveaux, en
marquant `suggested: true` sur ceux qui correspondent à l'âge.

C'est une suggestion, **jamais un filtre**. Un redoublement, une année d'avance ou une
reprise d'études après une interruption sont des situations normales, et une liste qui
les rendrait impossibles à saisir serait une liste cassée. Si aucun niveau ne correspond
— quelqu'un de 25 ans qui reprend — c'est le plus proche qui remonte, pour ne jamais
laisser l'écran sans proposition.

## Le référentiel appartient au content-service

Ce service ne stocke que les **codes** choisis par l'élève (`systemCode`, `levelCode`,
`trackCode`, `subjectCodes`). Savoir si un code est valide, et s'il l'est encore, revient
au `content-service` : c'est lui qui connaît les niveaux archivés, les filières
disponibles par classe et les matières pertinentes.

Les appels n'ont lieu que sur les **écritures** du parcours — huit fois dans la vie d'un
compte. La lecture de l'état d'inscription, elle, n'appelle personne : `hasTracks` est
recopié sur le profil au moment du choix du niveau.

> **Pourquoi cette recopie.** `GET /api/v1/onboarding` est appelé à chaque ouverture de
> l'application. Un appel réseau à cet endroit coûterait cher et rendrait l'écran
> dépendant d'un autre service. La contrepartie est assumée : si un administrateur change
> `hasTracks` sur un niveau, les profils déjà rattachés gardent l'ancienne valeur jusqu'à
> ce que leur niveau soit reconfirmé. C'est un changement rare.

Un `content-service` injoignable renvoie **503** et non 400 : l'élève n'a rien fait de
mal, il peut réessayer tel quel.

## API

Toutes les routes demandent un access token. Les listes de choix de l'inscription
(systèmes, niveaux, filières, matières) sont servies par `content-service`, que les
applications appellent directement.

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/v1/onboarding` | État du parcours + profil |
| PUT | `/api/v1/onboarding/identity` | Étape 1 |
| PUT | `/api/v1/onboarding/photo` | Étape 2 |
| POST | `/api/v1/onboarding/photo/skip` | Passer l'étape 2 |
| PUT | `/api/v1/onboarding/level` | Étape 3 |
| PUT | `/api/v1/onboarding/track` | Étape 4 |
| PUT | `/api/v1/onboarding/subjects` | Étape 5 |
| PUT | `/api/v1/onboarding/goal` | Étape 6 |
| PUT | `/api/v1/onboarding/difficulties` | Étape 7 |
| PUT | `/api/v1/onboarding/availability` | Étape 8 |
| GET | `/api/v1/profile/me` | Le profil seul |

Le profil est identifié par le `sub` de l'access token : il n'y a **aucun identifiant à
passer**, et personne ne peut donc modifier le profil d'un autre.

Chaque étape renvoie la même réponse :

```json
{
  "steps": [ { "step": "TRACK", "applicable": false, "required": true, "completed": false } ],
  "nextStep": "SUBJECTS",
  "completedCount": 3,
  "applicableCount": 7,
  "complete": false,
  "profile": { "age": 16, "levelCode": "TLE", "weeklyMinutes": 180 }
}
```

## Administration

Le back-office vit sous `/api/v1/admin` et exige `ROLE_ADMIN`, imposé sur le **préfixe**
dans la chaîne de filtres — une route ajoutée plus tard reste fermée même sans annotation.

L'administration du référentiel scolaire a suivi le référentiel : elle vit désormais dans
`content-service`, sous `/api/v1/admin/reference`.

**Les profils** (`/api/v1/admin/profiles`) : recherche paginée avec filtres facultatifs
combinables (`q`, `systemCode`, `levelCode`), et détail par identifiant. La vue montre
l'avancement du parcours d'inscription — utile pour voir où les élèves abandonnent.

C'est volontairement **en lecture seule** : corriger un niveau ou une matière se fait
depuis l'application, par l'élève lui-même.

## Démarrer

Mongo doit tourner, et `OJINO_JWT_SECRET` doit être **exactement le même** que celui de
l'`auth-service` — c'est ce partage qui permet de valider un token sans appeler
l'auth-service à chaque requête.

```bash
docker run -d -p 27017:27017 --name ojino-mongo mongo:7
./mvnw spring-boot:run
```

Le service écoute sur **8082** (auth 8081, content 8083).

**`content-service` doit tourner** pour que le parcours d'inscription fonctionne : c'est
lui qui valide les niveaux, filières et matières. Sans lui, la consultation du profil et
de l'état d'inscription marche toujours ; seules les étapes de choix renvoient 503.

## Configuration

| Variable | Rôle |
|---|---|
| `OJINO_JWT_SECRET` | Identique à celui de l'auth-service. Obligatoire en production. |
| `MONGODB_URI` | Connexion Mongo. |
| `CONTENT_SERVICE_URL` | Adresse du content-service (défaut `http://localhost:8083`). |

## Tests

```bash
./mvnw test
```

28 tests, aucun ne demande de base de données ni de content-service : cascade de
réinitialisation au changement de niveau, chevauchement de créneaux, ordre des étapes,
recopie de `hasTracks`, absence d'appel réseau à la lecture de l'état, panne du
content-service remontée en 503, assemblage des filtres de recherche, câblage du contexte.

Le référentiel lui-même a ses propres tests dans `content-service`.

## Reste à faire

- Envoi de la photo (le service stocke une URL, pas le fichier)
- Test de bout en bout avec un vrai Mongo (Testcontainers) — notamment les index uniques
- Le `weeklyMinutes` est calculé mais rien ne s'en sert encore : c'est la matière première
  du futur planificateur de révisions
