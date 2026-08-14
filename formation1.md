# Formation 1 — De la POO à Spring Boot

**Objectif :** être capable de lire, comprendre et écrire tout ce qui existe aujourd'hui
dans `auth-service` et `user-service`.

Cette formation part de zéro en Java et en Spring. Elle suppose seulement que tu sais
déjà programmer (variables, boucles, fonctions) dans un langage quelconque.

---

## Comment travailler ce document

**La règle : ouvre le vrai fichier.** Chaque notion renvoie à un fichier du projet.
Le but n'est pas de retenir la théorie, c'est de reconnaître le motif quand tu le
croises dans le code.

Ordre conseillé : lis une partie, ouvre les fichiers cités, fais les exercices de fin de
partie, puis passe à la suivante. Compte 2 à 3 heures par partie.

Les parties 1 à 3 sont du Java pur. Spring n'arrive qu'en partie 4 — c'est volontaire :
**Spring ne s'apprend pas sans Java.** La plupart des gens qui « n'arrivent pas à
comprendre Spring » butent en réalité sur une notion Java qu'ils ont sautée.

**Encadré « Pourquoi »** : à chaque fois que tu vois ce mot, arrête-toi. C'est là qu'est
la vraie compétence. Savoir écrire `@Service` s'apprend en dix secondes ; savoir
*pourquoi* on l'écrit, c'est ce qui distingue quelqu'un qui copie de quelqu'un qui
conçoit.

---

## Sommaire

| Partie | Sujet | Ce que tu sauras faire |
|---|---|---|
| 0 | Installation | Compiler et tester le projet |
| 1 | La POO en Java | Lire toutes les classes du projet |
| 2 | Java moderne | Comprendre streams, Optional, génériques |
| 3 | Maven | Ajouter une dépendance, comprendre le `pom.xml` |
| 4 | Injection de dépendances | Comprendre d'où viennent les objets |
| 5 | API REST | Écrire un endpoint de A à Z |
| 6 | MongoDB | Écrire une entité et son repository |
| 7 | Sécurité | Comprendre la chaîne de filtres et les JWT |
| 8 | Tests | Écrire un test unitaire propre |
| 9 | Lecture guidée | Suivre une requête de bout en bout |
| 10 | Projet final | Ajouter une fonctionnalité complète |

---

# Partie 0 — Vérifier son installation

## 0.1 Ce qu'il te faut

| Outil | Version | Vérifier avec |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | fourni par le projet | `./mvnw -version` |
| MongoDB | 7 | `docker run -d -p 27017:27017 --name ojino-mongo mongo:7` |

Tu n'as pas besoin d'installer Maven : le projet embarque un **wrapper**, les fichiers
`mvnw` (Linux/Mac) et `mvnw.cmd` (Windows). Il télécharge la bonne version tout seul.

> **Pourquoi un wrapper ?** Pour que tous les développeurs d'une équipe compilent avec
> exactement la même version de Maven. Sans ça, « ça marche chez moi » devient une phrase
> qu'on entend tous les jours.

## 0.2 Les trois commandes à connaître

```bash
cd auth-service

./mvnw compile          # compile le code source
./mvnw test             # compile + lance les tests
./mvnw spring-boot:run  # démarre le service
```

Lance `./mvnw test` maintenant. Tu dois voir `BUILD SUCCESS` et 62 tests passés.
Si oui, ton installation est bonne.

---

# Partie 1 — La POO en Java

La programmation orientée objet repose sur une idée simple : **regrouper des données et
le comportement qui va avec**, au lieu de les séparer.

## 1.1 Classe et objet

Une **classe** est un moule. Un **objet** est ce qui sort du moule.

```java
public class User {
    private String email;      // une donnée
    private String phone;
}
```

`User` est la classe. Chaque compte créé dans l'application est un objet `User`
différent, avec son propre email et son propre téléphone.

```java
User paul = new User();   // on fabrique un objet
User marie = new User();  // un autre, indépendant du premier
```

📂 **Ouvre** `auth-service/src/main/java/com/schoolcopilot/auth_service/domain/User.java`

C'est une vraie classe du projet. Tu y retrouves des champs (`email`, `phone`,
`passwordHash`…) et des méthodes.

## 1.2 L'encapsulation

Regarde le mot `private` devant chaque champ. Il interdit l'accès depuis l'extérieur :

```java
User paul = new User();
paul.email = "paul@example.com";   // ❌ ERREUR de compilation
```

On passe obligatoirement par des méthodes :

```java
paul.setEmail("paul@example.com");  // ✅
String email = paul.getEmail();     // ✅
```

Ces méthodes s'appellent **getter** (lire) et **setter** (écrire).

> **Pourquoi s'embêter ?** Parce que le jour où tu veux que tout email soit mis en
> minuscules avant d'être stocké, tu le fais **à un seul endroit** — dans le setter. Si
> cinquante fichiers écrivaient `user.email = ...` directement, tu devrais les corriger
> tous, et tu en oublierais.
>
> C'est exactement ce que fait le projet : dans
> `service/AccountService.java`, tous les emails passent par `normalizeEmail()` avant
> d'être enregistrés. Sans ça, `Paul@Example.com` et `paul@example.com` créeraient deux
> comptes différents.

## 1.3 Le constructeur

Un constructeur, c'est une méthode spéciale qui s'exécute à la création de l'objet. Elle
porte le nom de la classe et n'a pas de type de retour.

```java
public class RefreshToken {
    private String userId;

    public RefreshToken(String userId) {   // constructeur
        this.userId = userId;
    }
}
```

`this` désigne « l'objet en cours de construction ». Ici il sert à distinguer le champ
`this.userId` du paramètre `userId`.

Quand tu n'écris aucun constructeur, Java en fabrique un vide automatiquement. C'est le
cas de `User` dans le projet.

## 1.4 Le `record` : des données immuables

Écrire une classe avec dix champs, dix getters et un constructeur, c'est cent lignes de
code sans intérêt. Java 16 a introduit le `record` :

```java
public record LinkedIdentity(AuthProvider provider, String subject, Instant linkedAt) {
}
```

Cette seule ligne te donne **gratuitement** :
- trois champs privés et **finaux** (non modifiables)
- un constructeur `new LinkedIdentity(provider, subject, linkedAt)`
- trois accesseurs — attention, ils s'appellent `provider()`, pas `getProvider()`
- `equals()`, `hashCode()` et `toString()` corrects

📂 **Ouvre** `domain/LinkedIdentity.java`

```java
public record LinkedIdentity(AuthProvider provider, String subject, Instant linkedAt) {

    public static LinkedIdentity of(AuthProvider provider, String subject) {
        return new LinkedIdentity(provider, subject, Instant.now());
    }
}
```

Un record peut contenir des méthodes. Ici `of(...)` est une **méthode de fabrique** :
elle simplifie la création en remplissant la date toute seule.

### Classe ou record ?

| Utilise un `record` quand… | Utilise une `class` quand… |
|---|---|
| l'objet ne change jamais après création | l'objet évolue au fil du temps |
| c'est un porteur de données | il y a du comportement à encapsuler |

Dans le projet :
- `LinkedIdentity`, `AvailabilitySlot`, `EducationLevel`, tous les DTO → **records**
- `User`, `StudentProfile`, `RefreshToken` → **classes**, parce qu'on les modifie
  (désactiver un compte, valider une étape…)

## 1.5 L'`enum` : une liste fermée de valeurs

```java
public enum AuthProvider {
    EMAIL,
    PHONE,
    GOOGLE,
    APPLE
}
```

📂 **Ouvre** `domain/AuthProvider.java`

Une variable de type `AuthProvider` ne peut valoir que l'une de ces quatre valeurs. Le
compilateur refuse tout le reste.

> **Pourquoi pas juste des `String` ?** Avec des chaînes, `"GOGGLE"` compile très bien et
> plante en production. Avec un enum, la faute de frappe est refusée **à la compilation**.
> C'est la règle générale : plus une erreur est détectée tôt, moins elle coûte cher.

### Un enum peut avoir des champs et des méthodes

📂 **Ouvre** `user-service/.../domain/profile/OnboardingStep.java`

```java
public enum OnboardingStep {

    IDENTITY(1, true),
    PHOTO(2, false),
    LEVEL(3, true),
    // ...

    private final int order;
    private final boolean required;

    OnboardingStep(int order, boolean required) {
        this.order = order;
        this.required = required;
    }

    public int order() { return order; }
    public boolean required() { return required; }
}
```

Chaque étape porte son rang et le fait d'être obligatoire ou non. `PHOTO(2, false)`
signifie « deuxième étape, facultative ».

C'est beaucoup plus solide qu'un `if (step == PHOTO)` disséminé dans le code : l'info vit
**dans** l'enum, à côté de la valeur qu'elle décrit.

## 1.6 L'interface : un contrat

Une interface dit **ce qu'on peut faire**, sans dire **comment**.

📂 **Ouvre** `otp/SmsSender.java`

```java
public interface SmsSender {
    void sendVerificationCode(String phone, String code);
}
```

Aucun code : juste la signature. Ensuite, une classe s'engage à la remplir :

📂 **Ouvre** `otp/LoggingSmsSender.java`

```java
public class LoggingSmsSender implements SmsSender {

    @Override
    public void sendVerificationCode(String phone, String code) {
        log.warn("[SMS SIMULE] Code de verification pour {} : {}", phone, code);
    }
}
```

> **Pourquoi c'est le concept le plus important de cette partie.** Aujourd'hui le projet
> n'a pas de compte Twilio, donc les codes SMS partent dans les logs. Le jour où tu ouvres
> un compte, tu écris une classe `TwilioSmsSender implements SmsSender` et **tu ne touches
> à rien d'autre**. `OtpService` ne sait pas — et n'a pas à savoir — comment le SMS part.
>
> C'est ce qu'on appelle **programmer contre une interface**. Retiens-le : c'est la
> différence entre un code qu'on peut faire évoluer et un code qu'on doit réécrire.

`@Override` n'est pas obligatoire, mais mets-le toujours : si tu te trompes dans la
signature, le compilateur te le dit au lieu de créer silencieusement une méthode qui ne
sera jamais appelée.

## 1.7 L'héritage et les classes abstraites

Google et Apple se vérifient presque pareil. Plutôt que de dupliquer, on met le commun
dans une classe **abstraite** :

📂 **Ouvre** `social/JwksIdentityVerifier.java`

```java
abstract class JwksIdentityVerifier implements SocialIdentityVerifier {

    @Override
    public SocialUser verify(String idToken) {
        // ... code commun : vérifier la signature, l'émetteur, l'audience
        return toSocialUser(decoder().decode(idToken));
    }

    /** Chaque provider traduit ses propres claims. */
    protected abstract SocialUser toSocialUser(Jwt jwt);
}
```

`abstract` sur la classe = on ne peut pas l'instancier directement (`new
JwksIdentityVerifier()` est refusé).
`abstract` sur la méthode = pas de corps, les sous-classes **doivent** la fournir.

📂 **Ouvre** `social/GoogleIdentityVerifier.java`

```java
@Component
public class GoogleIdentityVerifier extends JwksIdentityVerifier {

    @Override
    protected SocialUser toSocialUser(Jwt jwt) {
        return new SocialUser(
                AuthProvider.GOOGLE,
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                readBoolean(jwt, "email_verified"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"));
    }
}
```

`extends` = hérite de. Google et Apple partagent toute la mécanique de vérification et ne
redéfinissent que la traduction des données.

### Interface ou classe abstraite ?

- **Interface** : « ces classes savent faire X ». Aucun code partagé. Une classe peut en
  implémenter plusieurs.
- **Classe abstraite** : « ces classes sont des variantes de X ». Du code partagé. Une
  classe n'en hérite que d'**une seule**.

Dans le doute, commence par une interface.

## 1.8 Le polymorphisme

C'est le fait de manipuler des objets différents à travers le même type.

📂 **Ouvre** `social/SocialVerifiers.java`

```java
@Component
public class SocialVerifiers {

    private final Map<AuthProvider, SocialIdentityVerifier> byProvider = new EnumMap<>(...);

    public SocialVerifiers(List<SocialIdentityVerifier> verifiers) {
        verifiers.forEach(verifier -> byProvider.put(verifier.provider(), verifier));
    }

    public SocialIdentityVerifier forProvider(AuthProvider provider) {
        // ...
    }
}
```

Cette classe reçoit **une liste de `SocialIdentityVerifier`** sans savoir lesquels. Elle
range Google et Apple dans une table, et le reste du code demande simplement « donne-moi
le vérificateur pour GOOGLE ».

Résultat : ajouter Facebook demain, c'est écrire **une seule classe**. Aucune ligne à
modifier ailleurs.

## 1.9 Les exceptions

Une exception, c'est un signal d'erreur qui remonte la pile d'appels jusqu'à ce que
quelqu'un le rattrape.

📂 **Ouvre** `exception/AuthException.java`

```java
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AuthException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                "Identifiants incorrects.");
    }
}
```

Trois choses à noter :

**1. `extends RuntimeException`** — c'est une exception « non vérifiée » : tu n'es pas
obligé de la déclarer avec `throws` ni de l'entourer d'un `try/catch`. C'est le choix
standard pour les erreurs métier.

**2. Les méthodes de fabrique statiques** (`invalidCredentials()`). Au lieu d'écrire
partout `throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "...")`,
on écrit `throw AuthException.invalidCredentials()`. Le message est défini une fois, il
ne peut pas diverger d'un endroit à l'autre.

**3. Le champ `code`.** C'est une chaîne stable que les applications mobiles testent. Le
`message`, lui, peut être retraduit ou reformulé sans rien casser.

Lancer et rattraper :

```java
throw AuthException.invalidCredentials();     // lancer

try {
    faireQuelqueChose();
} catch (AuthException e) {                   // rattraper
    log.error("échec", e);
}
```

Dans le projet, on ne rattrape presque jamais : c'est `GlobalExceptionHandler` qui le fait
au niveau HTTP, une seule fois pour tout le service. On verra ça en partie 5.

## 1.10 `static`, `final`, et les classes utilitaires

**`final`** = non modifiable après affectation.
**`static`** = appartient à la classe, pas à l'objet.

```java
public final class SecureTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokens() {     // constructeur privé : on ne peut pas l'instancier
    }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
```

📂 **Ouvre** `security/SecureTokens.java`

On l'appelle sans jamais créer d'objet :

```java
String token = SecureTokens.randomToken();
```

C'est le motif de la **classe utilitaire** : `final` (personne n'en hérite), constructeur
`private` (personne ne l'instancie), tout en `static`.

Utilise-le pour des fonctions pures — qui ne dépendent d'aucun état. Pour tout le reste,
préfère un vrai objet géré par Spring (partie 4).

## Exercices — Partie 1

**1.** Dans `User.java`, trouve la méthode `linkIdentity`. Explique en une phrase pourquoi
elle vérifie `alreadyLinked` avant d'ajouter.

**2.** Écris un `record Coordonnees(double latitude, double longitude)` avec une méthode
`estValide()` qui vérifie que la latitude est entre -90 et 90.

**3.** Crée une interface `NotificationSender` avec `void send(String destinataire, String
message)`, puis deux implémentations : `EmailSender` et `PushSender`, qui affichent
simplement un message différent.

**4.** Pourquoi `AvailabilitySlot` est-il un `record` alors que `StudentProfile` est une
`class` ? Ouvre les deux fichiers et justifie.

**5.** Ajoute une méthode de fabrique `AuthException.emailNotVerified()` renvoyant un
statut 403 et le code `email_not_verified`.

---

# Partie 2 — Le Java moderne du projet

## 2.1 `Optional` : l'absence assumée

Le problème : en Java, n'importe quelle variable peut valoir `null`, et l'oublier provoque
un `NullPointerException` — l'erreur la plus courante du langage.

`Optional<T>` est une boîte qui contient soit une valeur, soit rien, et qui **oblige** à
traiter le cas vide.

```java
Optional<User> resultat = users.findByEmail("paul@example.com");
```

Trois façons de l'ouvrir :

```java
// 1. Valeur par défaut
User user = resultat.orElse(new User());

// 2. Lever une exception si vide  ← le plus utilisé dans le projet
User user = resultat.orElseThrow(AuthException::invalidCredentials);

// 3. Faire quelque chose seulement si présent
resultat.ifPresent(user -> log.info("trouvé : {}", user.getId()));
```

📂 **Ouvre** `repository/UserRepository.java` — toutes les recherches renvoient un
`Optional`, parce qu'un email peut ne correspondre à aucun compte.

⚠️ **N'appelle jamais `.get()` sans avoir vérifié `.isPresent()`.** Tu perds tout
l'intérêt de l'`Optional`.

## 2.2 Les lambdas

Une lambda, c'est une fonction écrite sur place, sans lui donner de nom.

```java
// Avant : une classe anonyme entière
smsSender = new SmsSender() {
    @Override
    public void sendVerificationCode(String phone, String code) {
        System.out.println(code);
    }
};

// Avec une lambda
SmsSender smsSender = (phone, code) -> System.out.println(code);
```

Ça ne marche que pour les interfaces à **une seule méthode**. Java les appelle des
*interfaces fonctionnelles*.

📂 On en utilise une dans `OtpServiceTest.java` :
```java
SmsSender sender = (phone, code) -> lastSentCode = code;
```

### Les références de méthode

Quand la lambda ne fait qu'appeler une méthode existante, on raccourcit avec `::` :

```java
.map(subject -> Subject.code(subject))   // lambda
.map(Subject::code)                      // référence de méthode, identique
```

## 2.3 Les streams

Un stream est un tapis roulant sur une collection. On enchaîne des opérations, et le
résultat sort à la fin.

```java
List<String> codes = subjects.stream()   // 1. ouvrir le tapis
        .filter(s -> s.core())           // 2. ne garder que le tronc commun
        .map(Subject::code)              // 3. transformer en codes
        .toList();                       // 4. refermer en liste
```

Les trois opérations qui couvrent 90 % des cas :

| Opération | Rôle |
|---|---|
| `filter(...)` | garder les éléments qui remplissent une condition |
| `map(...)` | transformer chaque élément |
| `toList()` | récupérer le résultat en liste |

Et quelques autres utiles :

```java
.anyMatch(s -> s.core())      // vrai si au moins un remplit la condition
.allMatch(s -> s.core())      // vrai si tous la remplissent
.findFirst()                  // renvoie un Optional du premier
.sorted(...)                  // trier
.distinct()                   // dédoublonner
.count()                      // compter
```

Exemple réel, dans `TokenService.java` :

```java
List<RefreshToken> sessions = refreshTokens.findByUserId(userId).stream()
        .filter(token -> token.isUsable(now))
        .sorted((a, b) -> b.getIssuedAt().compareTo(a.getIssuedAt()))
        .toList();
```

Lis-le à voix haute : « prends les tokens de cet utilisateur, garde ceux encore
utilisables, trie du plus récent au plus ancien, donne-moi la liste ». Le code dit
exactement ce qu'il fait — c'est tout l'intérêt.

## 2.4 Les génériques

Les chevrons `<>` permettent d'écrire du code qui marche avec n'importe quel type.

```java
List<String> noms;      // une liste de chaînes
List<User> comptes;     // une liste d'utilisateurs
```

Tu peux en créer :

📂 **Ouvre** `web/admin/dto/PageResponse.java`

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        long totalElements) {

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        // ...
    }
}
```

`<T>` est un type qu'on décidera à l'usage. `PageResponse<AdminUserResponse>` est une page
de comptes, `PageResponse<AdminProfileView>` une page de profils. **Une seule classe pour
les deux**, et le compilateur t'empêche quand même de mélanger.

## 2.5 Les dates et durées

Java a une bonne bibliothèque de temps depuis la version 8. Les quatre types du projet :

| Type | Représente | Exemple |
|---|---|---|
| `Instant` | un point précis dans le temps, en UTC | date de création d'un compte |
| `Duration` | une durée | 30 minutes, 365 jours |
| `LocalDate` | une date sans heure | date de naissance |
| `LocalTime` | une heure sans date | 18h00 |

```java
Instant maintenant = Instant.now();
Instant dans30min = maintenant.plus(Duration.ofMinutes(30));

boolean expire = expiresAt.isBefore(Instant.now());
```

> **Pourquoi `LocalDate` pour la naissance et `Instant` pour la création ?** Une date de
> naissance n'a pas d'heure ni de fuseau : le 3 mars reste le 3 mars partout. Une date de
> création est un instant précis, qu'on veut pouvoir comparer entre serveurs.
>
> Et pourquoi le projet stocke la **date de naissance** au lieu de l'**âge** ? Parce qu'un
> âge stocké devient faux au bout d'un an. C'est écrit dans `StudentProfile.java`, avec la
> méthode `age()` qui le recalcule à chaque fois.

## Exercices — Partie 2

**1.** Écris une méthode qui prend `List<User>` et renvoie la liste des emails des comptes
non désactivés, triés alphabétiquement.

**2.** Dans `ReferenceService.java`, trouve la méthode `levelsFor`. Explique ce que fait
`anyMatch` à cet endroit.

**3.** Transforme cette boucle en stream :
```java
List<String> resultat = new ArrayList<>();
for (Subject s : subjects) {
    if (s.core()) {
        resultat.add(s.label().toUpperCase());
    }
}
```

**4.** Quelle est la différence entre `orElse(x)` et `orElseThrow(...)` ? Dans quel cas
utiliser lequel ?

---

# Partie 3 — Maven

Maven fait trois choses : il télécharge tes dépendances, il compile, il lance les tests.
Tout se décrit dans un fichier `pom.xml`.

## 3.1 Anatomie du `pom.xml`

📂 **Ouvre** `auth-service/pom.xml`

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
</parent>

<groupId>com.schoolcopilot</groupId>
<artifactId>auth-service</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

Toute bibliothèque Java s'identifie par trois coordonnées :
- **groupId** — l'organisation (`org.springframework.boot`)
- **artifactId** — le nom du composant (`spring-boot-starter-security`)
- **version** — (`4.1.0`)

Le bloc `<parent>` hérite de la configuration de Spring Boot. Son plus gros apport : il
connaît les versions compatibles de centaines de bibliothèques. C'est pour ça que les
dépendances du projet n'indiquent **aucune version** — le parent s'en charge.

> **Pourquoi c'est précieux :** Spring Security, Jackson, le driver Mongo et Tomcat
> doivent être dans des versions qui s'entendent. Les accorder à la main est un cauchemar.
> Le parent le fait pour toi.

## 3.2 Les starters

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Un **starter** est un paquet de dépendances cohérent. `starter-security` amène Spring
Security et tout ce dont il a besoin, en versions accordées.

Ceux du projet :

| Starter | Apporte |
|---|---|
| `spring-boot-starter-webmvc` | serveur HTTP + Spring MVC |
| `spring-boot-starter-security` | authentification et autorisation |
| `spring-boot-starter-oauth2-resource-server` | validation des JWT |
| `spring-boot-starter-data-mongodb` | accès MongoDB |
| `spring-boot-starter-validation` | `@NotBlank`, `@Email`… |

⚠️ **Piège spécifique à Boot 4 :** le starter web s'appelle `spring-boot-starter-webmvc`
et non `spring-boot-starter-web`. Et Boot 4 utilise **Jackson 3** : le package est
`tools.jackson.databind`, plus `com.fasterxml.jackson.databind`. Seules les annotations
(`@JsonFormat`, `@JsonInclude`) sont restées en `com.fasterxml`. La plupart des tutoriels
en ligne sont encore en Boot 3 — attention quand tu copies.

## 3.3 Le scope `test`

```xml
<dependency>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

`<scope>test</scope>` = disponible uniquement pour les tests, jamais embarqué dans le
livrable final. Ça garde l'application légère.

## 3.4 L'arborescence standard

Maven impose une structure. Respecte-la, tout marchera tout seul :

```
auth-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/          ← le code
    │   └── resources/     ← application.properties, fichiers JSON…
    └── test/
        └── java/          ← les tests
```

## Exercices — Partie 3

**1.** Ouvre `user-service/pom.xml` et liste les dépendances qu'il a en plus de celles
générées par Spring Initializr.

**2.** Lance `./mvnw dependency:tree` dans `auth-service`. Trouve d'où vient
`jackson-databind`.

**3.** Que se passe-t-il si tu retires `<scope>test</scope>` d'une dépendance de test ?

---

# Partie 4 — Spring Boot : l'injection de dépendances

C'est **le** concept de Spring. Si tu ne dois retenir qu'une partie de cette formation,
c'est celle-ci.

## 4.1 Le problème

`AuthService` a besoin d'un `AccountService`, qui a besoin d'un `UserRepository` et d'un
`PasswordEncoder`. Sans Spring :

```java
UserRepository repo = new UserRepositoryImpl(new MongoClient("mongodb://..."));
PasswordEncoder encoder = new BCryptPasswordEncoder();
AccountService accounts = new AccountService(repo, encoder, properties);
JwtService jwt = new JwtService(encoder2, properties);
TokenService tokens = new TokenService(refreshRepo, properties);
AuthService auth = new AuthService(accounts, otp, verifiers, jwt, tokens);
```

Trois problèmes :
1. C'est long, et il faut respecter l'ordre de construction
2. Chaque classe doit savoir **comment fabriquer** ses dépendances
3. En test, tu veux un faux `UserRepository` — il faut tout re-câbler à la main

## 4.2 La solution : le conteneur

Spring tient un annuaire d'objets, appelés **beans**. Tu déclares ce que tu as besoin, il
te le fournit.

```java
@Service
public class AuthService {

    private final AccountService accounts;
    private final JwtService jwtService;

    public AuthService(AccountService accounts, JwtService jwtService) {
        this.accounts = accounts;
        this.jwtService = jwtService;
    }
}
```

Tu n'écris **jamais** `new AuthService(...)`. Spring voit le constructeur, comprend qu'il
faut un `AccountService` et un `JwtService`, les trouve dans son annuaire, et construit
l'objet pour toi.

C'est ça, l'**injection de dépendances** : la classe déclare ses besoins, quelqu'un
d'autre les satisfait.

## 4.3 Comment un objet devient un bean

Deux façons.

### a) Une annotation stéréotype sur la classe

| Annotation | À utiliser pour |
|---|---|
| `@Service` | la logique métier |
| `@Repository` | l'accès aux données |
| `@RestController` | les points d'entrée HTTP |
| `@Component` | tout le reste |
| `@Configuration` | une classe qui fabrique d'autres beans |

Techniquement elles font toutes la même chose. Elles diffèrent par **l'intention qu'elles
communiquent au lecteur**. Utilise la plus précise.

### b) Une méthode `@Bean` dans une `@Configuration`

Quand la classe ne t'appartient pas (elle vient d'une bibliothèque), tu ne peux pas
l'annoter. Tu écris alors une méthode de fabrique :

📂 **Ouvre** `config/JwtConfig.java`

```java
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey accessTokenKey(AuthProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("...");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey accessTokenKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenKey));
    }
}
```

Remarque : `jwtEncoder` prend en paramètre le `SecretKey` produit juste au-dessus. Spring
comprend l'ordre tout seul.

## 4.4 Toujours injecter par le constructeur

Tu verras parfois cette forme dans de vieux tutoriels :

```java
@Autowired                        // ❌ à éviter
private UserRepository users;
```

Préfère toujours :

```java
private final UserRepository users;   // ✅

public AccountService(UserRepository users) {
    this.users = users;
}
```

> **Pourquoi.** Trois raisons concrètes :
> 1. Le champ peut être `final` — personne ne le remplacera par accident.
> 2. L'objet est **toujours complet** dès sa construction. Avec `@Autowired` sur le champ,
>    il existe un instant où il est là mais pas encore rempli.
> 3. **En test, tu écris juste `new AccountService(fauxRepo, encoder, props)`.** Sans
>    Spring, sans magie. C'est exactement ce que font tous les tests du projet, et c'est
>    pour ça qu'ils tournent en 0,2 seconde.
>
> Depuis Spring 4.3, quand une classe n'a **qu'un seul constructeur**, `@Autowired` est
> même inutile. Regarde le projet : il n'y a pas un seul `@Autowired` dans le code de
> production.

## 4.5 `@SpringBootApplication` et l'autoconfiguration

📂 **Ouvre** `AuthServiceApplication.java`

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

`@SpringBootApplication` cumule trois choses :
- **scanner** le package courant et ses sous-packages pour trouver tes beans
- activer la **configuration automatique**
- permettre de déclarer des beans dans cette classe

⚠️ **Conséquence importante :** Spring ne scanne que le package de cette classe et
ce qui est **dessous**. C'est pourquoi tout le code vit sous
`com.schoolcopilot.auth_service`. Une classe placée ailleurs serait invisible.

**L'autoconfiguration** est la marque de fabrique de Spring Boot : il regarde ce qu'il y a
dans ton classpath et configure en conséquence. Tu as ajouté
`spring-boot-starter-data-mongodb` ? Il crée un `MongoTemplate` tout seul. Tu as ajouté
`starter-security` ? Il installe une chaîne de filtres par défaut.

Tu peux toujours reprendre la main : dès que tu déclares ton propre bean, celui de
l'autoconfiguration s'efface.

## 4.6 La configuration externalisée

Les valeurs qui changent d'un environnement à l'autre ne doivent **jamais** être écrites
en dur dans le code.

📂 **Ouvre** `src/main/resources/application.properties`

```properties
server.port=8081
ojino.auth.jwt.access-token-ttl=30m
ojino.auth.refresh.mobile-ttl=365d
ojino.auth.jwt.secret=${OJINO_JWT_SECRET:dev-secret-a-remplacer...}
```

La syntaxe `${VARIABLE:valeur_par_defaut}` lit une variable d'environnement, avec une
valeur de repli pour le développement.

### Les relire proprement avec `@ConfigurationProperties`

📂 **Ouvre** `config/AuthProperties.java`

```java
@ConfigurationProperties(prefix = "ojino.auth")
public record AuthProperties(
        Jwt jwt,
        Refresh refresh,
        Cookie cookie,
        Otp otp,
        // ...
        ) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl) {
    }

    public record Refresh(Duration mobileTtl, Duration webTtl) {
    }
}
```

Spring lit les propriétés préfixées par `ojino.auth` et remplit ce record. Ensuite,
partout dans le code :

```java
Duration ttl = properties.jwt().accessTokenTtl();
```

Note que `30m` devient automatiquement une `Duration`, et `365d` aussi. La conversion est
faite par Spring.

> **Pourquoi c'est mieux que `@Value("${ojino.auth.jwt.secret}")` sur chaque champ :** tout
> est regroupé, typé, et une faute de frappe dans un nom de propriété se voit au
> démarrage plutôt qu'à l'exécution.

`@ConfigurationPropertiesScan` sur la classe principale active la détection de ces
records.

## Exercices — Partie 4

**1.** Ouvre `service/AuthService.java`. Liste ses cinq dépendances et dis, pour chacune,
comment elle est devenue un bean (annotation stéréotype ou méthode `@Bean`).

**2.** Crée un `@Service` nommé `SaluerService` avec une méthode `saluer(String nom)`.
Injecte-le dans un contrôleur et expose-le sur `/api/v1/salut?nom=Paul`.

**3.** Ajoute une propriété `ojino.auth.otp.length` à 4 dans `application.properties`,
relance les tests, et explique ce qui casse.

**4.** Pourquoi `JwtConfig` déclare-t-il `SecretKey` par une méthode `@Bean` plutôt que
d'annoter une classe ?

---

# Partie 5 — Exposer une API REST

## 5.1 Rappel HTTP

Une requête HTTP, c'est une **méthode**, un **chemin**, des **en-têtes** et parfois un
**corps**.

| Méthode | Usage | Idempotente ? |
|---|---|---|
| `GET` | lire | oui |
| `POST` | créer, ou déclencher une action | non |
| `PUT` | remplacer entièrement | oui |
| `PATCH` | modifier partiellement | non |
| `DELETE` | supprimer | oui |

*Idempotente* = la rejouer dix fois donne le même résultat qu'une fois.

Les codes de statut à connaître :

| Code | Sens |
|---|---|
| 200 | OK |
| 201 | Créé |
| 400 | Requête invalide (la faute du client) |
| 401 | Non authentifié — « je ne sais pas qui tu es » |
| 403 | Interdit — « je sais qui tu es, mais tu n'as pas le droit » |
| 404 | Introuvable |
| 409 | Conflit (ex. email déjà pris) |
| 429 | Trop de requêtes |
| 500 | Erreur du serveur (ta faute) |

La différence 401 / 403 tombe souvent en entretien. Retiens-la.

## 5.2 Le contrôleur

📂 **Ouvre** `web/client/AuthController.java`

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest httpRequest) {
        // ...
    }
}
```

- `@RestController` = un contrôleur dont chaque méthode renvoie directement des données
  (converties en JSON), et non le nom d'une page HTML.
- `@RequestMapping("/api/v1/auth")` = préfixe commun à toutes les routes de la classe.
- `@PostMapping("/login")` = répond à `POST /api/v1/auth/login`.

### Récupérer les données de la requête

| Annotation | Récupère | Exemple |
|---|---|---|
| `@RequestBody` | le corps JSON | `{"email": "..."}` |
| `@PathVariable` | un morceau du chemin | `/users/{userId}` |
| `@RequestParam` | un paramètre de requête | `?q=paul&page=2` |
| `@RequestHeader` | un en-tête | `X-Client-Type: web` |

Exemple avec les trois derniers, dans `AdminUserController.java` :

```java
@GetMapping("/{userId}")
public AdminUserResponse detail(@PathVariable String userId) { ... }

@GetMapping
public PageResponse<AdminUserResponse> list(@RequestParam(required = false) String q) { ... }
```

## 5.3 Les DTO

Un **DTO** (*Data Transfer Object*) est un objet dédié à l'entrée ou à la sortie de l'API.

📂 **Ouvre** `web/client/dto/LoginRequest.java` et `web/client/dto/UserResponse.java`

> **Pourquoi ne pas renvoyer directement l'entité `User` ?** Trois raisons, et chacune
> suffirait :
>
> 1. **Sécurité.** `User` contient `passwordHash`. Le renvoyer, même par accident, est une
>    fuite grave. Regarde `UserResponse` : le champ n'existe tout simplement pas.
> 2. **Stabilité.** Renommer un champ de `User` casserait toutes les applications
>    déployées. Le DTO isole ton modèle interne de ton contrat public.
> 3. **Forme.** L'API veut souvent une donnée calculée (`hasPassword`, `age`) ou aplatie,
>    qui n'a pas de sens dans l'entité.

Le motif de conversion utilisé partout dans le projet — une méthode statique `from` :

```java
public record UserResponse(String id, String email, boolean hasPassword) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash() != null);   // calculé, pas exposé
    }
}
```

## 5.4 La validation

Plutôt que d'écrire des `if` à la main, on annote le DTO :

📂 **Ouvre** `web/client/dto/RegisterRequest.java`

```java
public record RegisterRequest(

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "Format d'email invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 128, message = "Le mot de passe doit faire au moins 8 caracteres.")
        String password) {
}
```

Les annotations utiles :

| Annotation | Vérifie |
|---|---|
| `@NotNull` | non nul |
| `@NotBlank` | non nul, non vide, pas que des espaces |
| `@NotEmpty` | collection non vide |
| `@Email` | format d'email |
| `@Size(min, max)` | longueur |
| `@Min` / `@Max` | valeur numérique |
| `@Past` / `@Future` | date |
| `@Valid` | valider aussi les objets imbriqués |

⚠️ **Rien ne se déclenche sans `@Valid` sur le paramètre du contrôleur :**

```java
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request)
```

Oublie `@Valid`, et toutes tes annotations sont décoratives. C'est une erreur très
fréquente.

## 5.5 La gestion centralisée des erreurs

📂 **Ouvre** `web/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    ProblemDetail handleAuthException(AuthException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.getStatus(), exception.getMessage());
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        // ... transforme les erreurs de validation en réponse lisible
    }
}
```

`@RestControllerAdvice` = « applique-toi à tous les contrôleurs ». Chaque
`@ExceptionHandler` attrape un type d'exception et le convertit en réponse HTTP.

> **Pourquoi c'est un gros gain.** Sans ça, chaque méthode de contrôleur porterait son
> `try/catch`. Ici, `AccountService` lance simplement
> `throw AuthException.invalidCredentials()`, et la conversion en 401 avec le bon corps
> JSON se fait toute seule, une fois pour tout le service.

Note le dernier handler, qui attrape `Exception` :

```java
@ExceptionHandler(Exception.class)
ProblemDetail handleUnexpected(Exception exception) {
    log.error("Erreur inattendue", exception);
    // message générique renvoyé au client
}
```

Le détail technique reste dans les logs. Le renvoyer au client reviendrait à documenter
ton serveur pour un attaquant.

## Exercices — Partie 5

**1.** Ajoute un endpoint `GET /api/v1/auth/ping` qui renvoie `{"status": "ok"}`.

**2.** Crée un DTO `ChangePasswordRequest(String ancien, String nouveau)` avec validation :
les deux obligatoires, le nouveau d'au moins 8 caractères.

**3.** Dans `AuthController`, pourquoi `register` renvoie-t-il `201 CREATED` alors que
`login` renvoie `200 OK` ?

**4.** Fais un appel à `/api/v1/auth/register` avec un email invalide et observe la
réponse. Quel handler l'a produite ?

**5.** Explique pourquoi `AuthController.respond(...)` n'inclut pas le `refreshToken` dans
le corps quand le client est le web.

---

# Partie 6 — La persistance avec MongoDB

## 6.1 Document plutôt que table

MongoDB ne stocke pas des lignes dans des tables, mais des **documents** JSON dans des
**collections**.

| SQL | MongoDB |
|---|---|
| table | collection |
| ligne | document |
| colonne | champ |
| jointure | document imbriqué, ou deux requêtes |

Un `User` du projet ressemble à ça en base :

```json
{
  "_id": "65f1a...",
  "email": "paul@example.com",
  "emailVerified": false,
  "roles": ["USER"],
  "identities": [
    { "provider": "GOOGLE", "subject": "10923...", "linkedAt": "2026-08-14T09:00:00Z" }
  ]
}
```

Remarque `identities` : une **liste d'objets imbriqués** dans le même document. En SQL, ça
demanderait une seconde table et une jointure.

## 6.2 Déclarer un document

📂 **Ouvre** `domain/User.java`

```java
@Document(collection = "users")
@CompoundIndex(name = "idx_user_identity",
        def = "{'identities.provider': 1, 'identities.subject': 1}",
        unique = true, sparse = true)
public class User {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String email;

    private List<LinkedIdentity> identities = new ArrayList<>();
}
```

| Annotation | Rôle |
|---|---|
| `@Document(collection = "...")` | associe la classe à une collection |
| `@Id` | l'identifiant, rempli automatiquement à l'insertion |
| `@Indexed` | crée un index sur ce champ |
| `@CompoundIndex` | index sur plusieurs champs |

**Un index** est une structure qui accélère les recherches. Sans index sur `email`,
retrouver un compte par email obligerait Mongo à parcourir toute la collection.

- `unique = true` → deux documents ne peuvent pas avoir la même valeur. C'est ce qui
  empêche deux comptes avec le même email, **même en cas de requêtes simultanées**.
- `sparse = true` → les documents sans ce champ sont ignorés par l'index. Indispensable
  ici : un compte créé par SMS n'a pas d'email, et sans `sparse` ils entreraient tous en
  collision sur la valeur « absente ».

Un cas particulier utile, dans `RefreshToken.java` :

```java
@Indexed(expireAfter = "0s")
private Instant expiresAt;
```

C'est un **index TTL** : Mongo supprime le document tout seul quand cette date est
atteinte. Les sessions expirées ne s'accumulent jamais.

## 6.3 Les repositories

📂 **Ouvre** `repository/UserRepository.java`

```java
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);
}
```

C'est une **interface** — tu n'écris aucune implémentation. Spring Data la génère au
démarrage, en **lisant le nom des méthodes**.

`findByEmail` → « cherche un document dont le champ `email` vaut le paramètre ».

En héritant de `MongoRepository<User, String>` (l'entité, le type de l'`@Id`), tu obtiens
gratuitement `save`, `findById`, `findAll`, `delete`, `count`…

### La grammaire des noms de méthodes

| Nom | Requête générée |
|---|---|
| `findByEmail(String)` | `email = ?` |
| `findByEmailAndPhone(String, String)` | `email = ? ET phone = ?` |
| `findBySystemCodeOrderByRankAsc(String)` | `systemCode = ?`, trié par `rank` croissant |
| `countBySystemCode(String)` | compte |
| `existsByEmail(String)` | vrai/faux |
| `findFirstByPhoneOrderByCreatedAtDesc(String)` | le plus récent |

⚠️ Le nom doit correspondre **exactement** aux noms des champs. Une faute de frappe fait
échouer le démarrage de l'application — ce qui est une bonne nouvelle : l'erreur arrive
tout de suite, pas en production.

### Quand le nom ne suffit plus : `@Query`

```java
@Query("{ 'identities': { $elemMatch: { 'provider': ?0, 'subject': ?1 } } }")
Optional<User> findByIdentity(AuthProvider provider, String subject);
```

`?0` et `?1` sont les paramètres, dans l'ordre. Ici on cherche dans un tableau imbriqué,
ce que la grammaire des noms ne sait pas exprimer.

## 6.4 Les requêtes dynamiques : `MongoTemplate`

Quand les filtres sont **facultatifs et combinables**, ni les noms de méthodes ni `@Query`
ne conviennent.

📂 **Ouvre** `user-service/.../service/profile/AdminProfileService.java`

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
```

> **Pourquoi ici et pas ailleurs :** trois filtres facultatifs, ça fait huit combinaisons.
> Huit méthodes de repository à écrire et à maintenir. Un seul assemblage de critères les
> remplace toutes.
>
> Et note `Pattern.quote(...)` : sans cet échappement, le terme saisi par l'utilisateur
> serait interprété comme une expression régulière. Un `(a+)+$` bien choisi suffit alors à
> bloquer le serveur. C'est une faille réelle, pas une précaution théorique.

## 6.5 La pagination

Ne renvoie jamais `findAll()` sur une collection qui grandit. Utilise `Pageable` :

```java
@GetMapping
public PageResponse<AdminUserResponse> list(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {

    Page<User> page = adminUsers.search(q, pageable);
    return PageResponse.of(page, AdminUserResponse::from);
}
```

Spring lit `?page=0&size=20&sort=createdAt,desc` dans l'URL et construit le `Pageable`
tout seul.

## Exercices — Partie 6

**1.** Ajoute `List<User> findByDisabledTrue()` à `UserRepository` et devine ce qu'elle
génère.

**2.** Pourquoi l'index sur `email` est-il `sparse` ? Que se passerait-il sans, avec deux
comptes créés par SMS ?

**3.** Dans `OtpChallenge.java`, explique le rôle de `@Indexed(expireAfter = "0s")`.

**4.** Écris une méthode de repository qui compte les profils d'un niveau donné. (Indice :
elle existe déjà dans `StudentProfileRepository`.)

---

# Partie 7 — La sécurité

## 7.1 Authentification et autorisation

- **Authentification** : qui es-tu ? (login, token)
- **Autorisation** : as-tu le droit de faire ça ? (rôles)

401 = échec d'authentification. 403 = échec d'autorisation.

## 7.2 La chaîne de filtres

Spring Security s'insère **avant** tes contrôleurs, sous forme d'une chaîne de filtres.
Chaque requête la traverse :

```
Requête HTTP
    ↓
[ CORS ] → [ CSRF ] → [ lecture du token JWT ] → [ contrôle des autorisations ]
    ↓
Ton contrôleur   (uniquement si tous les filtres ont laissé passer)
```

Si un filtre rejette, ton code n'est jamais atteint.

## 7.3 Configurer la chaîne

📂 **Ouvre** `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(...))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN)
                    .requestMatchers("/api/v1/auth/me").authenticated()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));

        return http.build();
    }
}
```

⚠️ **L'ordre compte énormément.** Les règles sont évaluées de haut en bas, et **la première
qui correspond gagne**. Si `permitAll()` sur `/api/v1/auth/**` était écrit en premier,
`/api/v1/auth/me` deviendrait public.

C'est aussi pour ça que la règle admin est déclarée en tête dans les deux services :
aucune règle plus permissive ne peut la devancer.

### Trois choix expliqués

**`sessionCreationPolicy(STATELESS)`** — pas de session serveur. Chaque requête porte son
token et se suffit à elle-même. C'est ce qui permet de lancer dix instances du service
derrière un load balancer sans se soucier de savoir laquelle a la session.

**`csrf.disable()`** — le CSRF protège les formulaires HTML avec cookie de session. Ici,
API sans état consommée par des applications natives : il n'y a pas de formulaire à
protéger. Le cookie de refresh, lui, est couvert par son attribut `SameSite=Lax` qui
bloque les POST inter-sites.

**`cors(...)`** — le navigateur interdit par défaut à une page servie par
`localhost:3000` d'appeler `localhost:8081`. CORS déclare les origines autorisées.
`allowCredentials(true)` est indispensable pour que le cookie de refresh soit transmis —
et interdit en retour le joker `*`.

## 7.4 Les JWT

Un **JWT** (*JSON Web Token*) est une chaîne en trois parties séparées par des points :

```
eyJhbGciOiJIUzI1NiJ9  .  eyJzdWIiOiJ1c2VyLTQyIn0  .  dBjftJeZ4CVP...
      en-tête                   contenu                 signature
```

- **En-tête** : l'algorithme utilisé (`HS256`)
- **Contenu** (*payload*) : les *claims*, c'est-à-dire les données
- **Signature** : la preuve que personne n'a modifié les deux premières parties

⚠️ **Un JWT n'est pas chiffré, seulement signé.** Les deux premières parties sont du
Base64, lisible par n'importe qui. **Ne mets jamais de secret dans un JWT.**

Ce que le projet met dedans, dans `security/JwtService.java` :

```java
JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
        .issuer(properties.jwt().issuer())     // qui l'a émis
        .issuedAt(now)                          // quand
        .expiresAt(expiresAt)                   // jusqu'à quand
        .subject(user.getId())                  // "sub" : de qui on parle
        .claim("roles", List.copyOf(user.getRoles()));
```

`sub` (*subject*) est l'identifiant de l'utilisateur. C'est ce que les contrôleurs
récupèrent :

```java
@GetMapping("/me")
public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
    return UserResponse.from(authService.currentUser(jwt.getSubject()));
}
```

> **Pourquoi c'est élégant :** le client ne transmet **jamais** son identifiant. Il vient
> du token signé. Personne ne peut donc consulter le profil d'un autre en changeant un
> paramètre d'URL — la classe d'attaque la plus banale sur les APIs mal conçues.

## 7.5 Le service de ressources

`user-service` valide les tokens **sans jamais appeler** `auth-service` : les deux
partagent le même secret, donc vérifier la signature suffit.

📂 **Ouvre** `user-service/.../config/SecurityConfig.java`

```java
@Bean
public JwtDecoder jwtDecoder(SecurityProperties properties) {
    SecretKey key = new SecretKeySpec(secret, "HmacSHA256");

    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),      // pas expiré
            new JwtIssuerValidator(issuer))); // bien émis par nous
    return decoder;
}
```

> **Pourquoi pas un appel réseau à chaque requête :** ce serait une latence ajoutée sur
> 100 % des appels, et surtout un point de panne — `auth-service` en panne rendrait tout le
> système inutilisable.
>
> **La contrepartie**, à connaître : HS256 utilise un secret **symétrique**, que tous les
> services doivent posséder. À dix services, ça devient risqué : il suffit qu'un seul
> fuite. On passe alors à RS256, où l'auth-service signe avec une clé privée et publie une
> clé publique que les autres se contentent de lire. C'est noté dans le README comme
> évolution prévue.

## 7.6 Les rôles

Un rôle est stocké en clair sur le compte (`"ADMIN"`) et recopié dans le token. Spring
Security ajoute le préfixe `ROLE_` de son côté :

```java
authorities.setAuthoritiesClaimName("roles");
authorities.setAuthorityPrefix("ROLE_");
```

Conséquence : `hasRole("ADMIN")` et `hasAuthority("ROLE_ADMIN")` sont équivalents.
Mélanger les deux est une source d'erreur classique — le projet stocke sans préfixe et
utilise `hasRole`.

## 7.7 Le hachage des mots de passe

**Un mot de passe ne se stocke jamais en clair, ni chiffré.** Il se *hache* : une
transformation à sens unique.

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// à l'inscription
user.setPasswordHash(passwordEncoder.encode(rawPassword));

// à la connexion
if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
    throw AuthException.invalidCredentials();
}
```

BCrypt est **volontairement lent** (~100 ms). C'est une qualité : un attaquant qui vole ta
base ne peut tester que quelques milliers de mots de passe par seconde au lieu de
milliards. Il ajoute aussi un *sel* aléatoire, donc deux comptes avec le même mot de passe
ont des hachages différents.

> **Note ce détail dans `AccountService`** : email inconnu et mot de passe faux renvoient
> **exactement la même erreur**. Sinon l'endpoint permettrait de deviner qui possède un
> compte chez toi — ce qu'on appelle l'énumération de comptes.

## 7.8 Access token et refresh token

C'est le cœur du design du projet.

| | access token | refresh token |
|---|---|---|
| durée | 30 min | 1 an (mobile) / 90 j (web) |
| vérification | signature seule, aucune base | consulté en base |
| révocable ? | non | oui |
| transporté | en-tête `Authorization` | keystore ou cookie httpOnly |

> **Pourquoi deux tokens ?** Un access token ne se vérifie pas en base — c'est ce qui le
> rend rapide et scalable, mais ça veut dire qu'on **ne peut pas l'annuler**. On le fait
> donc vivre 30 minutes.
>
> Le refresh token, lui, est consulté en base à chaque usage : il est révocable
> instantanément. Il peut donc vivre un an sans danger.
>
> Résultat : sécurité **et** confort. L'utilisateur ne se reconnecte jamais, mais une
> session volée se coupe en 30 minutes maximum.

### La rotation

📂 **Ouvre** `security/TokenService.java`, méthode `rotate`

Chaque rafraîchissement **consomme** le token présenté et en émet un neuf, avec une durée
pleine. Deux conséquences :

1. **La session glisse** : tant que l'utilisateur ouvre l'app de temps en temps, elle ne
   s'interrompt jamais.
2. **Le vol se détecte** : un refresh token ne sert qu'une fois. S'il revient une seconde
   fois, c'est qu'une copie circule. Le projet révoque alors toute la « famille » de la
   session.

```java
if (current.isRotated()) {
    log.warn("Refresh token deja consomme reutilise ...");
    revokeFamily(current.getFamilyId(), now);
    throw AuthException.invalidRefreshToken();
}
```

### Ce qui est stocké

Ni les refresh tokens ni les codes SMS ne sont stockés en clair : seule leur empreinte
SHA-256 l'est. Une fuite de la base ne permet donc pas d'usurper une session.

Pourquoi SHA-256 ici et BCrypt pour les mots de passe ? Parce qu'un refresh token est déjà
256 bits d'aléatoire pur : il est impossible à deviner par force brute, un hachage lent
n'apporterait rien et coûterait cher à chaque rafraîchissement. Un mot de passe humain,
lui, est devinable — d'où la lenteur volontaire.

## Exercices — Partie 7

**1.** Colle un access token du projet sur `jwt.io` et lis son contenu. Retrouve `sub`,
`exp` et `roles`.

**2.** Pourquoi `/api/v1/auth/login` est-il en `permitAll()` ? Que se passerait-il sinon ?

**3.** Un utilisateur est désactivé par un admin. Combien de temps peut-il encore appeler
l'API ? Regarde `AdminUserService.disable` et explique pourquoi la révocation des sessions
y est indispensable.

**4.** Explique la différence entre 401 et 403 avec un exemple tiré du projet.

**5.** Pourquoi le refresh token du web est-il dans un cookie `httpOnly` alors que celui du
mobile est dans le corps de la réponse ?

---

# Partie 8 — Les tests

## 8.1 JUnit 5

```java
class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach                    // avant CHAQUE test
    void setUp() {
        tokenService = new TokenService(repository, TestFixtures.properties());
    }

    @Test
    @DisplayName("la rotation consomme l'ancien token et en emet un nouveau")
    void rotationReplacesTheToken() {
        // ...
    }
}
```

`@DisplayName` est très utilisé dans le projet : le rapport de test se lit alors comme une
spécification en français.

La structure d'un bon test — **Arrange, Act, Assert** :

```java
@Test
void expiredTokenIsRejected() {
    // Arrange : préparer
    IssuedRefreshToken issued = tokenService.startSession(user, MOBILE, unknown());
    issued.stored().setExpiresAt(Instant.now().minusSeconds(1));

    // Act + Assert : agir et vérifier
    assertThatThrownBy(() -> tokenService.rotate(issued.rawValue(), unknown()))
            .isInstanceOf(AuthException.class);
}
```

## 8.2 AssertJ

Toutes les assertions commencent par `assertThat` et se lisent comme de l'anglais :

```java
assertThat(profile.age()).isEqualTo(16);
assertThat(user.getRoles()).contains("USER", "ADMIN");
assertThat(sessions).isEmpty();
assertThat(levels).hasSize(7);
assertThat(created.id()).isEqualTo("CM-FR:1AC");

assertThatThrownBy(() -> service.faire())
        .isInstanceOf(ApiException.class)
        .hasFieldOrPropertyWithValue("code", "unknown_level");
```

Ce dernier motif est partout dans le projet : on vérifie le **code d'erreur**, pas le
message. Le message peut être reformulé sans casser le test.

## 8.3 Mockito : les doublures

Un **mock** est un faux objet dont tu programmes les réponses. Il sert à isoler la classe
testée de ses dépendances.

```java
UserRepository users = mock(UserRepository.class);

when(users.findByEmail("paul@example.com"))
        .thenReturn(Optional.of(unUser));

verify(users).save(any(User.class));       // vérifie qu'un appel a eu lieu
verify(levels, never()).delete(any());     // vérifie qu'il n'a PAS eu lieu
```

Le projet utilise beaucoup un mock « intelligent », adossé à une `Map`, qui simule une
vraie base en mémoire :

```java
when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
    RefreshToken token = invocation.getArgument(0);
    if (token.getId() == null) {
        token.setId(UUID.randomUUID().toString());
    }
    stored.put(token.getTokenHash(), token);
    return token;
});
```

C'est ce qui permet de tester des enchaînements complets (créer, faire tourner, rejouer)
sans MongoDB.

### Capturer un argument

```java
ArgumentCaptor<String> term = ArgumentCaptor.forClass(String.class);
verify(users).search(term.capture(), eq(pageable));
assertThat(term.getValue()).isEqualTo(Pattern.quote("(a+)+$"));
```

Ici on vérifie que le terme a bien été **échappé** avant d'atteindre la base. C'est une
propriété de sécurité vérifiée automatiquement — beaucoup plus fiable qu'un commentaire.

## 8.4 Les tests d'intégration Spring

📂 **Ouvre** `AuthServiceApplicationTests.java`

```java
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=...MongoAutoConfiguration,..."
})
class AuthServiceApplicationTests {

    @MockitoBean
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @Test
    void contextLoads() {
        assertThat(authService).isNotNull();
    }
}
```

`@SpringBootTest` démarre le vrai contexte Spring. `@MockitoBean` remplace un bean réel par
un mock **dans ce contexte**.

> **Pourquoi Mongo est écarté ici :** le bean `mongoTemplate` ouvre une connexion dès sa
> création. Sans cette exclusion, le test échouerait partout où aucune base ne tourne —
> notamment sur un serveur d'intégration continue.

Ce test paraît trivial (`isNotNull`), mais il attrape énormément : une dépendance
circulaire, un bean manquant, une propriété mal nommée, une configuration de sécurité
invalide. Il vaut son coût.

## 8.5 Quoi tester, et quoi ne pas tester

| Teste | Ne teste pas |
|---|---|
| ta logique métier | les getters et setters |
| les cas limites et les erreurs | le framework lui-même |
| les règles de sécurité | le code généré par Spring Data |
| les invariants (« un token ne sert qu'une fois ») | des mocks qui se répondent à eux-mêmes |

Le projet compte 113 tests, dont **aucun** ne demande de base de données. C'est un choix :
des tests rapides sont des tests qu'on lance à chaque sauvegarde. Le revers, honnêtement
reconnu dans les README, c'est que les index uniques ne sont pas couverts — il faudrait
Testcontainers pour ça.

## Exercices — Partie 8

**1.** Écris un test vérifiant que `PhoneNumbers.normalize("+237 690 00 00 00")` renvoie
`"+237690000000"`.

**2.** Dans `ProfileServiceTest`, trouve `changingLevelResetsWhatDependsOnIt`. Explique ce
que ce test protège et pourquoi c'est important.

**3.** Écris un test qui vérifie qu'un compte désactivé ne peut pas se connecter. (Indice :
il existe, cherche `disabledAccountIsRefused`.)

**4.** Pourquoi les tests du projet vérifient-ils `hasFieldOrPropertyWithValue("code",
...)` plutôt que le message d'erreur ?

---

# Partie 9 — Lecture guidée : une requête de bout en bout

Suivons `POST /api/v1/auth/register` avec `{"email":"paul@example.com","password":"secret123"}`.

### 1. Les filtres de sécurité

`SecurityConfig` : la route correspond à `/api/v1/auth/**` → `permitAll()`. Elle passe sans
token — heureusement, on ne peut pas exiger un token pour aller en chercher un.

### 2. Le contrôleur

`web/client/AuthController.register(...)`

- Jackson transforme le JSON en `RegisterRequest`
- `@Valid` déclenche la validation. Email invalide ou mot de passe trop court → 400, on
  s'arrête là
- `ClientType.from(header)` lit `X-Client-Type`, `MOBILE` par défaut

### 3. L'orchestration

`service/AuthService.register(...)` appelle `accounts.registerWithEmail(...)`, puis
`openSession(...)`.

### 4. La logique métier

`service/AccountService.registerWithEmail(...)`

- normalise l'email en minuscules
- `users.existsByEmail(...)` → si oui, `throw AuthException.emailAlreadyUsed()` → 409
- hache le mot de passe avec BCrypt
- crée le `User`, rattache l'identité `EMAIL`, sauvegarde

### 5. L'ouverture de session

`AuthService.openSession(...)`

- `jwtService.issue(user)` → un access token signé, valable 30 min
- `tokenService.startSession(...)` → un refresh token aléatoire ; **seule son empreinte
  SHA-256 part en base**

### 6. La réponse

`AuthController.respond(...)`

- mobile → le refresh token est dans le corps
- web → il part dans un cookie `httpOnly`, absent du corps
- statut `201 CREATED`, `newAccount: true`

### 7. Et si ça échoue ?

N'importe quelle `AuthException` levée en chemin remonte jusqu'à
`GlobalExceptionHandler`, qui la convertit en `application/problem+json` avec le bon
statut et le champ `code`.

```json
{
  "type": "about:blank",
  "title": "Echec de l'authentification",
  "status": 409,
  "detail": "Un compte existe deja avec cette adresse email.",
  "code": "email_already_used"
}
```

### Le schéma à retenir

```
Contrôleur   →  traduit HTTP ↔ objets Java. Aucune logique métier.
Service      →  la logique métier. Ne connaît rien de HTTP.
Repository   →  l'accès aux données. Aucune logique métier.
```

> **Pourquoi cette séparation en trois couches :** chacune se teste seule, et surtout
> chacune peut changer sans toucher aux autres. Passer de MongoDB à PostgreSQL ne toucherait
> que la couche repository. Ajouter une API GraphQL à côté du REST ne toucherait que la
> couche web. C'est la structure la plus répandue dans le monde Spring — reconnais-la, tu
> la retrouveras partout.
>
> Le test décisif : **un service ne doit jamais importer `jakarta.servlet` ni
> `ResponseEntity`.** Vérifie dans le projet, c'est respecté.

---

# Partie 10 — Projet final

Ajoute une fonctionnalité complète : **la liste de ses propres sessions actives, pour
qu'un utilisateur voie où il est connecté et puisse déconnecter un appareil.**

C'est une fonctionnalité réelle, listée dans les « reste à faire » du README.

### Ce qu'il faut produire

**1. Un DTO** `web/client/dto/MySessionResponse.java`
Champs : `id`, `clientType`, `userAgent`, `issuedAt`, et un booléen `current` indiquant
s'il s'agit de la session en cours.
⚠️ Ne mets **ni** le token **ni** son empreinte. Réfléchis à pourquoi.

**2. Une méthode de service** dans `TokenService` ou un nouveau service
`sessionsOf(String userId)`, qui renvoie les sessions utilisables triées du plus récent au
plus ancien. Regarde `AdminUserService.activeSessions` : le travail est déjà fait, inspire-toi.

**3. Deux endpoints** dans `AuthController`
```
GET    /api/v1/auth/sessions          → la liste
DELETE /api/v1/auth/sessions/{id}     → révoquer un appareil
```

**4. La sécurité** — le point le plus important
Ajoute les routes dans `SecurityConfig` avec `.authenticated()`.
Et surtout : **vérifie que la session supprimée appartient bien à l'appelant.** Sans ce
contrôle, n'importe qui déconnecte n'importe qui en devinant un identifiant. C'est la
faille la plus courante des APIs REST — elle a un nom, *IDOR*.

**5. Des tests**
- la liste ne montre que les sessions utilisables
- supprimer la session d'un autre utilisateur est refusé
- supprimer sa propre session la révoque bien

### Grille d'auto-évaluation

| | Critère |
|---|---|
| ☐ | Le contrôleur ne contient aucune logique métier |
| ☐ | Le service n'importe rien de `jakarta.servlet` |
| ☐ | Aucun secret n'apparaît dans le DTO |
| ☐ | L'identifiant utilisateur vient du token, jamais d'un paramètre |
| ☐ | Le contrôle d'appartenance est testé |
| ☐ | Les tests tournent sans base de données |
| ☐ | `./mvnw test` passe |

Si tu coches les sept, tu as le niveau visé par cette formation.

---

# Annexes

## A. Glossaire

| Terme | Définition |
|---|---|
| **Bean** | Un objet géré par Spring |
| **Conteneur IoC** | L'annuaire de beans de Spring |
| **DTO** | Objet dédié à l'entrée/sortie d'une API |
| **Entité** | Objet qui représente une donnée stockée |
| **Repository** | Couche d'accès aux données |
| **Endpoint** | Une route de l'API (méthode + chemin) |
| **JWT** | Jeton signé portant l'identité |
| **Claim** | Une donnée à l'intérieur d'un JWT |
| **Hachage** | Transformation à sens unique |
| **Mock** | Faux objet utilisé en test |
| **Idempotent** | Rejouable sans changer le résultat |
| **CORS** | Règles d'appel entre domaines différents |
| **TTL** | Durée de vie avant suppression automatique |

## B. Commandes utiles

```bash
./mvnw compile                          # compiler
./mvnw test                             # tester
./mvnw test -Dtest=TokenServiceTest     # un seul test
./mvnw spring-boot:run                  # démarrer
./mvnw dependency:tree                  # voir les dépendances
./mvnw clean                            # nettoyer target/

docker run -d -p 27017:27017 --name ojino-mongo mongo:7
docker start ojino-mongo
```

## C. Erreurs fréquentes

| Symptôme | Cause habituelle |
|---|---|
| `NoSuchBeanDefinitionException` | Classe hors du package scanné, ou annotation stéréotype oubliée |
| `Field required a bean of type ...` | Idem, ou une interface sans implémentation |
| La validation ne se déclenche pas | `@Valid` oublié sur le paramètre du contrôleur |
| 403 sur une route censée être ouverte | Une règle plus haute dans `authorizeHttpRequests` l'a interceptée |
| `MongoTimeoutException` au démarrage | MongoDB n'est pas lancé |
| Erreur de démarrage sur un repository | Faute de frappe dans un nom de méthode dérivée |
| `NullPointerException` | Un `Optional` mal ouvert, ou un champ jamais initialisé |
| `package com.fasterxml.jackson.databind does not exist` | Boot 4 utilise Jackson 3 : `tools.jackson.databind` |

## D. Pour aller plus loin

Dans l'ordre où ça devient utile pour ce projet :

1. **Testcontainers** — tester avec un vrai MongoDB éphémère
2. **API Gateway** — une seule URL d'entrée devant les deux services
3. **OpenAPI / Swagger** — documentation d'API générée automatiquement
4. **Docker et docker-compose** — lancer tout l'ensemble d'une commande
5. **RS256 et JWKS** — remplacer le secret partagé quand les services se multiplient
6. **Actuator** — sonde de santé et métriques
7. **Flux d'événements (Kafka, RabbitMQ)** — quand les services doivent se prévenir

---

## Ce que tu dois savoir faire en sortie

- [ ] Lire n'importe quelle classe du projet et dire à quoi elle sert
- [ ] Expliquer d'où vient chaque objet injecté dans un constructeur
- [ ] Ajouter un endpoint complet : DTO, validation, contrôleur, service, repository
- [ ] Écrire une entité MongoDB avec ses index et son repository
- [ ] Lire `SecurityConfig` et dire qui a le droit d'appeler quoi
- [ ] Expliquer la différence entre access token et refresh token, et pourquoi il y en a deux
- [ ] Écrire un test unitaire avec mocks, sans base de données
- [ ] Repérer une fuite de donnée dans un DTO

Si tu coches tout, tu es au niveau du code actuel du projet. La formation 2 portera sur
l'API Gateway, Docker et le déploiement.
