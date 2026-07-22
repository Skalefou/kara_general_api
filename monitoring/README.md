# Monitoring self-hosted — Kara pré-prod

Stack d'observabilité **auto-hébergée**, accessible **uniquement via Tailscale** (VPN
WireGuard). Rien n'est exposé sur Internet public.

- **Prometheus** — collecte des métriques (API, PostgreSQL, hôte) + règles d'alerte.
- **Grafana** — dashboards + exploration des logs. Seul service « visible » (loopback → Tailscale).
- **Loki + Promtail** — agrégation des logs de tous les conteneurs Docker.
- **postgres_exporter / node_exporter** — métriques PostgreSQL et hôte (CPU/RAM/disque).
- **Alertmanager** — routage des alertes (webhook/Slack/email à brancher).

Tous ces services tournent sur le réseau Docker interne `kara_net` (déclaré par
`compose.preprod.yaml`). **Seul Grafana** ouvre un port, et seulement sur `127.0.0.1`.

---

## 1. Démarrage

Le réseau `kara_net` est créé par la stack pré-prod : elle doit démarrer **en premier**.

```bash
# 1) Stack applicative (crée le réseau kara_net)
docker compose --env-file .env.preprod -f compose.preprod.yaml up -d

# 2) Stack monitoring (rejoint kara_net en `external`)
docker compose --env-file .env.preprod -f compose.monitoring.yaml -p kara-monitoring up -d
```

> **Pourquoi deux commandes ?** `compose.monitoring.yaml` référence `kara_net` comme
> réseau `external: true` (« il existe déjà »). Il faut donc que la stack pré-prod l'ait
> créé avant. Le projet monitoring est isolé sous `-p kara-monitoring` pour éviter toute
> collision de conteneurs/volumes avec la stack applicative.
>
> La commande « multi-fichiers » en un seul appel
> (`-f compose.preprod.yaml -f compose.monitoring.yaml up`) **ne fonctionne pas au premier
> lancement** : `external: true` échoue tant que le réseau n'existe pas. Utilisez toujours
> les deux commandes ci-dessus.

Arrêt de la seule stack monitoring :

```bash
docker compose -f compose.monitoring.yaml -p kara-monitoring down
```

Prérequis : `.env.preprod` doit contenir `GF_SECURITY_ADMIN_PASSWORD` (mot de passe admin
Grafana) et les identifiants Postgres (`POSTGRES_USER/PASSWORD/DB`). Voir
`.env.preprod.example`.

---

## 2. Accès distant via Tailscale (option retenue)

Grafana écoute **uniquement** sur `127.0.0.1:3000` — inaccessible depuis le réseau, y
compris `kara_net`. On l'expose au tailnet via `tailscale serve` (HTTPS + auth du tailnet).

```bash
# a) Installer Tailscale sur le serveur hôte
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up          # ouvre un lien d'authentification vers votre IdP (MFA)

# b) Exposer Grafana (127.0.0.1:3000) en HTTPS sur le tailnet, en arrière-plan
sudo tailscale serve --bg 3000
```

Grafana devient alors joignable sur :

```
https://<nom-hote>.<tailnet>.ts.net
```

… **uniquement** par les membres de votre tailnet (authentifiés via l'IdP Tailscale, MFA
incluse). Aucun port n'est ouvert sur l'IP publique du serveur.

Vérifier l'état : `sudo tailscale serve status`. Retirer l'exposition :
`sudo tailscale serve --https=443 off`.

### Alternative : sidecar Tailscale (conteneur)

Si vous ne voulez pas installer Tailscale sur l'hôte, un conteneur sidecar
(`tailscale/tailscale`) avec une **auth key** peut rejoindre le tailnet et faire
`tailscale serve` vers le conteneur Grafana. Plus lourd à configurer (clé d'auth, état
persistant, `TS_SERVE_CONFIG`) ; l'installation sur l'hôte reste l'option recommandée ici.

---

## 3. Pare-feu (rappel impératif)

Le modèle de sécurité repose sur : **aucun port monitoring ouvert sur Internet**. Seuls
les ports applicatifs le sont.

```bash
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # ACME HTTP-01 + redirection HTTPS (Caddy)
sudo ufw allow 443/tcp    # HTTPS applicatif (Caddy)
sudo ufw enable
```

**Ne JAMAIS ouvrir** : `3000` (Grafana), `9090` (Prometheus), `3100` (Loki), `9093`
(Alertmanager), `9100` (node_exporter), `9187` (postgres_exporter). Ils restent internes à
`kara_net` (ou en loopback pour Grafana) et transitent par Tailscale. Tailscale (WireGuard)
n'a pas besoin de règle entrante UDP explicite grâce au NAT traversal ; au besoin, ouvrir
`41641/udp`.

---

## 4. Dashboards Grafana

Un dashboard fonctionnel est provisionné automatiquement : **« Kara API — Overview »**
(dossier *Kara*) — up de l'API, débit HTTP, latence p95, pool HikariCP, heap JVM, logs Loki.

Dashboards communautaires recommandés (à **importer** depuis l'UI Grafana →
*Dashboards → New → Import*, coller l'ID, choisir la datasource *Prometheus*) :

| Dashboard                       | ID Grafana.com |
|---------------------------------|----------------|
| Spring Boot / Micrometer        | **19004**      |
| PostgreSQL (postgres_exporter)  | **9628**       |
| Node Exporter Full              | **1860**       |

Pour rendre un dashboard importé permanent, exportez son JSON et déposez-le dans
`monitoring/grafana/dashboards/` : le provider le rechargera automatiquement.

---

## 5. Requêtes lentes PostgreSQL (`pg_stat_statements`)

L'extension est préchargée (`shared_preload_libraries`, voir `command:` du service
`postgres`) et créée par `db/init.sql`. Les requêtes dépassant `log_min_duration_statement`
(500 ms) apparaissent aussi dans les logs (donc dans Loki, via Promtail).

Top 10 des requêtes les plus coûteuses :

```sql
SELECT query, calls, total_exec_time, mean_exec_time, rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;
```

Réinitialiser les statistiques : `SELECT pg_stat_statements_reset();`

---

## 6. Alertes

Les règles vivent dans `monitoring/prometheus/alert-rules.yml` : pool HikariCP en attente,
connexions PostgreSQL > 80% du max, latence HTTP p95 élevée, taux 5xx > 5%, disque hôte
< 15%, mémoire hôte > 90%. Le routage (`alertmanager/alertmanager.yml`) utilise un receiver
`default` (placeholder webhook) — **brancher Slack ou email** avant mise en production
(blocs `TODO` commentés).

> **Redis** : l'alerte mémoire Redis nécessite un `redis_exporter` (non inclus). Voir le
> bloc commenté en fin de `alert-rules.yml` pour l'activer.
