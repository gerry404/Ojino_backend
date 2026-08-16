# Formation 3 — De développeur Spring Boot à senior

**Objectif :** comprendre ce que le framework fait réellement, savoir pourquoi chaque
décision du projet a été prise, et être capable d'en prendre de nouvelles.

Prérequis : [formation 1](formation1.md) et [formation 2](formation2.md).

> **Ce qui sépare un junior d'un senior**, ce n'est pas le nombre d'annotations connues.
> C'est trois choses : savoir ce que le framework fait **sous** l'annotation, savoir ce qui
> casse **en production** et pas seulement en local, et savoir **ce qu'on ne construit
> pas**.
>
> Cette formation est écrite dans cet ordre.

---

## Sommaire

| Partie | Sujet |
|---|---|
| 1 | Le conteneur, vraiment |
| 2 | L'autoconfiguration démystifiée |
| 3 | Spring Boot 4 : ce qui a changé |
| 4 | Spring Security en profondeur |
| 5 | Transactions et cohérence |
| 6 | Performance et threads virtuels |
| 7 | La stratégie de test d'un senior |
| 8 | Observabilité |
| 9 | Architecture : lire les décisions du projet |
| 10 | Les erreurs qui coûtent cher |
| 11 | Parcours et sources |

---

# Partie 1 — Le conteneur, vraiment

## 1.1 Ce qui se passe au démarrage

Quand tu lances `SpringApplication.run()`, il se passe six choses, dans cet ordre :

1. **Scan** des packages → une liste de *définitions* de beans (pas encore d'objets)
2. **BeanFactoryPostProcessor** → modification des définitions
3. **Instanciation** → les objets sont créés, dépendances injectées
4. **BeanPostProcessor** → chaque bean peut être remplacé par un proxy
5. **`@PostConstruct`** / `InitializingBean`
6. **`ApplicationRunner`** → ton code démarre

📂 Regarde `ReferenceDataSeeder` dans `content-service` : c'est un `ApplicationRunner`,
donc il s'exécute **après** que tout est câblé. Le mettre dans un `@PostConstruct` aurait
été fragile — l'ordre des `@PostConstruct` entre beans n'est pas garanti.

## 1.2 Le proxy : la source n°1 des « ça ne marche pas »

À l'étape 4, Spring **remplace** certains beans par un proxy. Un proxy est un objet qui
enveloppe le tien et intercepte les appels.

C'est ce qui fait fonctionner `@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize`.

**Le piège de l'auto-invocation :**

```java
@Service
public class MonService {

    @Transactional
    public void a() { ... }

    public void b() {
        a();   // ❌ AUCUNE transaction
    }
}
```

Pourquoi ? L'appelant externe passe par le proxy. Mais `b()` appelle `a()` sur `this` —
l'objet réel, pas le proxy. L'annotation est **invisible**.

Trois sorties, par ordre de préférence :
1. **Déplacer `a()` dans un autre bean** — c'est presque toujours le signe qu'il manque
   une classe
2. Injecter le service dans lui-même (fonctionne, mais c'est un aveu)
3. `AopContext.currentProxy()` (dernier recours, illisible)

> **Ce que ça t'apprend :** dès qu'une annotation Spring « ne fait rien », demande-toi
> d'abord si l'appel passe par le proxy.

## 1.3 Les scopes

| Scope | Durée de vie | Quand |
|---|---|---|
| `singleton` (défaut) | toute l'application | 99 % des cas |
| `prototype` | un nouveau à chaque injection | rare |
| `request` / `session` | web | à éviter, couple au HTTP |

**Le piège :** injecter un `prototype` dans un `singleton` ne donne **qu'une seule
instance** — celle capturée à la construction. Si tu as besoin d'un nouvel objet à chaque
appel, utilise `ObjectProvider<T>` ou une fabrique.

**Corollaire important :** un singleton doit être **sans état mutable**. Tous les services
du projet le sont : leurs champs sont `final` et n'accueillent que des dépendances. Un
champ mutable dans un `@Service` est un bug de concurrence en attente.

## 1.4 `@Configuration` et `proxyBeanMethods`

```java
@Configuration
public class JwtConfig {

    @Bean SecretKey accessTokenKey(...) { ... }

    @Bean JwtEncoder jwtEncoder(SecretKey key) { ... }   // ✅ paramètre
}
```

Par défaut, une `@Configuration` est elle-même proxifiée : si `jwtEncoder()` appelait
`accessTokenKey()` directement, Spring intercepterait pour renvoyer le singleton au lieu
d'en créer un second.

**Bonne pratique :** passe toujours par les **paramètres de méthode**, comme fait
`JwtConfig`. Ça marche avec ou sans proxy, et ça rend la dépendance visible.

---

# Partie 2 — L'autoconfiguration démystifiée

## 2.1 Comment ça marche réellement

Ce n'est pas de la magie, c'est un fichier texte.

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Chaque starter en contient un, listant des classes de configuration. Spring les charge
**toutes**, puis chacune décide si elle s'applique via des `@Conditional`.

| Condition | Vraie si… |
|---|---|
| `@ConditionalOnClass` | une classe est sur le classpath |
| `@ConditionalOnMissingBean` | aucun bean de ce type n'existe |
| `@ConditionalOnProperty` | une propriété a une valeur |
| `@ConditionalOnWebApplication` | c'est une application web |

C'est pour ça qu'ajouter `spring-boot-starter-data-mongodb` crée un `MongoTemplate` : la
classe est sur le classpath, la condition passe.

## 2.2 Le piège qui nous a coûté deux fois

`@ConditionalOnMissingBean` **n'est fiable que dans une classe d'autoconfiguration.**

Dans une `@Configuration` ordinaire ou un `@Component` scanné, elle dépend de l'ordre de
traitement — qui n'est pas garanti. Et elle compare le **type déclaré** de la méthode
`@Bean`, pas le type réel de l'objet retourné.

📂 C'est exactement ce qui est arrivé dans `media-service` :

```java
@Bean
@ConditionalOnMissingBean(MediaStorage.class)
MediaStorage localMediaStorage(...) { ... }   // ❌ le contexte refusait de démarrer
```

Corrigé en :

```java
@Bean
@ConditionalOnProperty(name = "ojino.media.storage", havingValue = "local",
        matchIfMissing = true)
LocalMediaStorage localMediaStorage(...) { ... }   // ✅ déterministe, type concret
```

**La règle :** dans ton propre code, choisis par **propriété**. Le choix devient explicite
et lisible, au lieu d'être deviné.

## 2.3 Débugger le câblage

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug
```

Ça affiche le **rapport d'évaluation des conditions** : ce qui s'est appliqué, ce qui ne
s'est pas appliqué, et **pourquoi**. C'est le premier réflexe quand un bean manque.

Deux autres outils :

```java
// Lister tous les beans
context.getBeanDefinitionNames()

// Voir d'où vient une propriété
context.getEnvironment().getPropertySources()
```

## 2.4 Exclure une autoconfiguration

C'est ce que font tous les tests du projet :

```java
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=...MongoAutoConfiguration,..."
})
```

⚠️ En Boot 4, ces classes ont **changé de package** (`org.springframework.boot.mongodb.autoconfigure`).
Les noms de Boot 3 ne marchent plus — voir partie 3.

---

# Partie 3 — Spring Boot 4 : ce qui a changé

Boot 4.0 est sorti le **30 novembre 2025**, sur Spring Framework 7 et Jakarta EE 11.
C'est une migration majeure, et le projet est né dessus — tu ne le subiras donc pas, mais
tu dois savoir ce qui diffère de tout ce que tu liras en ligne.

## 3.1 La modularisation

Boot 4 livre **beaucoup de petits modules** au lieu de quelques gros jars. Conséquence
directe : des fonctionnalités qui marchaient « toutes seules » demandent maintenant un
starter explicite.

C'est pour ça que le projet déclare `spring-boot-starter-webmvc` et non
`spring-boot-starter-web`.

## 3.2 Jackson 3

```java
import tools.jackson.databind.ObjectMapper;              // ✅ Boot 4
import com.fasterxml.jackson.databind.ObjectMapper;      // ❌ n'existe plus
```

Seules les **annotations** restent en `com.fasterxml.jackson.annotation` (`@JsonFormat`,
`@JsonInclude`). C'est déroutant, et c'est la source d'erreur n°1 quand on copie un
tutoriel.

> ⚠️ **Piège vécu :** Jackson ne convertit pas `snake_case` → `camelCase` par défaut. Une
> clé JSON `orientation_detresse` ne se lie **pas** au champ `orientationDetresse` — elle
> vaut `null`, en silence. C'est arrivé dans `assistant-service`, et ça a désactivé les
> garde-fous de détresse sans aucune erreur. Seul un test l'a attrapé.

## 3.3 Spring Security 7

Trois suppressions qui cassent tout ce que tu trouveras sur internet :

| Supprimé | Remplacé par |
|---|---|
| `.and()` | le DSL lambda, obligatoire |
| `authorizeRequests()` | `authorizeHttpRequests()` |
| `antMatchers()`, `AntPathRequestMatcher`, `MvcRequestMatcher` | `requestMatchers()` avec `PathPatternRequestMatcher` |

📂 `SecurityConfig` du projet est déjà écrit dans le style Boot 4. Prends-le comme
référence, pas les tutoriels.

## 3.4 Le reste

- **Java 17 minimum** (le projet est en 21)
- **Undertow supprimé** — Tomcat ou Jetty
- **JSpecify** — annotations de nullité, le compilateur t'aide davantage
- **Versionnement d'API** natif dans le modèle de routage MVC
- **Clients HTTP déclaratifs** auto-configurés : une interface annotée suffit

> Ce dernier point mérite ton attention : les cinq `RestClient` écrits à la main dans le
> projet pourraient devenir des interfaces déclaratives. À évaluer quand tu auras besoin
> d'en ajouter un sixième.

---

# Partie 4 — Spring Security en profondeur

## 4.1 La chaîne de filtres, vraiment

Spring Security n'est pas « dans » ton contrôleur. C'est un **filtre servlet** qui
s'exécute avant que Spring MVC n'existe pour cette requête.

```
Requête HTTP
   ↓
FilterChainProxy
   ↓
[ CORS ] → [ CSRF ] → [ lecture du token ] → [ autorisation ]
   ↓
DispatcherServlet → ton contrôleur
```

**Conséquence pratique n°1 :** une requête refusée par la sécurité n'atteint jamais ton
`@RestControllerAdvice`. C'est pourquoi les erreurs 401 n'ont pas le même format que tes
erreurs métier — il faut un `AuthenticationEntryPoint` pour ça.

**Conséquence pratique n°2 :** un filtre à toi doit se placer explicitement. 📂 Regarde
`InternalApiFilter` dans `notification-service` : c'est un `OncePerRequestFilter`, et
`shouldNotFilter` lui évite de tourner sur toutes les routes.

## 4.2 L'ordre des règles

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN)  // 1er
        .requestMatchers("/api/v1/auth/**").permitAll()           // 2e
        .anyRequest().authenticated())
```

**La première règle qui correspond gagne.** Toujours.

Si `permitAll()` sur `/api/v1/auth/**` était écrit en premier, `/api/v1/auth/me`
deviendrait public. C'est pour ça que la règle admin est déclarée en tête dans les cinq
services qui en ont une.

> **Le principe senior :** protège par **préfixe dans la chaîne de filtres**, pas par
> annotation sur la méthode. Une `@PreAuthorize` oubliée ouvre la route en grand sans que
> rien ne le signale ; un préfixe protège aussi les routes que tu ajouteras dans six mois.

## 4.3 `SecurityContext` et les threads

Le contexte de sécurité est stocké dans un `ThreadLocal`. Trois conséquences :

- Dans un `@Async`, il est **perdu** (sauf configuration explicite)
- Dans un `CompletableFuture`, il est **perdu**
- Dans une tâche `@Scheduled`, il n'a **jamais existé**

C'est précisément pourquoi `notification-service` et `engagement-service` ont des routes
internes protégées par un secret partagé : leurs tâches planifiées n'ont aucun utilisateur
connecté.

## 4.4 Stateless : ce que ça implique

```java
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

Aucune session serveur. Chaque requête porte son token et se suffit à elle-même.

**Le gain :** tu peux lancer dix instances derrière un load balancer sans te demander
laquelle détient la session.

**Le coût :** un access token ne peut pas être révoqué. D'où le design à deux tokens du
projet — 30 min pour l'access, révocable pour le refresh.

## 4.5 CSRF : quand le désactiver

`csrf.disable()` fait sursauter, et c'est sain. Mais :

- Le CSRF protège les **formulaires HTML avec cookie de session**
- Une API sans état consommée par des apps natives n'a pas de formulaire
- Le cookie de refresh est couvert par `SameSite=Lax`, qui bloque les POST inter-sites

**La règle :** désactiver CSRF est légitime **si et seulement si** tu n'as aucune
authentification implicite par cookie. Écris-le en commentaire — le prochain lecteur se
posera la question.

---

# Partie 5 — Transactions et cohérence

## 5.1 `@Transactional` en trois lignes

C'est un proxy (partie 1.2). Il ouvre une transaction avant, commit après, rollback si une
`RuntimeException` sort.

⚠️ **Par défaut, il ne rollback PAS sur une exception vérifiée** (`checked`). Il faut
`@Transactional(rollbackFor = Exception.class)`.

C'est une des raisons pour lesquelles tout le projet utilise des `RuntimeException` —
`ApiException` et `AuthException` en héritent.

## 5.2 Pourquoi le projet n'a presque pas de `@Transactional`

Parce que MongoDB en instance unique **ne supporte pas les transactions multi-documents**
— il faut un replica set.

Mais surtout : **une écriture sur un seul document est atomique en soi.** Le projet est
modélisé pour que chaque opération touche un document, ce qui rend la transaction inutile.

📂 Quand ce n'est pas possible, le code le gère explicitement. Regarde
`TokenService.rotate` : il crée le nouveau token **avant** de marquer l'ancien. Si le
processus meurt entre les deux, on a un token en trop — gênant mais inoffensif. L'ordre
inverse aurait déconnecté l'utilisateur.

> **Le réflexe senior :** quand tu ne peux pas avoir de transaction, ordonne les écritures
> pour que **l'échec intermédiaire soit le moins grave**. Et écris pourquoi.

## 5.3 Ce que l'index unique garantit et pas le code

```java
if (users.existsByEmail(email)) {   // ← test
    throw AuthException.emailAlreadyUsed();
}
users.save(user);                    // ← écriture
```

Entre le test et l'écriture, une autre requête peut passer. Deux inscriptions simultanées
franchiraient toutes les deux le test.

**Seul l'index unique tranche vraiment.** Le code gère le cas courant avec un message
propre, l'index gère la course — d'où le `catch (DuplicateKeyException)` dans
`AccountService`.

---

# Partie 6 — Performance et threads virtuels

## 6.1 Les threads virtuels

Depuis Boot 3.2, une seule propriété :

```properties
spring.threads.virtual.enabled=true
```

**Ce que ça change :** un thread plateforme coûte ~1 Mo de pile. Un thread virtuel coûte
quelques kilo-octets. Tu passes de quelques centaines de requêtes concurrentes à des
dizaines de milliers.

**Ce que ça ne change pas :** les threads virtuels améliorent la **concurrence**, pas la
vitesse. Si ta base est lente, elle reste lente — simplement, plus de requêtes peuvent
attendre efficacement.

**Les précautions réelles :**

| Point | Pourquoi |
|---|---|
| Vérifier le *pinning* | Un bloc `synchronized` autour d'un appel bloquant épingle le thread porteur et annule le bénéfice |
| Mettre à jour les drivers | PostgreSQL 42.6+, MySQL Connector/J 8.2+ testés avec Loom |
| **Réduire** le pool de connexions | Contre-intuitif : les threads virtuels attendent efficacement, un pool de 10 suffit souvent |
| Tester en charge avant | Le comportement change qualitativement, pas seulement quantitativement |

> **Pour ce projet :** Spring MVC + threads virtuels est le bon choix. C'est plus simple à
> écrire, tester et déboguer que du réactif, pour des performances comparables sur ce type
> de charge. Ne pars pas sur WebFlux.
>
> **Sauf pour le temps réel** — et c'est exactement pourquoi `realtime-service` est en Go :
> tenir des connexions WebSocket ouvertes pendant des heures est un problème de
> concurrence pure, où une goroutine est encore plus légère.

## 6.2 Les délais d'attente : la leçon la plus chère

**Tout appel réseau sans délai est un incident en attente.**

Sans délai, un service lent immobilise un thread par requête. À quelques centaines de
requêtes, ton service tombe — non pas à cause de sa propre charge, mais à cause de celle
d'un autre.

📂 Tous les clients du projet en ont :

```java
factory.setConnectTimeout(properties.connectTimeout());   // 2s
factory.setReadTimeout(properties.readTimeout());         // 3s
```

**L'étape suivante**, quand le trafic le justifiera : un **disjoncteur**
(Resilience4j). Après N échecs, il ouvre le circuit et échoue immédiatement, laissant le
service en difficulté se rétablir au lieu de le marteler.

## 6.3 Dégrader plutôt qu'échouer

📂 Le motif est déjà partout dans le projet :

| Service | Panne | Réaction |
|---|---|---|
| `planning` | `learning-service` KO | Planifie sur les échéances seules |
| `assistant` | `learning-service` KO | Réponse moins ciblée, mais réponse |
| `assistant` | `user-service` KO | **Refuse** — sans le niveau, pas de registre |

> **Le raisonnement senior :** pour chaque dépendance, demande-toi « si elle tombe, est-ce
> que mon service peut encore rendre un service utile ? ». Si oui, dégrade. Si non, refuse
> proprement avec un 503 — pas un 500.

---

# Partie 7 — La stratégie de test d'un senior

## 7.1 Le vrai coût d'un test

| Type | Durée | Ce qu'il prouve |
|---|---|---|
| Unitaire pur | ~1 ms | Ta logique |
| Test slice (`@WebMvcTest`) | ~1 s | Une couche |
| `@SpringBootTest` | ~10 s | Le câblage |
| Testcontainers | ~30 s | Le comportement réel |

**Le principe :** teste au niveau le plus bas qui prouve ce que tu veux prouver.

📂 C'est exactement la répartition du projet — **272 tests, dont environ 250 purs** :
`StreakPolicy`, `NotionGraph`, `MasteryCalculator`, `DeliveryGate`, `WeeklyPlanner`,
`QuotaPolicy`, `SafetyGuard`. Chacun s'exécute sans base ni Spring.

## 7.2 Les test slices

```java
@WebMvcTest(AuthController.class)   // charge MVC + ce contrôleur, rien d'autre
@DataMongoTest                       // charge Mongo, pas le web
```

Elles chargent un contexte minimal, tournent bien plus vite que `@SpringBootTest`, et
**forcent une meilleure séparation** : si ton contrôleur ne peut pas se charger sans la
moitié de l'application, c'est qu'il en fait trop.

## 7.3 Le cache de contexte : l'optimisation que personne ne connaît

Spring **met en cache le contexte d'application entre les classes de test**, tant que la
configuration est identique.

Ça veut dire que ceci crée **deux contextes** au lieu d'un :

```java
@SpringBootTest(properties = "a=1")  class TestA { }
@SpringBootTest(properties = "a=2")  class TestB { }
```

**Les règles qui font gagner des minutes :**

1. **Standardise ta configuration de test.** Une classe de base commune, pas des
   `properties` éparpillées
2. **`@DirtiesContext` est une bombe** — il invalide le cache. À n'utiliser qu'en dernier
   recours
3. **Uniformise `@MockitoBean`** — chaque combinaison différente crée un contexte de plus

📂 Les neuf services du projet utilisent la **même** chaîne d'exclusion Mongo, ce qui n'est
pas un hasard.

## 7.4 Testcontainers

C'est ce qui manque au projet, et je l'ai écrit dans chaque README.

```java
@Testcontainers
@SpringBootTest
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
}
```

`@ServiceConnection` (Boot 3.1+) branche l'URI automatiquement — aucune propriété à écrire.

**Ce que seul Testcontainers prouve :**
- Les index uniques (le test applicatif ne prouve que le contrôle applicatif)
- Les index TTL
- Le comportement réel des `@Query`
- Les migrations

**Le `static` est essentiel** : il partage le conteneur entre les méthodes. Sans lui, tu
démarres un Mongo par test.

## 7.5 Ce qu'on ne teste pas

- Les getters, les setters, les mappings DTO triviaux
- Le framework lui-même
- Le code généré par Spring Data
- Les bouchons (`CannedAiEngine`)

> **Un test qui ne peut pas échouer autrement qu'en cassant la compilation ne sert à
> rien.** Il coûte du temps de maintenance et donne une fausse confiance.

## 7.6 Le test qui vaut le plus cher

📂 `everyCycleModuleIsRegistered` dans `content-service`.

Il vérifie que les cinq modules Maven de cycle sont bien ramassés par le scan. Sans lui, un
module retiré du `pom.xml` disparaîtrait **silencieusement** — et on ne s'en apercevrait
qu'au premier élève de maternelle.

**Cherche ces tests-là :** ceux qui protègent une décision d'architecture, pas une ligne de
code.

---

# Partie 8 — Observabilité

## 8.1 Actuator

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
```

⚠️ **N'expose jamais `*`.** `/actuator/env` révèle tes variables d'environnement,
`/actuator/heapdump` télécharge la mémoire du processus. Le projet n'expose que `health`.

## 8.2 Les logs

Trois règles qui distinguent un log utile d'un log de junior :

**1. Structuré, pas concaténé**

```java
log.info("Compte {} desactive par {}", userId, adminId);   // ✅
log.info("Compte " + userId + " desactive");               // ❌
```

Le premier est indexable et n'évalue pas la chaîne si le niveau est désactivé.

**2. Jamais de données personnelles**

📂 Regarde le projet : on logue des identifiants, jamais des emails, jamais le contenu
d'une conversation avec l'assistant.

**3. Le détail technique reste dans les logs**

```java
log.error("Erreur inattendue", exception);   // dans les logs
// message générique renvoyé au client
```

Renvoyer la trace au client documente ton serveur pour un attaquant.

## 8.3 Ce qui manque au projet

À ajouter quand le trafic le justifiera, dans cet ordre :

1. **Traçage distribué** (Micrometer Tracing + OpenTelemetry) — un identifiant de
   corrélation qui traverse les neuf services. Sans ça, déboguer une requête lente qui
   traverse `assistant → user → learning → content` est un cauchemar
2. **Métriques métier** — pas seulement CPU : « questions posées », « quotas atteints »,
   « séries cassées »
3. **Alerting** sur les signaux qui comptent

---

# Partie 9 — Architecture : lire les décisions du projet

Cette partie est la plus importante. Elle t'apprend à **lire une décision**, pas une
syntaxe.

## 9.1 Ports et adaptateurs — 4 instances

| Port | Bouchon | Réel |
|---|---|---|
| `SmsSender` | logs | Twilio |
| `MediaStorage` | disque | S3 |
| `NotificationSender` | logs | FCM |
| `AiEngine` | fabriqué | `ai-service` |

**Le motif :** une interface dans ton domaine, une implémentation de développement, une
implémentation réelle plus tard. Sélection par propriété.

**Ce que ça achète :** `assistant-service` a été écrit, testé et livré **avant**
`ai-service`. Quotas, garde-fous, contexte, historique — tout est éprouvé sans modèle
derrière, sans facture.

> **La question à te poser :** « qu'est-ce que je ne peux pas encore faire, et comment
> écrire le reste sans l'attendre ? »

## 9.2 Séparer par risque, pas par technique

```
web/
├── client/    ce que les apps consomment
└── admin/     le back-office, ROLE_ADMIN sur le préfixe
```

Ce n'est pas de l'organisation cosmétique. C'est le fait que **la règle de sécurité est
posée sur le préfixe**, pas sur chaque méthode.

## 9.3 Rien ne se supprime, tout s'archive

📂 `content-service` n'a aucune route de suppression.

**Pourquoi :** un niveau supprimé laisserait des profils pointant vers un code disparu — et
comme les services ont chacun leur base, aucune intégrité référentielle ne peut le
rattraper.

**La contrainte cachée des microservices :** tu perds les clés étrangères. Il faut la
remplacer par une discipline explicite.

## 9.4 Dénormaliser, mais l'écrire

📂 `StudentProfile.curriculumSteps` recopie une information de `content-service`.

**Le gain :** la lecture de l'état d'inscription — appelée à chaque ouverture de l'app —
ne fait aucun appel réseau.

**Le coût, assumé et documenté :** un changement côté référentiel ne se propage qu'à la
reconfirmation du niveau.

> **La dénormalisation n'est pas un péché, c'est un arbitrage.** Mais un arbitrage non
> écrit devient un bug six mois plus tard.

## 9.5 Ce qu'on ne construit pas

📂 La roadmap liste explicitement : pas de gateway, pas de BFF, pas de service discovery,
pas de Kafka, pas de CI/CD.

**Ce n'est pas de la paresse.** Ces briques résolvent des problèmes qu'on n'a pas encore, et
les poser trop tôt fige des choix avant d'avoir l'information pour les faire.

> **C'est peut-être la compétence senior la plus rare :** résister à construire ce qui
> serait intéressant à construire.

---

# Partie 10 — Les erreurs qui coûtent cher

Toutes viennent de ce projet ou de pièges classiques vérifiés.

| # | Erreur | Conséquence |
|---|---|---|
| 1 | Appel réseau sans délai d'attente | Un service lent en fait tomber cinq |
| 2 | `@ConditionalOnMissingBean` hors autoconfiguration | Le contexte refuse de démarrer, sans motif clair |
| 3 | `unique` sans `sparse` sur un champ facultatif | Le 2ᵉ document sans ce champ est refusé |
| 4 | Terme de recherche non échappé dans un `$regex` | Une requête bien choisie bloque la base |
| 5 | Compter le total après la pagination | Le total vaut la taille de la page |
| 6 | Exposer `Page` de Spring Data dans l'API | Le contrat casse à la montée de version |
| 7 | Renvoyer l'entité au lieu d'un DTO | Le hash du mot de passe part au client |
| 8 | `@Valid` oublié sur le paramètre | Toutes les annotations de validation sont décoratives |
| 9 | Une règle `permitAll` avant une règle restrictive | La route protégée devient publique |
| 10 | Champ mutable dans un singleton | Bug de concurrence intermittent |
| 11 | Auto-invocation d'une méthode `@Transactional` | Aucune transaction, silencieusement |
| 12 | `@DirtiesContext` par confort | La suite de tests triple de durée |
| 13 | Exposer `management.endpoints=*` | `/actuator/env` livre tes secrets |
| 14 | Clé JSON `snake_case` sur un champ `camelCase` | Champ `null`, sans erreur |
| 15 | Vérifier le quota **après** l'appel payant | Tu paies ce que tu refuses |

---

# Partie 11 — Parcours et sources

## 11.1 Ce que tu devrais faire, dans l'ordre

**Semaine 1-2 — Consolider**
- Relis `SecurityConfig`, `TokenService`, `AccountService` de `auth-service` ligne à ligne
- Lance `--debug` et lis le rapport de conditions en entier
- Écris un test qui échoue, comprends pourquoi, corrige-le

**Semaine 3-4 — Combler le trou principal**
- Ajoute **Testcontainers** sur un service. C'est le manque n°1 du projet, écrit dans
  chaque README
- Prouve qu'un index unique fait bien son travail — ce qu'aucun test actuel ne fait

**Mois 2 — Observer**
- Ajoute Micrometer Tracing sur trois services
- Suis une requête `assistant → user → content` de bout en bout
- Ajoute trois métriques métier

**Mois 3 — Construire seul**
- Prends `ai-service` en FastAPI, ou un dixième service Spring
- **Écris la décision avant le code** : quel port, quelles pannes, quels tests
- Compare ensuite avec ce que le projet aurait fait

## 11.2 Comment continuer à apprendre

| Source | Pourquoi |
|---|---|
| La doc Spring officielle | La seule à jour sur Boot 4 |
| Le code source de Spring | Ouvre `NimbusJwtDecoder`. C'est lisible, et c'est formateur |
| Les notes de version | Elles disent ce qui casse, avant que ça te casse |
| Ce dépôt | 24 commits qui disent chacun **pourquoi**, pas seulement quoi |

> **Le conseil qui compte le plus :** quand tu lis un tutoriel, demande-toi toujours « pour
> quelle version ? ». La moitié de ce qui est en ligne sur Spring Security ne compile plus
> en 7. Le code de ce projet, lui, tourne.

## 11.3 Ce que tu dois savoir faire en sortie

- [ ] Expliquer pourquoi une annotation Spring « ne fait rien » (proxy, ordre, condition)
- [ ] Lire un rapport d'évaluation de conditions
- [ ] Concevoir un port + adaptateur avant d'avoir le service réel
- [ ] Décider, pour chaque dépendance, entre dégrader et refuser
- [ ] Choisir le niveau de test le plus bas qui prouve ce qu'il faut prouver
- [ ] Repérer les 15 erreurs de la partie 10 en relecture de code
- [ ] Justifier une dénormalisation par écrit
- [ ] **Dire non** à une brique d'architecture prématurée

---

## Sources

Faits vérifiés en ligne à la rédaction, en complément du code du projet :

- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 4 Migration: Breaking Changes, New Defaults — Java Code Geeks](https://www.javacodegeeks.com/2026/05/spring-boot-4-migration-breaking-changes-new-defaultsand-what-actually-broke.html)
- [Configuration Migrations — Spring Security](https://docs.spring.io/spring-security/reference/6.5/migration-7/web.html)
- [Spring Security 5 to 6 to 7 Migration](https://ankurm.com/spring-security-5-to-6-to-7-migration-guide/)
- [Spring Boot TestContext Cache Best Practices — rieckpil](https://rieckpil.de/spring-boot-testcontext-cache-best-practices/)
- [Mastering Testing Efficiency in Spring Boot — Zalando Engineering](https://engineering.zalando.com/posts/2023/11/mastering-testing-efficiency-in-spring-boot-optimization-strategies-and-best-practices.html)
- [Testcontainers — Spring Boot Reference](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Virtual Threads vs WebFlux: Java Concurrency in 2026](https://plus8soft.com/blog/virtual-threads-vs-webflux/)
- [Java Virtual Threads Complete Guide — Marc Nuri](https://blog.marcnuri.com/java-virtual-threads-project-loom-complete-guide)
