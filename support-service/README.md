# support-service

Le centre d'aide. Une FAQ bilingue, rédigée par l'équipe, publiée quand elle est
prête. Port **8092**, base `ojino_support`.

Le service ne dépend que de l'`auth-service`, et seulement pour valider les
tokens. **Aucun autre service ne l'appelle** : pas de trafic interne, donc pas de
filtre interne.

## Le modèle éditorial

Une entrée de FAQ porte un `code` métier stable (`CHANGER_CLASSE`), une
catégorie, une question, une réponse, une position d'affichage — et deux champs
qui gouvernent tout le reste : `status` et `archived`.

**Une création est toujours un brouillon.** Le statut n'est pas dans la requête :
`FaqEntryUpsertRequest` ne le porte pas. Une entrée fraîchement saisie n'a été
relue par personne ; la publier d'emblée mettrait sous les yeux d'un utilisateur
un texte que personne n'a vérifié. Publier est une action séparée,
`PATCH /{code}/publish`.

**Le `code` est immuable.** À la modification, celui du corps est ignoré. C'est
un identifiant métier : des écrans et des liens pointent dessus. Qui veut un
autre code archive et recrée.

**On archive, on ne supprime pas.** Le service n'expose aucune suppression. Une
entrée effacée par erreur est irrécupérable ; une entrée archivée se restaure, et
retrouve exactement le statut qu'elle avait.

Ces trois règles vivent dans `FaqService`, jamais dans un contrôleur.

## Le bilingue

`LocalizedText` porte `fr` et `en` dans le même document — pas de seconde
collection, c'est précisément ce que Mongo permet.

Le back renvoie **les deux**, le front choisit. On ne filtre pas selon un
`Accept-Language` : ça obligerait à rappeler le serveur quand l'utilisateur
change de langue, alors que tout est déjà là.

Le français est obligatoire (`@NotBlank`), l'anglais peut manquer :
`forLanguage("en")` replie sur le français plutôt que d'afficher une case
blanche. Une traduction en retard ne doit pas empêcher de publier.

## Les deux frontières

| Préfixe | Qui | Voit |
|---|---|---|
| `/api/v1/faq` | utilisateur connecté | publié et non archivé |
| `/api/v1/admin/faq` | `ROLE_ADMIN` | tout |

Deux contrôleurs, deux fichiers, **jamais un `if (isAdmin)`**.

Le back-office est fermé deux fois : `/api/v1/admin/**` est la **première** règle
de la chaîne de sécurité — aucune règle plus permissive écrite plus bas ne peut
la devancer — et `AdminFaqController` porte en plus un `@PreAuthorize`. Si
quelqu'un ajoute demain une route en oubliant l'annotation, le chemin reste
fermé.

`FaqSecurityTest` vérifie les deux barrières en envoyant de vraies requêtes.

## La FAQ est-elle publique ?

```properties
ojino.support.faq.public-access=${SUPPORT_FAQ_PUBLIC:false}
```

Fermée par défaut. Une FAQ ouverte se référence sur Google et se lit avant de
créer un compte ; une FAQ fermée ne dit rien du produit à un concurrent.

C'est une décision produit, donc elle vit dans la configuration. En changer ne
demande pas de toucher une ligne de Java.

## Les index

```java
@CompoundIndex(name = "idx_faq_code", def = "{'code': 1}", unique = true)
@CompoundIndex(name = "idx_faq_listing",
        def = "{'status': 1, 'archived': 1, 'position': 1}")
```

Le premier fait porter l'unicité par **Mongo lui-même**. Le `existsByCode` du
service ne suffit pas : deux requêtes simultanées le passeraient toutes les deux.
Il sert à rendre un 409 propre, pas à garantir l'invariant.

Le second est calqué sur la requête la plus fréquente — publiées, non archivées,
dans l'ordre. Un index composé se lit de gauche à droite, comme un annuaire trié
par nom puis prénom : **on calque l'index sur la requête, jamais l'inverse.**

Les deux ne sont créés que grâce à
`spring.data.mongodb.auto-index-creation=true`. Sans cette ligne, les annotations
seraient de la documentation décorative.

## Les erreurs

`application/problem+json`, avec un champ `code` stable que les clients testent —
le message, lui, est pour les humains et peut changer.

| Situation | Statut | `code` |
|---|---|---|
| Entrée inconnue | 404 | `faq_entry_not_found` |
| Code déjà pris | 409 | `faq_code_already_exists` |
| Champ invalide | 400 | `validation_failed` + `errors` |
| Le reste | 500 | `internal_error` |

409 et non 400 sur un doublon : la requête est valide, c'est l'**état du
serveur** qui la rend impossible. La nuance décide si le client corrige sa saisie
ou change de code.

## Lancer

```bash
docker compose up -d          # depuis la racine du dépôt
./mvnw spring-boot:run
```

La base et l'utilisateur `support_service` viennent de
`docker/mongo-init/01-create-service-users.js`, rejoué uniquement quand le volume
est vide (`docker compose down -v`).

`requests.http` contient toutes les routes, le parcours qui prouve la règle
métier, et les contrôles de sécurité.

## Tests

```bash
./mvnw test        # 27 tests
```

| Classe | Ce qu'elle garantit |
|---|---|
| `FaqServiceTest` | les règles métier, sans Spring ni base |
| `LocalizedTextTest` | le repli de langue |
| `FaqSecurityTest` | 401 / 403 / 200 sur les vraies routes |
| `AdminFaqControllerTest` | le contrat HTTP, la validation, le 409 |
| `SupportServiceApplicationTests` | le câblage complet |

MongoDB est exclu du test de contexte : `mongoTemplate` ouvre sa connexion dès sa
création, et un test qui exige une base en marche passe ici et échoue partout
ailleurs.

## Reste à faire

Les demandes d'assistance (`SupportTicket`) : ce qu'un utilisateur envoie quand
la FAQ ne suffit pas. Cycle de vie ouverte → en cours → résolue → fermée, lecture
restreinte à son auteur, liste admin **paginée** — contrairement à la FAQ, il y
en aura des milliers.
