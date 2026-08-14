# Formation 2 — MongoDB avec Spring Boot

**Objectif :** savoir modéliser, indexer, interroger et exploiter MongoDB comme c'est fait
dans `auth-service`, `user-service` et `content-service`.

Prérequis : la [formation 1](formation1.md), au moins jusqu'à la partie 6.

Même règle que la première : **ouvre le vrai fichier** à chaque notion. Le but n'est pas de
retenir des annotations, c'est de reconnaître le motif et de savoir pourquoi il est là.

---

## Sommaire

| Partie | Sujet |
|---|---|
| 0 | Démarrer la base et la regarder |
| 1 | Penser en documents |
| 2 | Le document côté Java |
| 3 | Les index — la partie qui fait la différence |
| 4 | Les repositories |
| 5 | Requêtes dynamiques : MongoTemplate et Criteria |
| 6 | Pagination et tri |
| 7 | Modéliser : imbriquer ou référencer |
| 8 | Six pièges rencontrés sur ce projet |
| 9 | Exploitation et sécurité |
| 10 | Tester |
| 11 | Exercices |

---

# Partie 0 — Démarrer la base et la regarder

## 0.1 Lancer

```bash
cp .env.example .env
docker compose up -d
docker compose ps          # mongo doit être "healthy"
```

Une seule instance sert les trois services, avec **une base par service** :

| Base | Service | Utilisateur |
|---|---|---|
| `ojino_auth` | auth-service | `auth_service` |
| `ojino_user` | user-service | `user_service` |
| `ojino_content` | content-service | `content_service` |

> **Pourquoi séparer les bases.** Un service ne doit pouvoir toucher qu'à ses données.
> Si `auth-service` est compromis, l'attaquant n'atteint ni les profils scolaires ni le
> référentiel. C'est aussi ce qui rend les services déployables séparément : personne ne
> peut prendre l'habitude de lire la table du voisin.
>
> 📂 Regarde `docker/mongo-init/01-create-service-users.js` : chaque utilisateur reçoit
> `readWrite` sur **sa seule** base.

## 0.2 Regarder les données

En ligne de commande :

```bash
docker exec -it ojino-mongo mongosh -u root -p ojino_root_dev

use ojino_content
show collections
db.education_levels.find({ systemCode: "CM-FR" }).sort({ rank: 1 })
db.education_levels.countDocuments({ cycle: "EARLY_YEARS" })
```

Ou avec une interface web :

```bash
docker compose --profile tools up -d      # http://localhost:8090
```

`mongo-express` n'est pas démarré par défaut — c'est un outil de développement, pas un
composant de l'application.

## 0.3 Les commandes mongosh à connaître

```javascript
db.users.find({ email: "paul@example.com" })      // chercher
db.users.findOne({ _id: "..." })                  // un seul
db.users.countDocuments({ disabled: true })       // compter
db.users.getIndexes()                             // voir les index  ← très utile
db.users.find().sort({ createdAt: -1 }).limit(5)  // les 5 derniers
db.users.explain("executionStats").find({...})    // pourquoi c'est lent
```

Retiens `getIndexes()` et `explain()`. Ce sont les deux qui servent quand ça ralentit.

---

# Partie 1 — Penser en documents

## 1.1 La correspondance mentale

| SQL | MongoDB |
|---|---|
| table | collection |
| ligne | document |
| colonne | champ |
| jointure | document imbriqué, ou deux requêtes |
| schéma imposé par la base | schéma tenu par l'application |

## 1.2 La vraie différence

Ce n'est pas « pas de SQL ». C'est **le document est l'unité de lecture et d'écriture**.

Un `User` du projet, tel qu'il est en base :

```json
{
  "_id": "65f1a3b2...",
  "email": "paul@example.com",
  "emailVerified": false,
  "roles": ["USER"],
  "identities": [
    { "provider": "GOOGLE", "subject": "10923...", "linkedAt": "2026-08-14T09:00:00Z" },
    { "provider": "EMAIL",  "subject": "paul@example.com", "linkedAt": "..." }
  ],
  "createdAt": "2026-08-14T09:00:00Z"
}
```

`identities` est une **liste d'objets dans le même document**. En SQL, il faudrait une
seconde table et une jointure à chaque lecture. Ici, une seule lecture ramène tout.

> **La question à se poser en modélisant :** « qu'est-ce que je lis ensemble ? »
> Les identités d'un compte ne se lisent jamais sans le compte → même document.
> Les sessions d'un compte se lisent parfois seules, se comptent par milliers et expirent →
> collection séparée. Regarde `RefreshToken` : c'est un document à part.

## 1.3 Le schéma existe quand même

MongoDB accepte n'importe quoi. Ça ne veut pas dire qu'on écrit n'importe quoi : le schéma
**vit dans les classes Java**. Une faute de frappe dans un nom de champ ne fait pas
d'erreur en base, elle crée silencieusement un champ inutile.

C'est exactement pourquoi le projet utilise des types partout où c'est possible : `enum`
plutôt que `String` pour `EducationCycle`, `record` immuable pour le référentiel, `Instant`
plutôt que `long`.

---

# Partie 2 — Le document côté Java

## 2.1 Les annotations de base

📂 **Ouvre** `auth-service/.../domain/User.java`

```java
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String email;
}
```

| Annotation | Rôle |
|---|---|
| `@Document(collection = "...")` | associe la classe à une collection |
| `@Id` | l'identifiant, rempli automatiquement à l'insertion |
| `@Field("nom_en_base")` | quand le nom Java doit différer du nom stocké |
| `@Transient` | champ à ne jamais persister |

Nomme toujours la collection explicitement. Sans `collection = "users"`, Spring dérive le
nom de la classe — et renommer la classe renommerait la collection, donc perdrait les
données.

## 2.2 `record` ou `class` ?

Le projet utilise les deux, et le choix suit une règle simple.

**`record`** quand le document ne change pas après création :

📂 `content-service/content-core/.../domain/EducationLevel.java`

```java
@Document(collection = "education_levels")
public record EducationLevel(
        @Id String id,
        @Indexed String systemCode,
        String code,
        String label,
        EducationCycle cycle,
        int rank,
        int typicalAgeMin,
        int typicalAgeMax,
        boolean hasTracks,
        boolean archived) {
}
```

Une modification produit un **nouvel** objet. Regarde `AdminReferenceService.setLevelArchived` :
il reconstruit le record entier. C'est plus verbeux, et c'est le but — on voit exactement
ce qui est réécrit.

**`class`** quand le document évolue au fil de la vie de l'utilisateur :

`User`, `StudentProfile`, `RefreshToken` sont des classes. Un profil se construit étape par
étape, un compte se désactive, un token se fait tourner.

## 2.3 Les enums

```java
private EducationCycle cycle;    // stocké "EARLY_YEARS", "COLLEGE"...
```

Spring Data stocke le **nom** de la constante. Deux conséquences :

- Renommer une constante Java casse les données existantes. Traite les noms d'enum comme
  un contrat.
- Une valeur inconnue en base fait échouer la lecture. C'est une bonne nouvelle : l'erreur
  arrive tout de suite.

> **Ce que ça a apporté sur ce projet.** `cycle` était un `String` libre, avec `"LYCEE"`
> dans un système et `"HIGH_SCHOOL"` dans l'autre pour désigner la même chose. Le passage à
> l'enum a rendu l'incohérence impossible à écrire.

## 2.4 Les dates

```java
private Instant createdAt;        // un point dans le temps, en UTC
private LocalDate birthDate;      // une date sans heure ni fuseau
private LocalTime startTime;      // une heure sans date
```

Ils sont stockés nativement, pas en chaînes. Tu peux donc comparer et trier côté base.

📂 `AdminProfileService` trie sur `updatedAt` directement dans la requête.

---

# Partie 3 — Les index

C'est la partie qui sépare une application qui tient la charge d'une qui s'écroule.

## 3.1 À quoi sert un index

Sans index, retrouver un compte par email oblige MongoDB à **lire toute la collection**.
À 100 documents c'est instantané, à 500 000 c'est plusieurs secondes — sur chaque connexion.

```java
@Indexed(unique = true, sparse = true)
private String email;
```

## 3.2 `unique` : une garantie, pas une optimisation

`unique = true` interdit deux documents avec la même valeur. C'est ce qui empêche
réellement deux comptes avec le même email.

> **Pourquoi le test Java ne suffit pas.** `AccountService.registerWithEmail` vérifie
> `existsByEmail` avant d'insérer. Mais entre ce test et l'insertion, une autre requête peut
> passer. Deux inscriptions simultanées avec le même email franchiraient toutes les deux le
> test. **Seul l'index unique tranche vraiment.**
>
> C'est pourquoi `AccountService.save` rattrape `DuplicateKeyException` : le code gère le
> cas courant, l'index gère la course.

## 3.3 `sparse` : le piège qui coûte cher

`sparse = true` fait ignorer par l'index les documents **où le champ est absent**.

Sans lui : un compte créé par SMS n'a pas d'email. Deux comptes SMS auraient tous deux
`email` absent, donc la même valeur du point de vue de l'index unique — et **le second
serait refusé**. L'application deviendrait incapable de créer plus d'un compte par
téléphone.

Règle : **`unique` sur un champ facultatif exige `sparse`.**

## 3.4 Les index composés

📂 `User.java`

```java
@CompoundIndex(name = "idx_user_identity",
        def = "{'identities.provider': 1, 'identities.subject': 1}",
        unique = true, sparse = true)
```

`1` = ordre croissant, `-1` = décroissant. Ici l'index porte sur deux chemins **dans le
même tableau** — c'est autorisé. Ce qui ne l'est pas, c'est un index composé sur deux
tableaux différents.

Cet index garantit qu'une identité Google donnée ne peut être rattachée qu'à un seul
compte.

**L'ordre des champs compte.** Un index sur `(systemCode, code)` sert une recherche sur
`systemCode` seul, ou sur les deux — mais **pas** sur `code` seul. On appelle ça le
préfixe de l'index.

## 3.5 Les index TTL : le ménage automatique

📂 `RefreshToken.java` et `OtpChallenge.java`

```java
@Indexed(expireAfter = "0s")
private Instant expiresAt;
```

MongoDB supprime le document **de lui-même** quand cette date est atteinte. Les sessions
expirées et les codes SMS périmés ne s'accumulent jamais, sans une ligne de code de
nettoyage.

`expireAfter = "0s"` veut dire « zéro seconde **après** la date du champ ». Pour « une heure
après », ce serait `"1h"`.

⚠️ Le ramasse-miettes tourne environ toutes les 60 secondes : la suppression n'est pas
instantanée. Ne compte jamais dessus pour la sécurité — c'est pourquoi
`RefreshToken.isExpired()` revérifie en Java.

## 3.6 La création des index

```properties
spring.data.mongodb.auto-index-creation=true
```

Sans cette ligne, **les annotations d'index sont ignorées** (Spring Data 3+ ne les crée plus
automatiquement). C'est une source classique d'index absents en production.

> ⚠️ **En production, c'est discutable.** La création d'un index sur une grosse collection
> peut bloquer la base plusieurs minutes, au démarrage d'un service. La pratique
> recommandée est de créer les index par un script de migration maîtrisé, et de laisser
> `auto-index-creation` à `false`. Sur ce projet on l'a activé parce qu'il n'y a pas encore
> de données ; c'est à revoir avant la mise en ligne.

Vérifie toujours ce qui existe réellement :

```javascript
db.users.getIndexes()
```

---

# Partie 4 — Les repositories

## 4.1 L'interface qu'on n'implémente pas

📂 `auth-service/.../repository/UserRepository.java`

```java
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Tu n'écris **aucune** implémentation. Spring Data la génère au démarrage, en lisant le nom
des méthodes.

`MongoRepository<User, String>` — l'entité, puis le type de l'`@Id`. On hérite de `save`,
`findById`, `findAll`, `delete`, `count`…

## 4.2 La grammaire des noms

| Nom de méthode | Requête |
|---|---|
| `findByEmail(String)` | `{ email: ? }` |
| `findBySystemCodeAndCode(String, String)` | `{ systemCode: ?, code: ? }` |
| `findBySystemCodeOrderByRankAsc(String)` | filtre + tri croissant sur `rank` |
| `findByDisabledTrue()` | `{ disabled: true }` |
| `countBySystemCode(String)` | comptage |
| `existsByEmail(String)` | booléen |
| `findFirstByPhoneOrderByCreatedAtDesc(String)` | le plus récent |
| `findBySystemCodeAndProgramCodeOrderBySemesterAscCodeAsc(...)` | deux filtres, deux tris |

⚠️ Le nom doit correspondre **exactement** aux noms des champs Java. Une faute de frappe
fait échouer le **démarrage** de l'application — ce qui est le bon moment pour l'apprendre.

Quand le nom devient illisible, c'est le signal de passer à `@Query` ou `MongoTemplate`.

## 4.3 `@Query` : quand le nom ne suffit plus

📂 `UserRepository.java`

```java
@Query("{ 'identities': { $elemMatch: { 'provider': ?0, 'subject': ?1 } } }")
Optional<User> findByIdentity(AuthProvider provider, String subject);
```

`?0`, `?1` sont les paramètres dans l'ordre. `$elemMatch` cherche un élément du tableau qui
remplit **toutes** les conditions à la fois — sans lui, un document ayant une identité
Google et une identité Apple correspondrait à une recherche « provider Google ET subject
d'Apple ».

Un autre, avec `$or` :

```java
@Query("{ $or: ["
        + " { 'email':       { $regex: ?0, $options: 'i' } },"
        + " { 'phone':       { $regex: ?0, $options: 'i' } },"
        + " { 'displayName': { $regex: ?0, $options: 'i' } } ] }")
Page<User> search(String escapedTerm, Pageable pageable);
```

`$options: 'i'` = insensible à la casse.

⚠️ **Le nom du paramètre est `escapedTerm`, et ce n'est pas décoratif** — voir partie 8.

## 4.4 Les opérateurs Mongo utiles

| Opérateur | Sens |
|---|---|
| `$eq` `$ne` | égal, différent |
| `$gt` `$gte` `$lt` `$lte` | comparaisons |
| `$in` `$nin` | dans / hors d'une liste |
| `$exists` | le champ est présent |
| `$regex` | expression régulière |
| `$and` `$or` `$not` | combinaisons |
| `$elemMatch` | un élément de tableau remplit toutes les conditions |

---

# Partie 5 — Requêtes dynamiques : MongoTemplate

## 5.1 Le problème

Une recherche back-office avec trois filtres **facultatifs et combinables** : nom, système,
niveau. Ça fait huit combinaisons — donc huit méthodes de repository à écrire et à
maintenir.

## 5.2 La solution

📂 `user-service/.../service/profile/AdminProfileService.java`

```java
List<Criteria> filters = new ArrayList<>();

if (isPresent(term)) {
    String escaped = Pattern.quote(term.trim());
    filters.add(new Criteria().orOperator(
            Criteria.where("firstName").regex(escaped, "i"),
            Criteria.where("lastName").regex(escaped, "i")));
}
if (isPresent(systemCode)) {
    filters.add(Criteria.where("systemCode").is(systemCode.trim()));
}

Query query = new Query();
if (!filters.isEmpty()) {
    query.addCriteria(new Criteria().andOperator(filters));
}

long total = mongoTemplate.count(query, StudentProfile.class);
List<StudentProfile> content = mongoTemplate.find(query.with(pageable), StudentProfile.class);

return new PageImpl<>(content, pageable, total);
```

Un seul assemblage remplace les huit méthodes.

## 5.3 Le piège du total

```java
long total = mongoTemplate.count(query, ...);          // AVANT la pagination
List<...> content = mongoTemplate.find(query.with(pageable), ...);
```

`query.with(pageable)` **modifie** l'objet `query` en lui ajoutant `skip` et `limit`.
Compter après donnerait au mieux la taille de la page. C'est testé :
`totalIsCountedBeforePaging`.

## 5.4 Quand utiliser quoi

| Situation | Outil |
|---|---|
| filtre simple et fixe | méthode dérivée |
| requête fixe mais complexe | `@Query` |
| filtres facultatifs, combinables | `MongoTemplate` + `Criteria` |
| agrégations, regroupements, calculs | `Aggregation` (pas encore utilisé ici) |

---

# Partie 6 — Pagination et tri

Ne renvoie **jamais** `findAll()` sur une collection qui grandit.

```java
@GetMapping
public PageResponse<AdminUserResponse> list(
        @RequestParam(required = false) String q,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {

    Page<User> page = adminUsers.search(q, pageable);
    return PageResponse.of(page, AdminUserResponse::from);
}
```

Spring lit `?page=0&size=20&sort=createdAt,desc` et construit le `Pageable`.

> **Pourquoi le projet n'expose pas `Page` directement.** Sa sérialisation reflète une
> structure interne de Spring Data, non garantie d'une version à l'autre. Un contrat d'API
> doit rester stable — d'où `PageResponse`, écrit une fois et générique.

📂 `web/admin/dto/PageResponse.java`

⚠️ **Trier sur un champ non indexé force MongoDB à trier en mémoire**, et il refuse au-delà
de 32 Mo. Si tu paginais sur `updatedAt`, indexe `updatedAt`.

---

# Partie 7 — Modéliser : imbriquer ou référencer

La seule décision vraiment difficile en MongoDB.

## 7.1 Imbriquer

📂 `StudentProfile.java` — `availability`, `difficulties`, `subjectCodes` sont **dans** le
document.

Imbrique quand :
- la donnée ne se lit jamais sans son parent
- elle est bornée (quelques dizaines d'éléments, pas des milliers)
- elle appartient au parent et meurt avec lui

## 7.2 Référencer

📂 `RefreshToken.userId` — une simple chaîne, pas d'objet `User`.

Référence quand :
- la donnée a sa propre vie (une session se révoque seule)
- elle croît sans limite
- on l'interroge indépendamment

## 7.3 Référencer entre services

`StudentProfile.systemCode` vaut `"CM-FR"`, mais le référentiel vit dans **une autre base**,
servie par **un autre service**.

Il n'y a donc aucune intégrité référentielle possible. C'est assumé, et c'est le prix des
microservices. Deux conséquences concrètes dans le projet :

1. **La validation se fait à l'écriture**, par un appel à `content-service`.
   📂 `user-service/.../client/ContentClient.java`
2. **Rien ne se supprime dans le référentiel**, tout s'archive — sinon des profils
   pointeraient vers un code disparu, et rien ne les réparerait.

## 7.4 La dénormalisation assumée

📂 `StudentProfile.levelHasTracks`

Cette information appartient au référentiel. Elle est **recopiée** sur le profil au moment
du choix du niveau.

> **Pourquoi.** `GET /api/v1/onboarding` est appelé à chaque ouverture de l'application et a
> besoin de savoir s'il faut afficher l'étape « filière ». Sans la recopie, chaque lecture
> déclencherait un appel réseau à `content-service`.
>
> **Le prix.** Si un administrateur change `hasTracks`, les profils existants gardent
> l'ancienne valeur jusqu'à ce que leur niveau soit reconfirmé.
>
> **Pourquoi c'est le bon marché** : le changement est rare, la lecture est constante. La
> dénormalisation n'est pas un péché, c'est un arbitrage — mais il doit être **écrit**.
> Regarde le commentaire sur le champ.

---

# Partie 8 — Six pièges rencontrés sur ce projet

Tous sont réels, tous ont coûté du temps.

## 8.1 `mongoTemplate` se connecte à sa création

**Symptôme :** l'application refuse de démarrer sans Mongo, avec un
`MongoTimeoutException` au milieu d'une cascade de `BeanCreationException`.

Contrairement à ce qu'on croit souvent, le driver n'est pas paresseux ici : le bean
`mongoTemplate` teste le serveur dès son instanciation.

**Conséquence pour les tests :** un `@SpringBootTest` échouerait partout où aucune base ne
tourne. D'où, dans les trois services :

```java
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class AuthServiceApplicationTests {

    @MockitoBean
    UserRepository userRepository;
}
```

⚠️ En Boot 4 ces classes ont **changé de package** (`org.springframework.boot.mongodb.autoconfigure`,
`org.springframework.boot.data.mongodb.autoconfigure`). Les noms de Boot 3 ne marchent plus.

## 8.2 `unique` sans `sparse`

Voir 3.3. Le symptôme est cruel : tout marche avec un compte, et le deuxième compte sans
email est refusé sans raison apparente.

## 8.3 L'injection d'expression régulière

```java
return users.search(Pattern.quote(term.trim()), pageable);
```

Sans `Pattern.quote`, le terme saisi par un administrateur part **tel quel** dans un
`$regex`. Deux problèmes :

- un `.` ou un `*` change silencieusement le sens de la recherche
- un terme comme `(a+)+$` déclenche une explosion combinatoire qui **bloque la base**

C'est une faille réelle, pas une précaution théorique. Elle est vérifiée par un test qui
capture l'argument transmis : `searchTermIsEscaped`.

## 8.4 Boot 4 utilise Jackson 3

```java
import tools.jackson.databind.ObjectMapper;      // ✅ Boot 4
import com.fasterxml.jackson.databind.ObjectMapper;  // ❌ n'existe plus
```

Seules les **annotations** sont restées en `com.fasterxml.jackson.annotation`
(`@JsonFormat`, `@JsonInclude`). Presque tous les tutoriels en ligne sont encore en
Jackson 2.

## 8.5 Les index ne se créent pas tout seuls

Voir 3.6. Les annotations sont ignorées sans `auto-index-creation=true`. Vérifie avec
`db.collection.getIndexes()`, jamais en supposant.

## 8.6 L'enum non typé qui dérive

`cycle` était un `String`. Un système écrivait `"LYCEE"`, l'autre `"HIGH_SCHOOL"`, pour la
même chose. Personne ne s'en aperçoit — jusqu'au jour où un filtre par cycle ne remonte que
la moitié des données.

Le passage à `EducationCycle` a rendu l'erreur impossible à écrire. Règle générale :
**tout champ dont les valeurs possibles sont connues à l'avance doit être un enum.**

---

# Partie 9 — Exploitation et sécurité

## 9.1 Ce qu'on ne stocke jamais en clair

| Donnée | Stockage | Pourquoi |
|---|---|---|
| mot de passe | BCrypt | lent volontairement, résiste au cassage |
| refresh token | SHA-256 | déjà 256 bits d'aléatoire, un hachage lent n'apporte rien |
| code SMS | SHA-256 | idem, et il vit 5 minutes |

📂 `security/SecureTokens.java`

Une fuite de la base ne permet donc **ni de se connecter, ni d'usurper une session**.

## 9.2 Un utilisateur par service

Déjà vu en 0.1. Le principe est le **moindre privilège** : chaque service n'a que
`readWrite` sur sa base.

## 9.3 La connexion

```properties
spring.data.mongodb.uri=${MONGODB_URI:mongodb://auth_service:ojino_dev_password@localhost:27017/ojino_auth?authSource=ojino_auth}
```

`authSource` indique **où sont définis les identifiants**. L'oublier est la cause n°1 des
`Authentication failed` : par défaut Mongo cherche l'utilisateur dans `admin`, pas dans la
base cible.

En production, tout passe par `MONGODB_URI` et rien du fichier n'est utilisé.

## 9.4 Surveiller

```
GET /actuator/health
```

Actuator remonte l'état de la connexion Mongo. Les détails ne sont visibles que par un
appelant authentifié (`show-details=when-authorized`) : l'état interne n'a pas à être
public.

## 9.5 Ce qui reste à faire

- **Sauvegardes** (`mongodump`) — rien n'est en place
- **Réplication** — une instance unique aujourd'hui, donc pas de transactions multi-documents
- **Création des index par migration** plutôt que `auto-index-creation`

---

# Partie 10 — Tester

## 10.1 Le parti pris du projet

**134 tests, aucun ne demande MongoDB** (62 + 28 + 44). Les repositories sont remplacés par
des doublures.

```java
Map<String, RefreshToken> stored = new LinkedHashMap<>();
RefreshTokenRepository repository = mock(RefreshTokenRepository.class);

when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
    RefreshToken token = invocation.getArgument(0);
    if (token.getId() == null) {
        token.setId(UUID.randomUUID().toString());
    }
    stored.put(token.getTokenHash(), token);
    return token;
});
when(repository.findByTokenHash(anyString())).thenAnswer(invocation ->
        Optional.ofNullable(stored.get(invocation.getArgument(0))));
```

Une `Map` suffit à simuler la base pour tester des enchaînements complets : créer une
session, la faire tourner, rejouer l'ancien token et vérifier que toute la famille est
révoquée.

**Avantage :** la suite tourne en quelques secondes, partout, sans docker.

## 10.2 Ce que ça ne teste pas — et il faut le dire

Une doublure ne reproduit ni les **index uniques**, ni les **index TTL**, ni le
comportement réel des requêtes `@Query`. Le test de `registerRejectsDuplicateEmail` vérifie
le contrôle applicatif, **pas** la garantie de la base.

C'est un manque assumé, écrit dans les README. La réponse est **Testcontainers** : un vrai
MongoDB éphémère, démarré par le test.

```java
@Testcontainers
@SpringBootTest
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
}
```

`@ServiceConnection` (Boot 3.1+) branche l'URI tout seul — aucune propriété à écrire.

**La bonne combinaison :** des tests unitaires rapides avec doublures pour la logique, une
poignée de tests d'intégration Testcontainers pour ce que seule la base garantit.

---

# Partie 11 — Exercices

**1.** Lance `docker compose up -d`, connecte-toi avec `mongosh` et affiche tous les niveaux
du cycle `EARLY_YEARS` du système `CM-FR`, triés par rang.

**2.** Exécute `db.users.getIndexes()` sur `ojino_auth`. Retrouve l'index composé des
identités et explique pourquoi il est `sparse`.

**3.** Ajoute `List<User> findByEmailVerifiedFalse()` à `UserRepository`. Quelle requête
génère-t-elle ?

**4.** Dans `OtpChallenge`, à quoi sert `@Indexed(expireAfter = "0s")` ? Que se passerait-il
sans ?

**5.** Écris une méthode `AdminProfileService.search` acceptant un filtre supplémentaire
`onboardingComplete` (booléen facultatif). Attention : `false` et « absent » ne veulent pas
dire la même chose.

**6.** Pourquoi `RefreshToken` est-il un document séparé alors que `LinkedIdentity` est
imbriqué dans `User` ? Applique les critères de la partie 7.

**7.** Écris un test Testcontainers qui vérifie qu'insérer deux `User` avec le même email
lève bien une `DuplicateKeyException`. C'est exactement ce que les doublures ne peuvent pas
prouver.

---

## Ce que tu dois savoir faire en sortie

- [ ] Lancer la base, m'y connecter, lire et compter des documents
- [ ] Décider entre imbriquer et référencer, et justifier
- [ ] Écrire un document Java avec ses index, et dire à quoi sert chacun
- [ ] Expliquer `unique`, `sparse`, composé, TTL — et ce qui casse sans eux
- [ ] Choisir entre méthode dérivée, `@Query` et `MongoTemplate`
- [ ] Paginer sans compter le total au mauvais moment
- [ ] Repérer une injection d'expression régulière
- [ ] Écrire un test avec doublure, et dire ce qu'il ne prouve pas
