# Formation 4 — Architecture distribuée : gateway, BFF, événements

**Objectif :** comprendre les briques que la roadmap du projet dit de ne pas construire
tout de suite — et savoir décider **quand** elles deviennent nécessaires.

---

## Avant tout : une mise au point

La roadmap dit « explicitement pas maintenant » pour la gateway, le BFF, Kafka, le service
discovery et le config server.

**C'est un conseil de construction, pas un conseil d'apprentissage.** J'ai conflé les
deux dans la formation 3, et c'était une erreur : tu ne peux pas décider *quand* poser une
gateway si tu ne sais pas *ce qu'elle fait*.

La compétence senior, ce n'est pas « je ne connais pas Kafka donc je ne l'utilise pas ».
C'est **« je connais Kafka, je sais exactement ce qu'il résout, et je peux dire pourquoi
mon système n'en a pas encore besoin »**. La première phrase est de l'ignorance, la seconde
est un jugement.

Cette formation te donne le jugement.

---

## Sommaire

| Partie | Sujet |
|---|---|
| 0 | Les versions, et un piège |
| 1 | API Gateway |
| 2 | BFF — et pourquoi ce n'est pas la même chose |
| 3 | Service discovery |
| 4 | Configuration centralisée |
| 5 | Résilience : disjoncteur, cloisons, reprises |
| 6 | Événements et Kafka |
| 7 | Transactions distribuées : la saga |
| 8 | Traçage distribué |
| 9 | Les signaux : quand introduire quoi |
| 10 | Ce que ça donnerait sur Ojino |

---

# Partie 0 — Les versions, et un piège

**Spring Boot 4.1.0**, sortie le **11 juin 2026**, est la version stable actuelle. C'est
celle du projet — tu es à jour.

Pour tout ce qui suit, il te faut **Spring Cloud**, qui est un *train de versions* séparé.

| Ce que tu utilises | Ce qu'il te faut |
|---|---|
| Spring Boot 4.1.x | **Spring Cloud 2025.1.2** (Oakwood) |
| Spring Boot 4.0.x | Spring Cloud 2025.1.1 ou .2 |

> ⚠️ **Le piège :** Spring Cloud **2025.1.1 ne fonctionne pas** avec Boot 4.1.x. Il faut au
> minimum **2025.1.2**. C'est le genre d'incompatibilité qui coûte une demi-journée si on
> ne le sait pas.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2025.1.2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**La règle générale :** vérifie toujours la matrice de compatibilité Spring Boot ↔ Spring
Cloud avant d'ajouter une dépendance Cloud. Les deux n'évoluent pas au même rythme.

---

# Partie 1 — API Gateway

## 1.1 Le problème qu'elle résout

Aujourd'hui, ton application mobile doit connaître **neuf adresses** :

```
auth      → :8081        notification → :8087
user      → :8082        engagement   → :8088
content   → :8083        assistant    → :8089
learning  → :8084        realtime     → :8090
planning  → :8085
media     → :8086
```

Ça marche en développement. En production, ça veut dire neuf noms de domaine, neuf
certificats TLS, neuf configurations CORS, et une application cliente qui doit être
redéployée si tu déplaces un service.

**Une gateway, c'est une seule porte d'entrée** qui route vers les services :

```
                    ┌──────────────┐
Client ──── :443 ───│   Gateway    │───┬── auth-service
                    └──────────────┘   ├── user-service
                                       └── ...
```

## 1.2 Ce qu'elle fait vraiment

| Rôle | Détail |
|---|---|
| **Routage** | `/api/v1/auth/**` → `auth-service` |
| **Terminaison TLS** | Un seul certificat au lieu de neuf |
| **CORS** | Une seule configuration au lieu de neuf |
| **Limitation de débit** | Avant que la requête n'atteigne un service |
| **Authentification en amont** | Rejeter un token invalide une fois, pas neuf |
| **Observabilité** | Un point unique pour mesurer la latence globale |

## 1.3 Spring Cloud Gateway concrètement

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth
          uri: http://auth-service:8081
          predicates:
            - Path=/api/v1/auth/**
        - id: content
          uri: http://content-service:8083
          predicates:
            - Path=/api/v1/reference/**,/api/v1/curriculum/**
```

**Les prédicats** décident si une route s'applique : chemin, méthode, en-tête, hôte, heure.
**Les filtres** transforment la requête ou la réponse.

```yaml
          filters:
            - StripPrefix=1                    # retire un segment
            - AddRequestHeader=X-Source,gateway
            - name: RequestRateLimiter          # limitation de débit
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
```

## 1.4 Deux pièges

**Piège n°1 — Spring Cloud Gateway est réactif.** Il est bâti sur WebFlux, pas sur MVC.
Tu ne peux pas y mettre du code bloquant sans annuler son intérêt. C'est un service à part,
avec son propre modèle de programmation.

> Depuis peu il existe une variante MVC (`spring-cloud-starter-gateway-mvc`) si tu préfères
> rester sur le modèle bloquant. Plus simple à écrire, moins efficace sous forte charge.

**Piège n°2 — la gateway ne doit contenir aucune règle métier.** Si elle commence à décider
de quelque chose, tu as créé un point de couplage central que tout le monde devra modifier.
C'est exactement le reproche fait aux ESB d'il y a quinze ans.

## 1.5 Faut-il valider le token à la gateway ?

Deux écoles :

| Approche | Avantage | Inconvénient |
|---|---|---|
| **Valider à la gateway seulement** | Un seul point, services simplifiés | Un service atteint autrement est sans défense |
| **Valider partout** | Défense en profondeur | Duplication |

**Le bon choix :** valider à la gateway **et** dans chaque service. C'est ce que fait le
projet aujourd'hui (chaque service valide), et ajouter la gateway ne doit rien retirer.

> **Le principe :** ne suppose jamais que le seul chemin vers ton service est celui que tu
> as prévu. Une requête interne mal configurée, un port exposé par erreur, et la gateway
> est contournée.

---

# Partie 2 — BFF, et pourquoi ce n'est pas la même chose

C'est la confusion la plus fréquente. **Une gateway route. Un BFF compose.**

## 2.1 Le problème du BFF

Ton écran d'accueil mobile affiche : le prénom, la série en cours, les 3 prochaines
séances, et le nombre de notifications non lues.

Sans BFF, l'application fait **quatre appels** :

```
GET /api/v1/profile/me              → user-service
GET /api/v1/engagement/streak       → engagement-service
GET /api/v1/planning/today          → planning-service
GET /api/v1/notifications/unread-count → notification-service
```

Sur un réseau mobile camerounais à 200 ms de latence, ça fait **800 ms** avant le premier
pixel — et quatre occasions d'échouer.

**Un BFF fait un seul appel** et compose côté serveur, où la latence entre services est de
2 ms :

```
GET /bff/mobile/home  →  { profile, streak, sessions, unreadCount }
```

## 2.2 « Backend For Frontend » : un par client

Le nom le dit : **un BFF par type de client**, pas un pour tous.

```
Mobile  →  bff-mobile  ─┐
                        ├──→ les 9 services
Web     →  bff-web     ─┘
```

Pourquoi ? Parce que l'écran d'accueil web et l'écran d'accueil mobile n'affichent pas la
même chose. Un BFF unique redeviendrait un compromis qui ne convient à personne — et tu
aurais recréé le problème que tu voulais résoudre.

## 2.3 Gateway et BFF ensemble

Ils ne s'excluent pas :

```
Client ──→ Gateway ──┬──→ BFF mobile ──→ services
                     └──→ services (appels directs simples)
```

La gateway fait le routage et la sécurité. Le BFF fait la composition. Un appel simple
n'a pas besoin de passer par le BFF.

## 2.4 Le vrai danger du BFF

**Il attire la logique métier comme un aimant.**

« Tant qu'à composer, autant calculer ça ici… » — et six mois plus tard, ton BFF contient
des règles qui devraient être dans les services, dupliquées entre le BFF mobile et le BFF
web.

> **La règle :** un BFF **appelle, agrège, reformate**. Il ne calcule rien. Si tu as besoin
> d'une règle, elle appartient au service qui possède la donnée.

---

# Partie 3 — Service discovery

## 3.1 Le problème

Dans ton `application.properties` :

```properties
ojino.downstream.user.base-url=${USER_SERVICE_URL:http://localhost:8082}
```

Une adresse en dur, injectée par variable d'environnement. Ça marche tant que :
- il y a **une seule instance** de chaque service
- son adresse **ne change pas**

Dès que tu lances trois instances de `content-service` pour tenir la charge, il faut savoir
laquelle appeler — et que faire quand l'une meurt.

## 3.2 Les deux approches

**Approche 1 — un registre (Eureka, Consul)**

Chaque service s'enregistre au démarrage. Les autres demandent au registre où le trouver.

```java
@LoadBalanced   // résout "content-service" via le registre
@Bean RestClient.Builder loadBalancedBuilder() { ... }
```

```java
.baseUrl("http://content-service")   // pas d'adresse, un nom logique
```

**Approche 2 — l'orchestrateur s'en charge (Kubernetes)**

Kubernetes a un DNS interne. `http://content-service` résout tout seul vers l'un des pods
sains, avec répartition de charge.

## 3.3 Le conseil pratique

> **Si tu déploies sur Kubernetes, tu n'as pas besoin d'Eureka.** Le service discovery est
> déjà là, et ajouter un registre applicatif duplique une fonction de l'infrastructure.
>
> Eureka garde du sens hors Kubernetes : machines virtuelles, déploiement classique,
> plusieurs environnements hétérogènes.

C'est pour ça que la roadmap dit d'attendre : le choix dépend d'une décision de déploiement
qui n'est pas encore prise.

---

# Partie 4 — Configuration centralisée

## 4.1 Le problème

`OJINO_JWT_SECRET` doit être **identique** dans neuf services. Aujourd'hui, c'est neuf
variables d'environnement à tenir synchronisées à la main.

Le jour où tu le fais tourner, il faut modifier neuf déploiements sans en oublier un — et
un oubli signifie qu'un service rejette tous les tokens.

## 4.2 Spring Cloud Config

Un service qui sert la configuration, adossé à un dépôt Git :

```
config-repo/
├── application.yml           commun à tous
├── auth-service.yml
├── content-service.yml
└── application-prod.yml      surcharge par profil
```

Chaque service la lit au démarrage. Avantages : versionné, auditable, un seul endroit.

## 4.3 L'alternative moderne

Sur Kubernetes, les **ConfigMaps** et **Secrets** font la même chose, gérés par
l'infrastructure. Avec un gestionnaire de secrets (Vault, AWS Secrets Manager) pour ce qui
est sensible.

> **Le vrai enjeu n'est pas la centralisation, c'est la rotation.** Pouvoir changer un
> secret sans redéployer, et sans interruption. C'est ça qu'il faut chercher, quel que soit
> l'outil.

---

# Partie 5 — Résilience

## 5.1 Le disjoncteur

**Le problème :** `content-service` devient lent. `user-service` l'appelle et attend. Ses
threads se remplissent d'attente. `user-service` tombe. Puis `assistant-service`, qui
appelle `user-service`.

**Un service lent en fait tomber trois.** C'est la panne en cascade, et c'est la façon
n°1 dont un système distribué meurt.

**Le disjoncteur** compte les échecs. Au-delà d'un seuil, il **ouvre** le circuit : les
appels suivants échouent immédiatement, sans attendre. Après un délai, il laisse passer un
appel d'essai pour voir si le service est rétabli.

```
FERMÉ ──(trop d'échecs)──→ OUVERT ──(après délai)──→ SEMI-OUVERT
  ↑                                                       │
  └───────────────(l'essai réussit)───────────────────────┘
```

```java
@CircuitBreaker(name = "content", fallbackMethod = "contentUnavailable")
public LevelView requireLevel(String system, String level) { ... }

private LevelView contentUnavailable(String system, String level, Throwable t) {
    return null;   // ou une valeur dégradée
}
```

## 5.2 Ce que le projet fait déjà

Les délais d'attente sont partout — c'est le premier niveau, et le plus important.

```java
factory.setConnectTimeout(Duration.ofSeconds(2));
factory.setReadTimeout(Duration.ofSeconds(3));
```

Et la dégradation gracieuse est déjà en place :

📂 `LearningClient` dans `planning-service` renvoie une liste vide plutôt que d'échouer.
C'est un **repli manuel** — le disjoncteur l'automatiserait et éviterait de réessayer un
service qu'on sait tombé.

## 5.3 La cloison

Séparer les ressources par dépendance : si les appels vers `content-service` ont leur
propre pool de threads, leur saturation ne bloque pas les appels vers `user-service`.

Avec les threads virtuels, ce problème s'atténue beaucoup — c'est une raison de plus de les
activer avant d'ajouter des cloisons.

## 5.4 Les reprises : attention

**Ne réessaie jamais une opération non idempotente.**

Réessayer un `GET` est sans danger. Réessayer un `POST /messages` peut créer deux messages
et facturer deux fois l'IA.

📂 C'est exactement pourquoi `notification-service` a une **clé de déduplication** : une
tâche rejouée ne produit pas deux notifications.

> **La règle :** conçois l'idempotence **avant** d'ajouter des reprises. L'inverse produit
> des doublons que personne ne comprend.

---

# Partie 6 — Événements et Kafka

## 6.1 Le problème que ça résout

Aujourd'hui, quand une séance se termine :

```
planning-service ──HTTP──→ engagement-service   (met à jour la série)
                 ──HTTP──→ learning-service     (enregistre l'activité)
                 ──HTTP──→ notification-service (félicite)
```

Trois appels synchrones. Si `engagement-service` est lent, **la fin de séance est lente**.
S'il est en panne, il faut décider quoi faire. Et demain, quand un quatrième service voudra
réagir, il faudra **modifier `planning-service`**.

**Avec un événement :**

```
planning-service ──publie──→ [ SessionCompleted ] ──→ engagement
                                                  ──→ learning
                                                  ──→ notification
                                                  ──→ (le prochain, sans rien modifier)
```

`planning-service` ne sait plus qui l'écoute. C'est **l'inversion de dépendance appliquée
au réseau**.

## 6.2 Kafka en trois idées

| Idée | Ce que ça change |
|---|---|
| **Journal persistant** | Les messages ne disparaissent pas à la lecture. Un consommateur peut rejouer depuis le début |
| **Partitions** | Le parallélisme. L'ordre n'est garanti **que dans une partition** |
| **Groupes de consommateurs** | Chaque groupe reçoit tous les messages ; dans un groupe, un seul consommateur par partition |

**La conséquence pratique la plus importante :** si l'ordre compte pour un utilisateur,
**utilise son identifiant comme clé de partition**. Sinon ses événements peuvent arriver
dans le désordre.

```java
kafkaTemplate.send("session.completed", userId, event);
//                                      ↑ clé = même partition = ordre garanti
```

## 6.3 RabbitMQ ou Kafka ?

| | RabbitMQ | Kafka |
|---|---|---|
| Modèle | File de messages | Journal d'événements |
| Après lecture | Le message disparaît | Il reste, rejouable |
| Routage | Riche (exchanges, routing keys) | Simple (topics) |
| Débit | Élevé | Très élevé |
| Complexité opérationnelle | Modérée | **Importante** |

> **Pour Ojino, RabbitMQ suffirait largement** — et coûterait bien moins cher à exploiter.
> Kafka se justifie quand tu veux rejouer l'historique, ou quand tu dépasses des centaines
> de milliers de messages par seconde. Ni l'un ni l'autre n'est le cas.

## 6.4 Les deux pièges qui font mal

**Piège n°1 — le monolithe distribué.**

C'est l'anti-motif le plus documenté : des chaînes d'appels **synchrones de plus de deux
sauts**. Tu as tous les inconvénients des microservices (latence réseau, pannes partielles)
et aucun de leurs avantages (déploiement indépendant), parce que tout est couplé.

**Le symptôme :** tu ne peux pas déployer un service sans en déployer trois autres.

**Piège n°2 — écrire dans Kafka depuis la gateway.**

Ça paraît élégant : la gateway pousse directement dans Kafka. En pratique, **les écritures
Kafka ne sont pas lentes, elles sont imprévisibles** — un rééquilibrage de partition, une
élection de leader, et ta latence explose au pire moment.

La solution courante : un petit service dédié à côté de la gateway, qui parle à Kafka.

## 6.5 Ce qu'un événement doit contenir

Deux styles, et le choix compte :

```json
// Notification d'événement : léger, mais le consommateur doit rappeler
{ "type": "SessionCompleted", "sessionId": "abc", "userId": "u1" }

// Transfert d'état : autonome, mais plus gros et vite périmé
{ "type": "SessionCompleted", "sessionId": "abc", "userId": "u1",
  "notionCode": "LIMITES", "minutes": 45, "completedAt": "..." }
```

> **Commence par le second.** Un événement autonome évite au consommateur de rappeler
> l'émetteur — ce qui recréerait le couplage synchrone que tu voulais supprimer.

---

# Partie 7 — Transactions distribuées : la saga

## 7.1 Le problème

Une inscription complète touche `auth-service` (créer le compte) et `user-service` (créer
le profil). Deux bases. **Aucune transaction possible.**

Si la seconde échoue, tu as un compte sans profil.

## 7.2 La saga

Une suite d'opérations locales, chacune avec sa **compensation** :

```
1. auth : créer le compte        →  compensation : supprimer le compte
2. user : créer le profil        →  compensation : supprimer le profil
3. notification : souhaiter la bienvenue  →  (rien à compenser)
```

Si l'étape 2 échoue, on exécute la compensation de l'étape 1.

**Deux styles :**

| Chorégraphie | Orchestration |
|---|---|
| Chaque service réagit aux événements | Un coordinateur pilote |
| Pas de point central | Le déroulé est lisible en un endroit |
| Devient vite illisible au-delà de 3 étapes | Un composant de plus à maintenir |

## 7.3 La vérité sur les sagas

**Elles sont difficiles.** Il faut penser à :
- la compensation de chaque étape (et certaines ne se compensent pas — un email envoyé ne
  se rappelle pas)
- l'idempotence de chaque étape
- l'état de la saga en cours si le coordinateur redémarre

> **Le conseil qui vaut le plus :** avant d'écrire une saga, demande-toi si tu peux
> **éviter le besoin**. Souvent, la bonne réponse est de regrouper les deux opérations dans
> le même service, ou d'accepter une cohérence différée avec une tâche de réconciliation.
>
> 📂 Le projet évite le problème : le profil est créé **paresseusement**, à la première
> lecture. Pas de saga, pas de compensation, pas d'état incohérent possible.

---

# Partie 8 — Traçage distribué

## 8.1 Le problème

Une requête vers l'assistant traverse : `gateway → assistant → user → content → learning`.
Elle prend 3 secondes. **Où ?**

Sans traçage, tu ouvres cinq fichiers de logs et tu compares des horodatages à la main.

## 8.2 La solution

Un **identifiant de corrélation** propagé dans les en-têtes HTTP à travers tous les
services. Chaque étape enregistre sa durée. Un outil (Zipkin, Jaeger, Grafana Tempo)
reconstitue l'arbre.

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
```

Spring Boot instrumente automatiquement `RestClient`, les contrôleurs, et ajoute l'identifiant
dans les logs.

```
2026-08-16 [assistant-service,a3f2c1,b7d9] INFO  Question reçue
2026-08-16 [user-service,a3f2c1,c1e4]      INFO  Profil lu
                          ↑ même trace, spans différents
```

## 8.3 Pourquoi c'est la première brique à ajouter

> De toutes celles de cette formation, **le traçage est celle qui rapporte le plus vite**.
> Elle ne change pas ton architecture, elle te permet de la comprendre. Et elle devient
> indispensable exactement au moment où le système devient difficile à déboguer — c'est-à-dire
> avant que tu aies besoin de tout le reste.

---

# Partie 9 — Les signaux : quand introduire quoi

C'est le cœur de cette formation. Chaque brique a un **déclencheur observable**.

| Brique | Introduis-la quand… | N'attends pas si… |
|---|---|---|
| **Traçage distribué** | Tu as ≥ 3 services qui s'appellent | — (fais-le tôt) |
| **Gateway** | Tu déploies en production, ou tu as > 3 clients externes | Un seul service exposé |
| **BFF** | Un écran fait ≥ 3 appels, ou le mobile et le web divergent | Les clients affichent la même chose |
| **Disjoncteur** | Une panne d'un service en a déjà fait tomber un autre | Tes délais d'attente suffisent |
| **Service discovery** | Tu as > 1 instance d'un service | Kubernetes le fait déjà |
| **Config centralisée** | Un changement de secret demande > 3 déploiements | Peu de services, peu de secrets |
| **Événements** | Un événement intéresse ≥ 3 consommateurs, ou une chaîne synchrone dépasse 2 sauts | Un seul consommateur |
| **Saga** | Une opération doit être cohérente sur ≥ 2 bases | Tu peux l'éviter en regroupant |
| **Kafka plutôt que RabbitMQ** | Tu veux rejouer l'historique, ou > 100k msg/s | Tout le reste |

## 9.1 Le principe général

> **Chaque brique distribuée résout un problème et en crée un autre.**
>
> Une gateway ajoute un point de panne unique. Kafka ajoute un système à exploiter,
> surveiller et mettre à jour. Une saga ajoute des chemins d'exécution que personne ne teste
> jamais tous.
>
> Introduis-la quand le problème qu'elle résout **te coûte déjà plus cher** que celui
> qu'elle apporte. Pas avant.

## 9.2 Le test qui ne trompe pas

Demande-toi : **« quel incident réel cette brique aurait-elle évité le mois dernier ? »**

Si tu ne peux pas nommer l'incident, tu n'en as pas encore besoin.

---

# Partie 10 — Ce que ça donnerait sur Ojino

Concrètement, dans l'ordre où je le ferais.

## Étape 1 — Traçage (maintenant)

Neuf services qui s'appellent, aucune visibilité de bout en bout. C'est déjà un problème,
et il empire à chaque service ajouté.

**Coût :** une dépendance, une configuration. **Gain :** immédiat.

## Étape 2 — Gateway (au premier déploiement)

Une seule entrée, un certificat, une configuration CORS. Le jour où tu déploies, c'est
ça ou neuf sous-domaines.

```yaml
routes:
  - id: auth        → :8081   Path=/api/v1/auth/**
  - id: user        → :8082   Path=/api/v1/profile/**,/api/v1/onboarding/**
  - id: content     → :8083   Path=/api/v1/reference/**,/api/v1/curriculum/**
  - id: assistant   → :8089   Path=/api/v1/assistant/**
  - id: realtime    → :8090   Path=/ws          # WebSocket, attention à la config
```

⚠️ **Le WebSocket demande une configuration particulière** : la gateway doit laisser passer
le passage de protocole et ne pas appliquer de délai d'écriture. C'est le piège classique
qui casse `realtime-service` le jour où on ajoute la gateway.

## Étape 3 — BFF mobile (quand l'app existera)

L'écran d'accueil fera quatre appels. Sur un réseau mobile, ça se voit.

## Étape 4 — Événements (quand un 3ᵉ service écoutera)

Aujourd'hui, une séance terminée intéresse `engagement` et `learning`. **Deux.** Le jour où
un troisième arrive, `planning-service` devra être modifié — et c'est le signal.

Premier événement à extraire : `SessionCompleted`. RabbitMQ, pas Kafka.

## Étape 5 — Le reste

Service discovery et config centralisée dépendent de la façon dont tu déploieras. Sur
Kubernetes, tu n'auras probablement besoin ni de l'un ni de l'autre.

---

## Ce que tu dois savoir faire en sortie

- [ ] Expliquer la différence entre une gateway et un BFF, sans hésiter
- [ ] Dire pourquoi un BFF ne doit contenir aucune règle métier
- [ ] Décrire les trois états d'un disjoncteur et ce qui les déclenche
- [ ] Expliquer pourquoi une clé de partition Kafka détermine l'ordre
- [ ] Choisir entre RabbitMQ et Kafka avec un argument, pas une préférence
- [ ] Reconnaître un monolithe distribué à son symptôme
- [ ] Nommer, pour chaque brique, l'incident qu'elle aurait évité
- [ ] **Justifier une absence** aussi bien qu'une présence

---

## Sources

Faits vérifiés en ligne à la rédaction :

- [Spring Boot — page projet officielle](https://spring.io/projects/spring-boot) (4.1.0 en GA)
- [Spring Cloud 2025.1.2 (Oakwood) Has Been Released](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released/)
- [Spring Version Compatibility Cheatsheet](https://stevenpg.com/posts/spring-compat-cheatsheet/)
- [Spring Boot Versions, EOL Dates, and Latest Releases — HeroDevs](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026)
- [Pattern: API Gateway / Backends for Frontends — microservices.io](https://microservices.io/patterns/apigateway.html)
- [36 Microservices Patterns & Anti-Patterns (2026)](https://appscale.blog/en/blog/microservices-patterns-anti-patterns-master-index-2026)
- [Why Writing to Kafka at the API Gateway Breaks Your Latency Budget](https://medium.com/@openresty/why-writing-to-kafka-at-the-api-gateway-breaks-your-latency-budget-b786881dd7a3)
- [Microservices Communication Patterns](https://knowledgelib.io/software/system-design/microservices-communication/2026)
