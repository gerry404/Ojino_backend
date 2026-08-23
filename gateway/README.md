# gateway

Passerelle **Caddy** devant les onze services d'Ojino.

## Pourquoi Caddy

Le débit n'était pas le critère. HAProxy monte à ~52 000 req/s et Traefik plafonne
à ~18 400 : le plus lent des deux encaisse déjà 1,5 milliard de requêtes par
jour. Avec dix JVM Spring sur la même machine, le goulot d'étranglement sera
MongoDB, jamais le proxy.

Les deux critères réels :

**La RAM.** Caddy tient dans ~30 Mo, contre ~80 pour Traefik. Sur un serveur
personnel où dix services Spring prennent déjà 3 à 5 Go, ces 50 Mo pèsent plus
lourd qu'un débit théorique jamais atteint.

**Le HTTPS.** Certificat obtenu et renouvelé automatiquement, sans une ligne de
configuration ACME. Nginx aurait donné 30 % de débit en plus au prix d'un
certbot à surveiller et d'un certificat qui expire un dimanche.

## Le schéma d'URL

Une **seule origine** pour le navigateur : l'application et l'API sous le même
domaine. Le CORS disparaît donc en production.

```
ojino.cm/                      → Angular (SSR, Node)
ojino.cm/api/auth/**           → auth-service      8081
ojino.cm/api/users/**          → user-service      8082
ojino.cm/api/content/**        → content-service   8083
ojino.cm/api/learning/**       → learning-service  8084
ojino.cm/api/planning/**       → planning-service  8085
ojino.cm/api/media/**          → media-service     8086
ojino.cm/api/notifications/**  → notification      8087
ojino.cm/api/engagement/**     → engagement        8088
ojino.cm/api/assistant/**      → assistant-service 8089
ojino.cm/api/support/**        → support-service   8092
ojino.cm/ws/**                 → realtime-service  8090
```

`handle_path` **retire le préfixe** avant de transmettre :

```
/api/support/api/v1/faq   →   support-service   /api/v1/faq
```

Le préfixe est indispensable : sans lui, dix services se disputeraient
`/api/v1/admin/**`.

Et il garde la **même forme d'URL** qu'en développement direct — le code Angular
écrit toujours `${base}/api/v1/...`, seule la base change :

| | base |
|---|---|
| dev | `http://localhost:8092` |
| prod | `https://ojino.cm/api/support` |

## ai-service n'est pas exposé — volontairement

Il n'a pas de resource server : il se protège par un jeton interne partagé avec
`assistant-service`, son seul appelant. Le publier offrirait un accès direct au
moteur d'inférence, coût de calcul compris.

**Règle générale : un service sans authentification utilisateur n'a rien à faire
derrière une passerelle publique.**

## Deux détails qui comptent

**`flush_interval -1` sur `/api/assistant/**`.** L'assistant diffuse ses réponses
jeton par jeton. Sans cette ligne, Caddy accumulerait le flux et le livrerait
d'un bloc — le streaming ne servirait plus à rien.

**Délais d'une heure sur `/ws/**`.** Caddy détecte l'`Upgrade` WebSocket et
bascule seul ; il ne reste qu'à desserrer les délais, une connexion temps réel
restant ouverte longtemps.

## Lancer

La plateforme s'etale sur trois depots, clones **cote a cote** : le
`docker-compose.yml` construit les deux voisins par des chemins relatifs.

```bash
git clone https://github.com/gerry404/Ojino_backend.git
git clone https://github.com/gerry404/Ojino_fast_backend.git
git clone https://github.com/gerry404/Ojino_ia_backend.git
cd Ojino_backend && cp .env.example .env
```

Chaque dossier porte le nom de son depot — c'est ce qui rend les chemins par
defaut corrects sans rien configurer. Si ton arborescence differe, renseigne
`REALTIME_CONTEXT` et `AI_CONTEXT` dans le `.env`.

La passerelle vit dans le `docker-compose.yml` principal, sous le profil `full` :

```bash
docker compose --profile full up -d
```

Sur le serveur, une fois le domaine pointe dessus, renseigne `OJINO_DOMAIN` et
`ACME_EMAIL` dans le `.env`. Caddy obtient le certificat au premier demarrage.

Les ports **80 et 443 doivent etre ouverts** : Let's Encrypt verifie le domaine
en appelant le 80.

Pour valider le routage sans conteneuriser les services, garde-les dans l'IDE et
pointe les amonts vers l'hote :

```bash
SUPPORT_UPSTREAM=host.docker.internal:8092 docker compose --profile full up -d gateway
```

## Ce qui manque encore

Seule l'application Angular n'est pas conteneurisee : `WEB_UPSTREAM` vise
`host.docker.internal:4200`, c'est-a-dire `ng serve`. Une fois construite et
servie par Node, ce sera `ojino-web:4000`.

`realtime-service` et `ai-service` vivent dans des depots voisins, clones a cote
de celui-ci. Leurs contextes de build sont des variables — `REALTIME_CONTEXT` et
`AI_CONTEXT` — parce que les dossiers ne portent pas le meme nom en local et sur
le serveur.
