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

`GET /api/v1/reference/systems/CM-FR/levels?age=15` renvoie **tous** les niveaux, en
marquant `suggested: true` sur ceux qui correspondent à l'âge.

C'est une suggestion, **jamais un filtre**. Un redoublement, une année d'avance ou une
reprise d'études après une interruption sont des situations normales, et une liste qui
les rendrait impossibles à saisir serait une liste cassée. Si aucun niveau ne correspond
— quelqu'un de 25 ans qui reprend — c'est le plus proche qui remonte, pour ne jamais
laisser l'écran sans proposition.

## Le référentiel est configurable

Niveaux, filières et matières vivent **en base**, pas dans le code : ouvrir l'application
à un nouveau pays revient à insérer des documents.

Deux systèmes sont livrés dans `resources/reference/education-systems.json` et chargés au
premier démarrage : `CM-FR` (francophone, 6e → Terminale, filières A4/C/D/E/TI) et `CM-EN`
(anglophone, Form 1 → Upper Sixth, Arts/Science/Commercial).

Le chargement est conservateur : **un système déjà présent n'est jamais écrasé**. Les
corrections faites directement dans Mongo, ou les pays ajoutés en production, survivent
aux redéploiements.

## API

Toutes les routes demandent un access token, sauf `/reference/**` qui est public — les
écrans d'inscription en ont besoin avant même qu'un profil existe, et il ne contient
aucune donnée personnelle.

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
| GET | `/api/v1/reference/systems` | Systèmes scolaires |
| GET | `/api/v1/reference/systems/{s}/levels?age=` | Niveaux, avec suggestion |
| GET | `/api/v1/reference/systems/{s}/levels/{l}/tracks` | Filières du niveau |
| GET | `/api/v1/reference/systems/{s}/subjects?level=&track=` | Matières |

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

**Le référentiel scolaire** (`/api/v1/admin/reference`) : c'est ce qui rend le référentiel
réellement configurable. Sans ces routes, ouvrir l'app à un nouveau pays supposerait de
modifier Mongo à la main.

| Méthode | Route |
|---|---|
| GET / POST | `/systems` |
| PUT | `/systems/{code}` |
| POST | `/systems/{code}/active?value=false` |
| GET / POST | `/systems/{s}/levels` · `/tracks` · `/subjects` |
| PUT / DELETE | `/systems/{s}/levels/{code}` · `/tracks/{code}` · `/subjects/{code}` |

Deux protections contre les référentiels cassés :

- **Un système ne se supprime pas, il se désactive.** Les profils qui s'y rattachent
  doivent rester lisibles.
- **Un niveau, une filière ou une matière encore référencé refuse d'être supprimé.**
  L'erreur indique combien de profils sont concernés. Sinon ces profils pointeraient vers
  un code disparu, et rien ne les réparerait.

Une filière ou une matière ne peut pas non plus citer un niveau inexistant : elle
existerait en base sans jamais apparaître nulle part.

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

Le service écoute sur **8082** (l'auth-service sur 8081).

## Configuration

| Variable | Rôle |
|---|---|
| `OJINO_JWT_SECRET` | Identique à celui de l'auth-service. Obligatoire en production. |
| `MONGODB_URI` | Connexion Mongo. |
| `ojino.reference.seed-on-startup` | Charge les systèmes livrés si absents. |
| `ojino.reference.default-system` | Système proposé par défaut. |

## Tests

```bash
./mvnw test
```

51 tests, aucun ne demande de base de données : suggestion par l'âge et ses cas limites,
cascade de réinitialisation au changement de niveau, chevauchement de créneaux, ordre des
étapes, lecture du fichier de référentiel, protections de suppression du back-office,
assemblage des filtres de recherche, câblage du contexte.

## Reste à faire

- Envoi de la photo (le service stocke une URL, pas le fichier)
- Test de bout en bout avec un vrai Mongo (Testcontainers) — notamment les index uniques
- Le `weeklyMinutes` est calculé mais rien ne s'en sert encore : c'est la matière première
  du futur planificateur de révisions
