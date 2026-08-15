# Tutoriel — construire `assistant-service`

Guide de construction du 9ᵉ service. Il donne la structure, les décisions et les
pièges. **Le code, c'est toi qui l'écris.**

Port **8089**, base `ojino_assistant`.

---

## 0. Ce que ce service fait — et ne fait pas

| Il possède | Il ne possède pas |
|---|---|
| Les conversations et leur historique | **L'inférence** — c'est `ai-service`, en FastAPI |
| Les quotas et la limitation de débit | Le contenu pédagogique — c'est `content-service` |
| Les garde-fous et le registre de langage | Ce que l'élève maîtrise — c'est `learning-service` |
| La construction du contexte | Le profil — c'est `user-service` |
| La traçabilité et les retours | |

> **La règle à ne pas casser :** les quotas et les garde-fous vivent **ici**, en Spring.
> Jamais dans FastAPI. `ai-service` fera l'inférence, point. Si tu laisses la règle métier
> migrer vers le service d'IA, tu ne pourras plus la tester sans appeler un modèle, ni la
> changer sans redéployer l'IA.

**Tu écris ce service avant `ai-service`, donc avec un moteur bouchonné.** C'est
volontaire : toute la logique — quotas, garde-fous, contexte, historique — devient
testable sans dépendre du modèle ni le payer.

---

## 1. Le squelette

Repars de `engagement-service`, c'est le plus proche structurellement (il a déjà le filtre
interne et un client HTTP sortant) :

```bash
cp -r engagement-service assistant-service
# puis renomme : pom.xml, package, application.properties
```

Garde tel quel :
- `config/SecurityConfig.java`, `SecurityProperties.java`, `CorsProperties.java`, `Role.java`
- `config/InternalApiFilter.java`
- `web/GlobalExceptionHandler.java`
- `exception/ApiException.java` (vide-le de ses méthodes, garde la forme)

**Checkpoint 1** — `./mvnw compile` passe, le service démarre sur 8089.

---

## 2. Le port `AiEngine` — commence par là

C'est le 4ᵉ port du projet. Tu connais déjà le motif :

| Port | Implémentation de dev | Implémentation réelle |
|---|---|---|
| `SmsSender` | logs | Twilio |
| `MediaStorage` | disque | S3 |
| `NotificationSender` | logs | FCM |
| **`AiEngine`** | **canned** | **`ai-service` (FastAPI)** |

### L'interface

```java
public interface AiEngine {
    AiReply complete(AiRequest request);
}
```

Réfléchis à ce que porte `AiRequest`. Au minimum :
- les messages de la conversation (rôle + contenu)
- le contexte pédagogique construit à l'étape 4
- le registre de langage (étape 6)

Et `AiReply` :
- le texte
- les **tokens consommés** — tu en as besoin pour les quotas et le coût
- le modèle utilisé — pour la traçabilité
- éventuellement les sources citées

> **Piège :** ne fais pas retourner un `String`. Le jour où tu veux facturer, débugger ou
> tracer, tu devras reprendre tous les appelants. Un record coûte cinq minutes maintenant.

### L'implémentation bouchonnée

Une classe `CannedAiEngine` qui renvoie une réponse fabriquée, en reprenant des morceaux
de la question et du contexte. Deux exigences :

1. **Qu'elle soit reconnaissable** — préfixe-la clairement, comme `LoggingSmsSender` écrit
   `[SMS SIMULE]`. Personne ne doit croire que c'est une vraie réponse.
2. **Qu'elle consomme des tokens plausibles** — sinon tu ne pourras pas éprouver les
   quotas.

Sélection par propriété, comme dans `media-service` :

```properties
ojino.assistant.engine=${AI_ENGINE:canned}
```

⚠️ **N'utilise pas `@ConditionalOnMissingBean`.** Je m'y suis fait prendre dans
`media-service` : c'est ordre-dépendant hors autoconfiguration, et le contexte refuse de
démarrer. `@ConditionalOnProperty` est déterministe.

**Checkpoint 2** — un test injecte `AiEngine`, appelle `complete()`, reçoit une réponse.

---

## 3. Conversations et messages

### Le modèle

Deux documents, pas un seul :

- **`Conversation`** — id, userId, titre, dates, éventuellement le sujet (notion, chapitre)
- **`Message`** — id, conversationId, rôle (`USER` / `ASSISTANT` / `SYSTEM`), contenu, tokens, date

> **Pourquoi séparer.** Une conversation peut atteindre des centaines de messages.
> Les imbriquer ferait réécrire un document de plus en plus gros à chaque échange, et
> Mongo plafonne un document à 16 Mo. Applique le critère de la formation 2 : la donnée
> croît sans limite → collection séparée.

### Les index

Tu en as besoin d'au moins deux :
- `{conversationId: 1, createdAt: 1}` sur les messages — c'est la lecture principale
- `{userId: 1, updatedAt: -1}` sur les conversations — la liste

### La fenêtre de contexte

Tu ne peux pas renvoyer 400 messages au modèle : ça coûte cher et ça dépasse la limite.

Décide d'une stratégie et **écris pourquoi** :
- les N derniers messages (simple, suffisant au début)
- ou un résumé des anciens + les N derniers (mieux, plus tard)

Commence par le simple. Mais isole-le dans une classe dédiée (`ContextWindow`), pour
pouvoir changer sans toucher au reste.

**Checkpoint 3** — créer une conversation, poster un message, relire l'historique.

---

## 4. Le contexte pédagogique — le cœur du service

C'est ce qui distingue un assistant scolaire d'un chatbot générique.

Avant chaque appel au modèle, tu croises **trois services** :

| Source | Ce que tu en tires | Pourquoi |
|---|---|---|
| `user-service` `/api/v1/profile/me` | âge, niveau, filière, matières | Le registre de langage en dépend |
| `learning-service` `/api/v1/learning/gaps` | où l'élève bloque | Pour expliquer à partir de ce qu'il maîtrise |
| `content-service` `/api/v1/curriculum/...` | la notion, ses prérequis | Pour ancrer la réponse dans le programme |

### Comment appeler

**Retransmets le token de l'élève**, comme fait `planning-service`. Regarde
`ProfileClient` : il prend un `bearerToken` en paramètre et le repose en en-tête. Résultat,
le service ne peut lire que le profil de l'appelant.

### Les pannes

Décide, pour chaque source, ce qui se passe si elle ne répond pas :

- **`user-service` en panne** → tu ne connais pas l'âge → **refuse**. Répondre à un enfant
  de 6 ans avec le registre d'un lycéen est pire que ne pas répondre.
- **`learning-service` en panne** → tu perds les lacunes → **continue**. La réponse sera
  moins ciblée, mais utile.
- **`content-service` en panne** → tu perds l'ancrage → **continue**, en le signalant dans
  la traçabilité.

Regarde `LearningClient` dans `planning-service` pour le motif « renvoie une liste vide
plutôt que d'échouer ».

> **Piège de performance :** trois appels HTTP séquentiels avant chaque message, c'est
> lourd. Deux pistes : les paralléliser, ou mettre en cache le profil (il change rarement).
> Ne fais ni l'un ni l'autre tout de suite — mais **laisse la place** en isolant la
> construction du contexte dans un `ContextBuilder`.

**Checkpoint 4** — un test avec les trois clients bouchonnés produit un contexte complet,
et un autre vérifie qu'une panne de `user-service` refuse la requête.

---

## 5. Les quotas — l'IA coûte de l'argent

C'est la partie qu'on regrette de ne pas avoir faite le jour où la facture arrive.

### Ce qu'il faut compter

Pas seulement le nombre de messages : les **tokens**. Un élève qui colle trois pages de
cours consomme cent fois plus qu'un qui pose une question courte.

Modélise un `UsageQuota` par utilisateur et par période :
- messages envoyés
- tokens consommés
- période (jour, ou mois)

### Les seuils

Externalise-les, comme partout ailleurs dans le projet :

```properties
ojino.assistant.quota.daily-messages=50
ojino.assistant.quota.daily-tokens=100000
ojino.assistant.quota.max-input-chars=4000
```

> **Réfléchis à la différenciation.** Un élève de prépa consomme légitimement plus qu'un
> CP. Tu peux faire varier le quota par cycle — mais **ne le fais pas au premier jet**.
> Commence uniforme, mesure, puis ajuste. Un quota différencié sans données est une
> supposition déguisée en règle.

### L'ordre des vérifications

Le même raisonnement que `DeliveryGate` dans `notification-service` :

1. **La taille de l'entrée** — refuse tout de suite, avant tout appel
2. **Le quota** — refuse avant l'appel au modèle, pas après
3. **Puis** l'appel

Vérifier le quota après l'appel te ferait payer la requête que tu refuses. C'est exactement
l'erreur que la limite de taille de `media-service` évite déjà pour les fichiers.

### Compter après coup

Les tokens réels ne sont connus **qu'après** la réponse. Donc : tu vérifies avec une
estimation avant, tu enregistres le réel après. Accepte le léger dépassement — l'alternative
(refuser un dépassement de 2 %) frustre pour rien.

**Checkpoint 5** — un test épuise le quota et vérifie que le moteur n'est **jamais appelé**
(`verifyNoInteractions`).

---

## 6. Les garde-fous — la partie la plus difficile

C'est ce que j'ai signalé comme la contrainte la plus dure du produit : **l'écart de
registre entre la maternelle et la prépa**.

### Le registre de langage

Tu as `EducationCycle` dans `content-service`. Définis ici un `LanguageRegister` qui en
dérive :

| Cycle | Registre attendu |
|---|---|
| `EARLY_YEARS` | Phrases très courtes, vocabulaire concret, ton chaleureux, aucun jargon |
| `COLLEGE` | Phrases simples, un concept à la fois, exemples du quotidien |
| `HIGH_SCHOOL` | Vocabulaire disciplinaire assumé, raisonnement structuré |
| `PREPA` | Rigueur, densité, notations formelles |
| `UNIVERSITY` | Références, nuance, autonomie supposée |

**Ce n'est pas un paramètre qu'on ajoute à la fin.** Ça se conçoit maintenant, parce que
ça change la forme de `AiRequest` et le contenu du prompt système.

### Les refus

Décide ce que l'assistant **ne fait pas** :
- il n'écrit pas le devoir à la place de l'élève — il l'aide à le faire
- il ne répond pas hors sujet scolaire
- il ne donne pas de conseil médical ou psychologique — il oriente

Écris ces règles comme des **constantes ou un fichier**, pas noyées dans un prompt. Elles
seront relues par quelqu'un d'autre que toi.

### Le filtrage

Deux passes :
- **En entrée** — longueur, contenu manifestement hors sujet
- **En sortie** — c'est là que ça compte vraiment. Un modèle peut produire n'importe quoi,
  et ton public a six ans.

Isole ça dans un `SafetyGuard`, testable sans modèle.

> **Sois honnête sur les limites.** Un filtre par mots-clés attrape le grossier et rate le
> subtil. Écris-le dans le code : c'est une première barrière, pas une garantie. La vraie
> modération viendra avec `ai-service` et un modèle dédié.

**Checkpoint 6** — des tests vérifient qu'un même contexte produit des registres différents
selon le cycle, et que les refus se déclenchent.

---

## 7. L'API

### Client

```
POST   /api/v1/assistant/conversations              créer
GET    /api/v1/assistant/conversations              lister
GET    /api/v1/assistant/conversations/{id}         historique
POST   /api/v1/assistant/conversations/{id}/messages  poser une question
DELETE /api/v1/assistant/conversations/{id}         supprimer
POST   /api/v1/assistant/messages/{id}/feedback     pouce haut / bas
GET    /api/v1/assistant/quota                      ce qu'il reste
```

Toutes sur le `sub` du token. **Vérifie l'appartenance** de la conversation — une
conversation est ce qu'il y a de plus personnel dans ce produit.

### Le retour utilisateur

`POST /messages/{id}/feedback` avec un pouce et une raison facultative. C'est la base de
l'évaluation qualité, et ça ne coûte presque rien à poser maintenant.

### Streaming ?

**Pas encore.** Le streaming arrivera avec `realtime-service` en Go. Pour l'instant, une
réponse synchrone suffit — et ça t'évite de concevoir deux fois.

**Checkpoint 7** — le parcours complet en curl : créer, poser, relire, noter.

---

## 8. Les tests

Suis la répartition du reste du projet : **teste les logiques pures, mocke le reste.**

| À tester sérieusement | Pourquoi |
|---|---|
| `QuotaPolicy` | L'argent. Épuisement, remise à zéro, dépassement toléré |
| `SafetyGuard` | Le public. Refus, filtrage, cas limites |
| `LanguageRegister` | La contrainte produit. Un cycle = un registre |
| `ContextBuilder` | Les pannes partielles, la panne bloquante |
| `ContextWindow` | La troncature, l'ordre des messages |

À ne pas tester : le `CannedAiEngine` (c'est un bouchon), les getters, les mappings DTO.

Vise **25-30 tests**, aucun ne demandant Mongo ni réseau — comme les huit autres services.

---

## 9. Checklist finale

| | |
|---|---|
| ☐ | `AiEngine` est une interface, l'implémentation se choisit par propriété |
| ☐ | Le service compile et démarre **sans** `ai-service` |
| ☐ | Les quotas sont vérifiés **avant** l'appel au moteur |
| ☐ | Un test prouve que le moteur n'est pas appelé quand le quota est épuisé |
| ☐ | Le registre de langage dérive du cycle, et c'est testé |
| ☐ | Une panne de `user-service` refuse la requête ; les deux autres la dégradent |
| ☐ | Le token de l'élève est retransmis aux services consultés |
| ☐ | L'appartenance de la conversation est vérifiée |
| ☐ | Les DTO n'exposent ni prompt système ni détail de modèle |
| ☐ | Ce qui est configurable est dans `application.properties`, pas en dur |
| ☐ | `./mvnw test` passe, sans Mongo |

---

## Ce qu'il te restera à faire ensuite

Le service sera complet mais parlera à un bouchon. Ensuite :

1. **`ai-service`** en FastAPI — il implémente le contrat de `AiEngine` derrière HTTP
2. Une classe `RemoteAiEngine` ici, sélectionnée par `ojino.assistant.engine=remote`
3. **Rien d'autre ne change** — c'est tout l'intérêt d'avoir commencé par le port

---

## Où regarder dans le code existant

| Ce que tu cherches | Va voir |
|---|---|
| Le motif port + adaptateur | `auth-service` → `otp/SmsSender.java` |
| La sélection par propriété | `media-service` → `config/StorageConfig.java` |
| Un client HTTP avec délais | `planning-service` → `config/DownstreamClientConfig.java` |
| Retransmettre le token | `planning-service` → `client/ProfileClient.java` |
| Dégrader au lieu d'échouer | `planning-service` → `client/LearningClient.java` |
| Une décision en plusieurs règles ordonnées | `notification-service` → `service/DeliveryGate.java` |
| Un algorithme pur bien testé | `engagement-service` → `service/StreakPolicy.java` |
| Le filtre des routes internes | `notification-service` → `config/InternalApiFilter.java` |

---

## Un dernier mot

L'ordre proposé — port, puis domaine, puis contexte, puis quotas, puis garde-fous — n'est
pas arbitraire. Chaque étape est testable avant de passer à la suivante, et **aucune ne
dépend de `ai-service`**.

Si tu commences par l'appel au modèle, tu te retrouveras à débugger trois choses à la fois :
ton code, le prompt, et le modèle. En finissant par lui, tu n'auras plus qu'une inconnue.
