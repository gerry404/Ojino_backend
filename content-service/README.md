# content-service

Le socle pédagogique d'Ojino. Il détient le **référentiel scolaire** : systèmes, cycles,
niveaux, filières, matières — et ce qui est propre à chaque cycle.

C'est le service le plus structurant du projet : tout ce qui suivra (chapitres, notions,
exercices, progression, grounding de l'IA) s'y accrochera.

## Un module Maven par cycle

```
content-service/               agrégateur
├── content-core/              vocabulaire partagé + contrat des cycles
├── content-earlyyears/        maternelle → CP
├── content-college/           collège / secondary
├── content-highschool/        lycée / high school
├── content-prepa/             classes préparatoires
├── content-university/        licence, master, doctorat
└── content-app/               l'application, seul module exécutable
```

**Ce n'est pas de la décoration.** Maven refuse à la compilation qu'un module en importe
un autre sans l'avoir déclaré. Chaque module de cycle ne dépend que de `content-core` —
vérifiable avec `./mvnw dependency:tree`. Le lycée ne peut donc pas se mettre à dépendre
de la prépa par inadvertance.

> `content-earlyyears` est destiné à devenir **une application séparée**. Il porte ses
> propres données et son propre chargement, sans rien demander au cœur, et **aucun autre
> module ne dépend de lui**. Le jour de l'extraction, il part tel quel.

### Pourquoi trois modules sont minces

`content-college`, `content-highschool` et `content-prepa` ne contiennent aujourd'hui
qu'un descripteur. C'est délibéré : ces trois cycles partagent tout leur vocabulaire —
niveau, filière, matière — et ce vocabulaire vit dans `content-core`. Le dupliquer trois
fois pour « remplir » les modules aurait été du bruit, pas de l'architecture.

Ils existent pour accueillir ce qui leur est propre : le BEPC et le socle commun pour le
collège, le Probatoire et le Bac pour le lycée, les concours et les colles pour la prépa.

`content-earlyyears` et `content-university`, eux, portent un vrai modèle de données,
parce que leur vocabulaire **rompt** avec celui du secondaire : domaines d'apprentissage
d'un côté, parcours et unités d'enseignement de l'autre.

## Le contrat entre le cœur et les cycles

`content-core` ignore qu'il existe un lycée ou une université. Il ne voit que des
implémentations de `CurriculumModule`, ramassées par Spring au démarrage :

```java
public interface CurriculumModule {
    EducationCycle cycle();
    String label();
    List<CurriculumStep> steps();   // ce que le cycle demande APRÈS la classe
}
```

| Cycle | `steps()` |
|---|---|
| `EARLY_YEARS` | `[LEARNING_DOMAINS]` |
| `COLLEGE` | `[SUBJECTS]` |
| `HIGH_SCHOOL` | `[TRACK, SUBJECTS]` |
| `PREPA` | `[TRACK, SUBJECTS]` |
| `UNIVERSITY` | `[PROGRAM, SEMESTER, COURSE_UNITS]` |

C'est ce qui permet au parcours d'inscription de `user-service` de s'adapter **sans
connaître les cycles** : il lit `steps` sur le niveau choisi et enchaîne.

Le contrat fait un vrai travail, pas seulement de la déclaration. Exemple : les matières
sans restriction de niveau valent partout. Sans garde-fou, un enfant de grande section se
verrait proposer la philosophie. Le cœur interroge le module du cycle, voit qu'il ne
déclare pas `SUBJECTS`, et renvoie une liste vide. C'est testé.

Un cycle présent dans les données mais dont le module Maven n'est pas embarqué est
**masqué** : mieux vaut ne rien proposer qu'ouvrir un parcours que l'application ne saura
pas mener à bout.

## Le référentiel est configurable

Niveaux, filières et matières vivent **en base**, pas dans le code : ouvrir l'application
à un nouveau pays revient à insérer des documents.

| Code | Système | Cycles couverts |
|---|---|---|
| `CM-FR` | Cameroun francophone | maternelle → CP, 6e → 3e, Seconde → Terminale |
| `CM-EN` | Cameroun anglophone | Form 1 → Form 5, Lower/Upper Sixth |

Le chargement est conservateur : **un système déjà présent n'est jamais écrasé**. Les
corrections faites dans Mongo, ou les pays ajoutés en production, survivent aux
redéploiements.

> **Trou assumé** : entre le CP et la 6e, le primaire (CE1 → CM2) n'est pas encore
> couvert. Il sera traité avec le périmètre de l'application maternelle.

## Rien ne se supprime, tout s'archive

Un élément archivé **disparaît des choix proposés mais reste résolvable**. Un profil déjà
rattaché à un niveau retiré continue donc de fonctionner.

C'est une conséquence directe de la séparation des services. Quand le référentiel vivait
avec les profils, un contrôle d'usage suffisait. Le maintenir ici imposerait à ce service
d'appeler `user-service`, qui l'appelle déjà pour valider les choix des élèves — deux
services qui s'appellent mutuellement ne se déploient plus séparément. L'archivage rend la
question sans objet.

## API

### Commune — `/api/v1/reference`

Publique : les écrans d'inscription en ont besoin avant qu'un profil existe.

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/systems` | Systèmes actifs |
| GET | `/systems/{s}/cycles` | Cycles servis, avec leurs `steps` |
| GET | `/systems/{s}/levels?age=15&cycle=COLLEGE` | Niveaux, avec `suggested` |
| GET | `/systems/{s}/levels/{code}` | Un niveau — **route de validation** |
| GET | `/systems/{s}/levels/{l}/tracks` | Filières |
| GET | `/systems/{s}/subjects?level=&track=` | Matières |

`GET /systems/{s}/levels/{code}` répond **404** si inconnu, **409** si archivé, **503** si
le cycle n'est pas servi. C'est ce que `user-service` appelle au choix de la classe, et il
y lit `steps` pour savoir quoi demander ensuite.

**La suggestion par l'âge n'est jamais un filtre.** Redoublement, année d'avance ou reprise
d'études à 25 ans restent saisissables. Si aucun niveau ne correspond, c'est le plus proche
qui remonte.

### Propre à un cycle

| Route | Module |
|---|---|
| `/api/v1/reference/earlyyears/systems/{s}/levels/{l}/domains` | earlyyears |
| `/api/v1/reference/university/systems/{s}/programs` | university |
| `/api/v1/reference/university/systems/{s}/programs/{p}/units?semester=3` | university |

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
| GET / POST / PUT | `/earlyyears/systems/{s}/domains` |

Les listes admin incluent les éléments archivés — sinon on ne pourrait pas les désarchiver.
L'archivage a sa **propre route** : une modification de libellé ne peut pas archiver un
élément au passage.

## Démarrer

```bash
docker compose up -d                        # depuis la racine du dépôt
./mvnw spring-boot:run -pl content-app      # le seul module exécutable
```

Le service écoute sur **8083** (auth 8081, user 8082).

`-pl content-app` est obligatoire : l'agrégateur n'est pas une application.

## Tests

```bash
./mvnw test
```

44 tests, aucun ne demande de base de données. Dont deux qui protègent le découpage :
`everyCycleModuleIsRegistered` échoue si un module cesse d'être ramassé par le scan, et
`earlyYearsHasNoSubjects` vérifie que le contrat des cycles filtre réellement.

## Reste à faire

Le référentiel n'est que la première brique. La suite, dans l'ordre :

- **Chapitre** — rattaché à système + niveau + matière, ordonné
- **Notion** — le grain fin, unité de maîtrise à laquelle tout s'accrochera
- **Graphe de prérequis** entre notions — sans lui, « rattraper son retard » n'est pas
  implémentable
- **Ressource** et **exercice** — leçon, fiche, énoncé, corrigé
- **Cycle éditorial** — brouillon / publié
- **Registre de langage par cycle** — de la maternelle à la prépa
- Back-office CRUD pour `content-university` (lecture seule aujourd'hui)
- Données de prépa (le module est en place, les niveaux n'existent pas encore)
