# 🦀 CLAUDE.md — `kara_image_worker` (Worker Rust)

> Il fait **autorité** pour ce dépôt (et seulement lui).
> Le coordinateur (`PA/CLAUDE.md`) orchestre ; ici on exécute le worker.

---

## 🎯 Rôle du service

`kara_image_worker` est un **service de type Worker**, **totalement isolé** du
Backend (`kara_general_api`). Il :

1. **consomme** des demandes sur la queue `image-jobs` (Backend → Worker) ;
2. **télécharge** l'image originale depuis le bucket **privé** ;
3. **valide** le fichier (magic bytes, format, dimensions/poids) ;
4. **redimensionne** l'image en plusieurs variantes (thumbnail / detail / full) ;
5. **upload** les variantes dans le bucket **public** ;
6. **publie** un résultat — **succès OU échec** — sur la queue `image-results`
   (Worker → Backend).

C'est un worker **multi-tâches par conception** : d'autres tâches (ex. envoi
d'email) pourront s'ajouter plus tard, mais la tâche métier de référence est le
**traitement + validation d'image**.

---

## 🚫 Règles d'or (non négociables — exigées par le cahier des charges)

1. **Aucun accès à la base de données du Backend.** Jamais. Le Worker ne connaît
   ni la DB, ni les modèles du Backend. Il reçoit **tout** ce dont il a besoin
   dans le message d'entrée.
2. **Aucun appel synchrone au Backend.** La seule communication est **par
   queue** : on consomme `image-jobs`, on publie sur `image-results`. Pas de
   HTTP vers l'API, pas de partage de code.
3. **Toujours répondre**, même en cas d'échec. Chaque job consommé produit un
   message sur `image-results` (`status: ok` ou `status: failed`).
4. **Le Worker possède sa politique de retry.** C'est lui — pas le Backend — qui
   décide quoi ré-essayer, combien de fois, avec quel backoff (voir §Retry).
5. **Idempotence obligatoire.** Livraison *at-least-once* : un même job peut
   arriver deux fois. Les clés de sortie sont **déterministes** → un rejeu
   écrase les mêmes objets, sans doublon.
6. **Le Backend reste seul responsable de la DB.** Le Worker renvoie des **clés
   d'objets** ; c'est le Backend qui persiste. Le Worker n'écrit jamais en base.
7. **Le contrat des messages est figé côté API.** Toute divergence est un bug.
   Voir §Contrat.

---

## 📨 Contrat des messages (source de vérité = `kara_general_api`)

Deux queues. Champs en `camelCase`. `schemaVersion` des deux côtés.
**Jamais d'octets d'image dans les messages** — uniquement des clés d'objets.

### Demande — queue `image-jobs` (Backend → Worker)

```json
{
  "schemaVersion": 1,
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "roomId": "b3f1c2a4-1111-2222-3333-444455556666",
  "imageId": "c4a2d0e8-aaaa-bbbb-cccc-ddddeeeeffff",
  "source": {
    "bucket": "karapi-...-private",
    "key": "rooms/{roomId}/originals/{imageId}.jpg",
    "contentType": "image/jpeg"
  },
  "target": {
    "bucket": "karapi-...-public",
    "keyPrefix": "rooms/{roomId}/{imageId}"
  },
  "variants": [
    { "name": "thumbnail", "width": 320,  "height": 320,  "fit": "cover",   "format": "webp" },
    { "name": "detail",    "width": 1024, "height": 768,  "fit": "contain", "format": "webp" },
    { "name": "full",      "width": 2048, "height": 2048, "fit": "inside",  "format": "webp" }
  ],
  "replyTo": "image-results",
  "enqueuedAt": "2026-07-23T12:00:00Z"
}
```

- `jobId` : corrélation unique, **repris tel quel** dans la réponse.
- `source.key` : original à télécharger (bucket privé).
- `target.keyPrefix` : le Worker écrit chaque variante à `{keyPrefix}/{name}.{format}`
  → **clés déterministes** (rejeu = écrasement).
- `fit` : `cover` (crop remplissant), `contain`/`inside` (homothétie sans dépassement).

### Réponse — queue `image-results` (Worker → Backend)

Succès :
```json
{
  "schemaVersion": 1,
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ok",
  "variants": [
    { "name": "thumbnail", "bucket": "karapi-...-public", "key": "rooms/.../thumbnail.webp", "width": 320, "height": 320, "sizeBytes": 18234, "contentType": "image/webp" }
  ],
  "processedAt": "2026-07-23T12:00:03Z"
}
```

Échec :
```json
{
  "schemaVersion": 1,
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "failed",
  "error": { "code": "DOWNLOAD_FAILED", "message": "source object not found" },
  "processedAt": "2026-07-23T12:00:01Z"
}
```

**Invariants** :
- `variants` présent **ssi** `status = "ok"`.
- `error` présent **ssi** `status = "failed"`.
- Codes d'erreur (énum figée, `SCREAMING_SNAKE_CASE`) :
  `DOWNLOAD_FAILED`, `UNSUPPORTED_FORMAT`, `DECODE_FAILED`, `RESIZE_FAILED`,
  `UPLOAD_FAILED`, `TIMEOUT`, `INTERNAL`.
- Bump `schemaVersion` pour tout changement cassant, coordonné avec l'API.

> ⚠️ Les structs `serde` de `src/messaging/contract.rs` sont la **copie exacte**
> de ce contrat. Si l'API fait évoluer le contrat, ce fichier se met à jour ici
> **en miroir** — jamais l'inverse (l'API est la source de vérité).

---

## 🗄️ Accès au stockage (GCS via interop S3 + clés HMAC)

Le stockage réel est **Google Cloud Storage**, mais on l'utilise via son **API
compatible S3**. Le Worker reste donc un **client S3 standard** :

- SDK : `aws-sdk-s3`, **endpoint** `https://storage.googleapis.com`, **path-style**.
- Auth : **clés HMAC GCS** (`accessKey` / `secret`) mappées sur les credentials
  AWS. Le Worker ne détient **jamais** le service account GCP complet.
- Lecture : bucket **privé** (`source`). Écriture : bucket **public** (`target`).
- Le Worker n'a **que** ces deux accès objets — rien d'autre de l'infra.

---

## 🧱 Stack & dépendances

| Besoin | Crate |
|---|---|
| Runtime async | `tokio` |
| AMQP / RabbitMQ | `lapin` |
| Décodage / resize | `image` + `fast_image_resize` |
| Encodage WebP | `webp` |
| Stockage S3 (GCS interop) | `aws-sdk-s3` |
| (Dé)sérialisation | `serde`, `serde_json` |
| Config / env | `config`, `dotenvy` |
| Observabilité | `tracing`, `tracing-subscriber`, `metrics`, `metrics-exporter-prometheus` |
| Erreurs | `thiserror`, `anyhow` |
| Retry / backoff | `backoff` |
| Tests d'intégration | `testcontainers` (RabbitMQ) |

---

## 🗂️ Structure du dépôt

```
kara_image_worker/
├── Cargo.toml
├── .env.example
├── Dockerfile
├── CLAUDE.md
└── src/
    ├── main.rs                # bootstrap: config, tracing, connexion, boucle consumer
    ├── config.rs              # chargement env (validé au démarrage)
    ├── messaging/
    │   ├── contract.rs        # structs serde = miroir EXACT du contrat API
    │   ├── consumer.rs        # consume image-jobs, ack manuel, DLX
    │   └── publisher.rs       # publish image-results (ok|failed)
    ├── storage/s3.rs          # GET original (privé) / PUT variantes (public)
    ├── processing/
    │   ├── validate.rs        # magic bytes, format autorisé, dims/poids max
    │   └── resize.rs          # decode → resize → encode WebP par variante
    ├── retry.rs               # backoff + classification transitoire/permanent
    ├── error.rs               # WorkerError → code du contrat
    └── metrics.rs             # compteurs/histogrammes Prometheus
```

---

## 🔁 Pipeline de traitement (par message)

1. **Désérialiser** le job. Échec de parsing → **DLQ** (poison message), pas de traitement.
2. **Valider** : magic bytes, format ∈ {jpeg, png, webp}, dimensions & poids max
   → invalide = **échec permanent** (`UNSUPPORTED_FORMAT` / `DECODE_FAILED`).
3. **Télécharger** l'original (bucket privé).
4. **Resize + encode** chaque variante sur un **pool bloquant**
   (`tokio::task::spawn_blocking` ou `rayon`) — le resize est **CPU-bound** et ne
   doit **jamais** bloquer le réacteur Tokio. Clé = `{target.keyPrefix}/{name}.webp`.
5. **Upload** chaque variante (bucket public), écrasement idempotent.
6. **Publier** le résultat sur `image-results`.
7. **Ack** le message **seulement après** publication du résultat.
   (Crash avant ack → redelivery → retraité, idempotent.)

---

## 🛡️ Politique de retry (par criticité)

| Classe | Exemples | Politique |
|---|---|---|
| **Transitoire** | timeout/5xx/throttling stockage, réseau | retry **in-process**, backoff exponentiel + jitter, **max 5**, puis `failed` + ack |
| **Permanent** | décodage impossible, format non supporté, validation | **aucun retry** → `failed` immédiat |
| **Poison** | message non désérialisable | **DLQ**, pas de traitement |

Principes : idempotence (clés déterministes), **réponse systématique** (même en
échec), **ack après réponse**. Le Worker ne ré-enqueue **jamais** vers le Backend
et ne l'appelle **jamais** en synchrone.

---

## ⚙️ Configuration (variables d'environnement)

Voir `.env.example`. Principales :

```
# RabbitMQ
RABBITMQ_URL=amqp://user:pass@localhost:5672/%2f
QUEUE_JOBS=image-jobs
QUEUE_RESULTS=image-results
PREFETCH_COUNT=8
MAX_CONCURRENCY=8

# Stockage (GCS interop S3)
S3_ENDPOINT=https://storage.googleapis.com
S3_REGION=auto
S3_ACCESS_KEY=...        # clé HMAC GCS
S3_SECRET_KEY=...        # secret HMAC GCS

# Limites de validation
MAX_IMAGE_BYTES=5242880  # 5 Mo (aligné sur le plafond upload de l'API)
MAX_DIMENSION=8000

# Retry
RETRY_MAX_ATTEMPTS=5
RETRY_BASE_MS=200

# Observabilité
METRICS_PORT=9100
RUST_LOG=info
```

La config est **validée au démarrage** : toute variable manquante/invalide fait
échouer le boot (fail-fast), pas au premier message.

---

## 🧪 Commandes

```bash
cargo build                       # build debug
cargo build --release             # build optimisé (à utiliser pour mesurer/perf)
cargo run                         # lance le worker (lit .env)
cargo test                        # tests unitaires + intégration
cargo clippy -- -D warnings       # lint strict (zéro warning toléré)
cargo fmt                         # formatage
docker build -t kara-image-worker .
```

**Avant de considérer une tâche terminée** : `cargo fmt`, `cargo clippy -D warnings`
et `cargo test` passent tous.

---

## 🎨 Conventions de code

- **Édition Rust** : 2021 (ou supérieure), toolchain stable.
- **Erreurs** : `thiserror` pour les erreurs typées du domaine (→ code contrat),
  `anyhow` uniquement aux frontières (main/bootstrap).
- **Pas de `unwrap()`/`expect()`** dans le chemin de traitement d'un message —
  toute erreur devient un `status: failed` avec le bon code, jamais un panic qui
  tue le consumer.
- **Logs** via `tracing`, structurés, avec `jobId` en champ de corrélation.
  Messages de log en **anglais**.
- **CPU-bound sur pool bloquant** : jamais de decode/resize/encode directement
  dans une tâche async du réacteur.
- **Frontières nettes** : `messaging` (I/O queue) / `storage` (I/O objets) /
  `processing` (CPU pur) / `retry` (politique). Le domaine `processing` ne
  connaît ni RabbitMQ ni S3.

---

## 🔗 Cohérence inter-dépôts

- **Contrat** : `src/messaging/contract.rs` = miroir exact du contrat défini côté
  `kara_general_api`. Les noms de queues (`image-jobs`, `image-results`), les
  champs, les codes d'erreur doivent être **identiques** des deux côtés.
- **Vocabulaire métier partagé** : `room` (salle), `image`, `variant`, `job`.
- **Sens de propagation** : le contrat part de l'API. Ici on **suit**, on ne
  décide pas du contrat. Un besoin d'évolution du contrat remonte à l'API
  d'abord.

---

## 🧾 Protocole de commit

- **Ne jamais committer ni pusher toi-même.** Tu **proposes** un message de
  commit propre au diff de ce dépôt ; l'humain décide et exécute.
- Format **Conventional Commits**, en anglais, scope pertinent
  (`worker`, `messaging`, `storage`, `processing`, `retry`, `config`).
- Un seul message par étape, propre à `kara_image_worker`.

---

## ✅ Préflight avant toute tâche

- [ ] Je ne touche jamais à la DB du Backend ni à ses modèles.
- [ ] Je ne fais aucun appel synchrone au Backend.
- [ ] Chaque job consommé produit une réponse sur `image-results` (ok **ou** failed).
- [ ] Le traitement est idempotent (clés déterministes).
- [ ] Les structs de contrat restent le miroir exact de l'API.
- [ ] `cargo fmt` + `clippy -D warnings` + `cargo test` passent.
- [ ] Je propose un message de commit, sans l'exécuter.

---

**En cas de doute : respecte le contrat, reste isolé du Backend, réponds toujours,
laisse l'humain committer.**
