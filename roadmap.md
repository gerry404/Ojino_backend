# Roadmap backend

Ordre d'exécution. Pas de dates : on avance service par service, chacun terminé et testé
avant le suivant.

**Règle :** tous les services Spring Boot d'abord, puis FastAPI, puis Go.

---

## Vue d'ensemble

| # | Service | Stack | Statut |
|---|---|---|---|
| 1 | `auth-service` | Spring Boot | ✅ fait |
| 2 | `user-service` | Spring Boot | ✅ fait |
| 3 | `content-service` | Spring Boot | ✅ code fait — le contenu reste à saisir |
| 4 | `learning-service` | Spring Boot | ✅ fait |
| 5 | `planning-service` | Spring Boot | ✅ fait |
| 6 | `media-service` | Spring Boot | ✅ fait |
| 7 | `notification-service` | Spring Boot | ✅ fait |
| 8 | `assistant-service` | Spring Boot | ✅ fait |
| 9 | `engagement-service` | Spring Boot | ✅ fait |
| 10 | `ai-service` | FastAPI | ✅ fait — **branché** via `RemoteAiEngine` |
| 11 | `realtime-service` | Go / Gin | 🚧 hub et WebSocket faits, aucun producteur |
| 12 | `support-service` | Spring Boot | ✅ fait |

---

## 1. auth-service ✅

**Possède :** identité, sessions, rôles.

Fait : 4 providers (email, SMS, Google, Apple), access + refresh avec rotation et
détection de vol, back-office des comptes.

**Reste ouvert** (à reprendre quand le besoin se présente) : vérification d'email,
réinitialisation de mot de passe, vrai fournisseur SMS, limitation de débit sur `/login`,
sessions visibles par l'utilisateur lui-même.

---

## 2. user-service ✅

**Possède :** profil scolaire de l'élève, parcours d'inscription.

Fait : onboarding en 8 étapes piloté par le serveur, référentiel scolaire configurable,
back-office.

**Reste ouvert :** rôle parent/tuteur rattaché à un élève, gestion multi-enfants.

---

## 3. content-service

**Possède :** le socle pédagogique — la taxonomie scolaire et le programme.

C'est le service le plus structurant du projet. Tout ce qui suit s'y accroche.

1. ✅ **Migrer le référentiel** (systèmes, niveaux, filières, matières) depuis `user-service`
2. ✅ **Un module Maven par cycle** — maternelle→CP, collège, lycée, prépa, université,
   avec le contrat `CurriculumModule` qui dit ce que chaque cycle demande à l'élève
3. **Chapitre** — rattaché à système + niveau + matière (+ filière si applicable), ordonné
3. **Notion** — le grain fin : « les limites », « l'accord du participe passé »
4. **Graphe de prérequis** entre notions
5. **Ressource** — leçon, fiche, vidéo, lien externe
6. **Exercice** — énoncé, corrigé, difficulté, rattaché à une notion
7. **Cycle éditorial** — brouillon / publié, versionnement léger
8. **Registre de langage par cycle** — maternelle, primaire, collège, lycée, supérieur
9. **Back-office éditorial** complet
10. **API de consultation** par niveau, matière, chapitre

**Dépend de :** rien.

**Deux points structurants à ne pas rater :**

- **La notion est l'unité de maîtrise.** Le suivi de progression, la révision espacée, la
  remédiation et le grounding de l'IA s'y rattachent tous. Un grain trop grossier
  (le chapitre) rend la remédiation inutilisable ; trop fin, le contenu devient
  ingérable à produire.
- **Le graphe de prérequis** est ce qui permet de dire « tu bloques sur les dérivées parce
  que les limites ne sont pas acquises ». Sans lui, « rattraper son retard » — l'un des
  objectifs annoncés du produit — n'est pas implémentable.

**Décision prise :** le référentiel a été migré ici. `user-service` ne stocke plus que les
codes choisis par l'élève et appelle ce service pour les valider, en `RestClient`
synchrone.

La séparation a imposé un changement : le contrôle d'usage avant suppression aurait obligé
`content-service` à appeler `user-service`, qui l'appelle déjà — deux services qui
s'appellent mutuellement ne se déploient plus séparément. Les suppressions ont donc été
remplacées par de l'**archivage** : un élément archivé disparaît des choix mais reste
résolvable.

---

## 4. learning-service

**Possède :** ce que l'élève sait, ce qu'il a fait, ce qu'il doit réviser.

1. **Événement d'apprentissage** — a lu, a répondu juste/faux, a abandonné
2. **Maîtrise par notion** — score et confiance, recalculés à chaque événement
3. **Test de positionnement** à l'entrée
4. **Quiz et évaluations** — composition, passation, résultat
5. **Révision espacée** — prochaine échéance de révision par notion
6. **Détection des lacunes** via le graphe de prérequis
7. **Tableau de bord de progression** par matière et par chapitre
8. **API interne** — « que doit-il réviser », « où bloque-t-il »

**Dépend de :** `content-service`.

**Point d'attention :** c'est le service à plus fort volume d'écriture du projet. Modéliser
les événements en append-only et calculer la maîtrise à partir d'eux, plutôt que de
mettre à jour un score en place — sinon on ne peut ni rejouer, ni corriger un algorithme
de scoring a posteriori.

---

## 5. planning-service

**Possède :** quand l'élève travaille, et sur quoi.

1. **Échéance** — devoir, contrôle, examen officiel
2. **Session de travail planifiée** — notion + créneau + durée
3. **Génération du plan hebdomadaire** à partir des disponibilités et des priorités
4. **Replanification** quand une session est manquée
5. **Compte à rebours** vers les examens
6. **Suivi réel vs prévu** — démarrée, terminée, abandonnée
7. **Rappels** produits ici, envoyés plus tard par `notification-service`

**Dépend de :** `user-service` (disponibilités déjà collectées à l'onboarding),
`learning-service` (priorités de révision).

**Point d'attention :** la replanification est le vrai sujet. Un planning qui ne se
réajuste pas après deux sessions manquées devient culpabilisant et se fait abandonner.

---

## 6. media-service

**Possède :** les fichiers.

1. **Upload par URL pré-signée** (S3 ou compatible)
2. **Contrôles** — types autorisés, taille maximale
3. **Photo de profil** — débloque l'étape `PHOTO` de l'onboarding, aujourd'hui incomplète
4. **Photo d'exercice ou de devoir** — prérequis de la correction par l'IA
5. **Cycle de vie** — suppression, nettoyage des orphelins

**Dépend de :** rien.

Petit service, placé ici parce qu'il débloque deux choses en aval sans rien coûter.

---

## 7. notification-service

**Possède :** l'envoi de messages et les préférences.

1. **Canaux** — push (FCM / APNs), email, in-app
2. **Templates** et langue (`fr` et `en` — les deux systèmes scolaires livrés)
3. **Préférences par utilisateur et par type**
4. **Heures de silence** — ne pas notifier un enfant à 23 h
5. **File d'attente et reprises sur échec**
6. **Historique des envois**

**Dépend de :** rien. Ce sont les autres qui l'appellent.

**Point d'attention :** avec un public qui commence à la maternelle, les heures de silence
et le plafonnement du nombre de notifications ne sont pas du confort — c'est une
obligation, et probablement un point de conformité.

---

## 8. assistant-service

**Possède :** les conversations, les quotas, les garde-fous. **Pas l'inférence.**

1. **Conversation et messages** — historique persistant
2. **Construction du contexte** — âge et niveau (`user`), point de blocage (`learning`),
   contenu de référence (`content`)
3. **Port `AiEngine` + adaptateur bouchonné** — même motif que `SmsSender` : l'interface
   d'abord, l'implémentation réelle en étape 10
4. **Quotas et limitation de débit** — l'IA a un coût réel par requête
5. **Garde-fous** — registre de langage selon l'âge, filtrage, refus hors sujet
6. **Traçabilité** — ce qui a été demandé et répondu, pour audit et amélioration
7. **Retour utilisateur** — pouce haut/bas, base de l'évaluation qualité

**Dépend de :** `user-service`, `content-service`, `learning-service`.

**Conséquence de l'ordre choisi :** ce service sort avant `ai-service`, donc avec un
adaptateur bouchonné. C'est volontaire et sain — toute la logique métier, les quotas et
les garde-fous sont testables sans dépendre du modèle ni le payer.

---

## 9. engagement-service

**Possède :** motivation, régularité, bien-être.

1. **Série (streak)** et activité quotidienne
2. **Objectifs personnels** et suivi
3. **Badges et accomplissements**
4. **Check-in d'humeur** et de charge mentale
5. **Détection de décrochage ou de surcharge** — sessions manquées, humeur en baisse
6. **Contenus de gestion du stress** — respiration, pauses (stockés dans `content`)
7. **Relances de motivation** via `notification-service`

**Dépend de :** `learning-service`, `planning-service`, `notification-service`.

**Point d'attention :** la mécanique de série est à double tranchant. Chez un enfant, une
série cassée peut produire l'effet inverse de celui recherché. Prévoir dès la conception
un mécanisme de rattrapage ou de gel.

---

## 10. ai-service — FastAPI

Premier service hors Spring. Branché derrière le port `AiEngine` défini en étape 8.

1. **Passerelle** vers le ou les modèles
2. **RAG** sur le contenu de `content-service` — indexation, embeddings
3. **Adaptation du registre** selon l'âge — de la maternelle à la prépa
4. **Génération d'exercices et de quiz** rattachés à une notion
5. **Correction de devoirs** — texte, puis photo avec OCR
6. **Génération de plans de révision** — reste à faire
7. **Streaming des réponses**
8. **Garde-fous et évaluation** de la qualité des réponses

**Branchement :** `RemoteAiEngine` + `ojino.assistant.engine=remote`. Rien d'autre n'a
changé côté Spring — quotas, garde-fous, contexte et historique étaient écrits et testés
avant que le service existe. C'est ce que le port avait acheté.

**Point d'attention :** l'écart de registre entre la maternelle et la prépa est la
contrainte la plus dure du produit. Ce n'est pas un paramètre de prompt qu'on ajoute à la
fin — ça se conçoit dès le départ, et ça se teste.

---

## 11. realtime-service — Go / Gin

1. **WebSocket** authentifié par le même JWT que le reste
2. **Streaming** des réponses de l'assistant vers le client
3. **Notifications en direct**
4. **Présence**
5. **Salles d'étude partagées** — pomodoro à plusieurs
6. **Montée en charge** — hub et pub/sub entre instances

**Dépend de :** `auth-service` (validation du token), `assistant-service`,
`notification-service`.

## 12. support-service ✅

**Possède :** le centre d'aide. Port 8092, base `ojino_support`.

1. **FAQ bilingue** — question et réponse portées en français et en anglais dans
   le même document ; le back renvoie les deux, le front choisit
2. **Statut éditorial** — `DRAFT` / `PUBLISHED`, une création est toujours un
   brouillon
3. **Archivage** — aucune suppression exposée
4. **Routes séparées** — `/api/v1/faq` en lecture, `/api/v1/admin/faq` fermé à
   `ROLE_ADMIN` dans la chaîne de sécurité *et* par `@PreAuthorize`
5. **Ouverture configurable** — `ojino.support.faq.public-access` décide si la
   FAQ se lit sans jeton, sans toucher au code

**Dépend de :** `auth-service` seulement, pour la validation du token. Aucun
service ne l'appelle : pas de filtre interne.

**Reste à faire :** les demandes d'assistance (`SupportTicket`), paginées.

---

## Décisions transverses

À trancher au moment indiqué, pas avant.

| Quand | Décision | Recommandation |
|---|---|---|
| ~~Avant #3~~ | ~~Où vit le référentiel scolaire~~ | ✅ **Tranché** — migré dans `content-service` |
| ~~Avant #4~~ | ~~Comment les services se parlent~~ | ✅ **Tranché** — REST synchrone (`RestClient`), délais courts ; les événements quand un vrai besoin asynchrone apparaîtra, probablement à #9 |
| Avant #4 | Une base par service ou partagée | Une par service — c'est déjà le cas (`ojino_auth`, `ojino_user`) |
| Avant #8 | Secret partagé HS256 ou RS256 + JWKS | Passer à RS256 : à cinq services et plus, un secret symétrique qui fuit compromet tout |
| Avant #8 | Où vivent les quotas IA | Dans `assistant-service`, jamais dans `ai-service` — la règle métier reste côté Spring |
| ~~Avant le déploiement~~ | ~~MongoDB local~~ | ✅ **Fait** — `docker compose up -d`, un utilisateur par service, Actuator |
| Avant le déploiement | Reste de l'outillage : conteneurs des services, observabilité, CI | À traiter d'un bloc, une fois les services Spring terminés |

---

## Explicitement pas maintenant

- API Gateway
- BFF
- Service discovery (Eureka / Consul)
- Config server
- Bus d'événements (Kafka / RabbitMQ)
- CI/CD

Ces briques résolvent des problèmes qu'on n'a pas encore. Les poser trop tôt coûte du
temps et fige des choix avant d'avoir les informations pour les faire.
