# content-service

Le socle pédagogique d'Ojino. Il détient **le référentiel scolaire** : systèmes,
niveaux, filières, matières.

C'est le service le plus structurant du projet — tout ce qui suivra (chapitres, notions,
exercices, progression, grounding de l'IA) s'y accrochera.

## Pourquoi le référentiel est ici

Il vivait au départ dans `user-service`, parce que l'onboarding en avait besoin. Mais
c'est le contenu pédagogique qui en sera le gros consommateur : un chapitre appartient à
un niveau et une matière, une notion à un chapitre.

`user-service` ne stocke désormais que les **codes** choisis par l'élève et appelle ce
service pour les valider.

## Le référentiel est configurable

Niveaux, filières et matières vivent **en base**, pas dans le code : ouvrir l'application
à un nouveau pays revient à insérer des documents, pas à redéployer.

Deux systèmes sont livrés dans `resources/reference/education-systems.json` et chargés au
premier démarrage :

| Code | Système | Niveaux | Filières |
|---|---|---|---|
| `CM-FR` | Cameroun francophone | 6e → Terminale | A4, C, D, E, TI |
| `CM-EN` | Cameroun anglophone | Form 1 → Upper Sixth | Arts, Science, Commercial |

Le chargement est conservateur : **un système déjà présent n'est jamais écrasé**. Les
corrections faites directement dans Mongo, ou les pays ajoutés en production, survivent
aux redéploiements.

## Rien ne se supprime, tout s'archive

Un élément archivé **disparaît des choix proposés mais reste résolvable**. Un profil déjà
rattaché à un niveau retiré continue donc de fonctionner.

C'est une conséquence directe de la séparation des services. Quand le référentiel vivait
avec les profils, un contrôle d'usage suffisait à refuser les suppressions dangereuses.
Le maintenir ici imposerait à ce service d'appeler `user-service` avant chaque
suppression — alors que `user-service` l'appelle déjà pour valider les choix des élèves.
Deux services qui s'appellent mutuellement ne se déploient ni ne se testent séparément.
L'archivage rend la question sans objet.

Un système entier, lui, se **désactive** (`active: false`), avec la même logique.

## API

### Publique — `/api/v1/reference`

Ouverte sans authentification : les écrans d'inscription en ont besoin avant même qu'un
profil existe, et elle ne révèle aucune donnée personnelle. Les éléments archivés n'y
apparaissent jamais — c'est **ici**, et non chez l'appelant, que se décide ce qui reste
choisissable.

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/systems` | Systèmes actifs |
| GET | `/systems/{s}/levels?age=15` | Niveaux, avec `suggested: true` selon l'âge |
| GET | `/systems/{s}/levels/{code}` | Un niveau — **route de validation** |
| GET | `/systems/{s}/levels/{l}/tracks` | Filières de ce niveau |
| GET | `/systems/{s}/subjects?level=&track=` | Matières pertinentes |

`GET /systems/{s}/levels/{code}` répond **404** pour un niveau inconnu et **409** pour un
niveau archivé. C'est ce que `user-service` appelle quand l'élève choisit sa classe, et
il y lit `hasTracks` pour savoir s'il faudra lui demander une filière.

**La suggestion par l'âge n'est jamais un filtre.** Tous les niveaux restent proposés :
un redoublement, une année d'avance ou une reprise d'études à 25 ans doivent rester
saisissables. Si aucun niveau ne correspond à l'âge, c'est le plus proche qui remonte,
pour ne jamais laisser l'écran sans proposition.

### Administration — `/api/v1/admin/reference`

Exige `ROLE_ADMIN`, imposé sur le **préfixe** dans la chaîne de filtres.

| Méthode | Route |
|---|---|
| GET / POST | `/systems` |
| PUT | `/systems/{code}` |
| POST | `/systems/{code}/active?value=false` |
| GET / POST | `/systems/{s}/levels` · `/tracks` · `/subjects` |
| PUT | `/systems/{s}/levels/{code}` · `/tracks/{code}` · `/subjects/{code}` |
| POST | `/systems/{s}/levels/{code}/archived?value=true` (idem tracks, subjects) |

Les listes admin incluent les éléments archivés — sinon on ne pourrait pas les
désarchiver.

L'archivage se pilote par sa **propre route** : une modification de libellé ne peut pas
archiver un élément au passage en glissant un booléen dans le corps.

Une filière ou une matière ne peut pas citer un niveau inexistant : elle existerait en
base sans jamais apparaître nulle part, et personne ne comprendrait pourquoi.

## Démarrer

```bash
docker run -d -p 27017:27017 --name ojino-mongo mongo:7
./mvnw spring-boot:run
```

Le service écoute sur **8083** (auth 8081, user 8082).

## Configuration

| Variable | Rôle |
|---|---|
| `OJINO_JWT_SECRET` | Identique à celui de l'auth-service. Obligatoire en production. |
| `MONGODB_URI` | Connexion Mongo (`ojino_content`). |
| `ojino.reference.seed-on-startup` | Charge les systèmes livrés si absents. |

## Tests

```bash
./mvnw test
```

39 tests, aucun ne demande de base de données : suggestion par l'âge et ses cas limites,
archivé encore résolvable mais plus choisissable, cohérence des filières et matières,
lecture du fichier de référentiel, câblage du contexte.

## Reste à faire

Le référentiel n'est que la première brique. La suite, dans l'ordre :

- **Chapitre** — rattaché à système + niveau + matière, ordonné
- **Notion** — le grain fin, unité de maîtrise à laquelle tout s'accrochera
- **Graphe de prérequis** entre notions — sans lui, « rattraper son retard » n'est pas
  implémentable
- **Ressource** — leçon, fiche, vidéo
- **Exercice** — énoncé, corrigé, difficulté
- **Cycle éditorial** — brouillon / publié
- **Registre de langage par cycle** — de la maternelle à la prépa
