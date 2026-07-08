# Stockage d'images — Google Cloud Storage + Cloud CDN

Deux buckets, un modèle d'accès par type d'image :

| Bucket | Contenu | Visibilité | Servi via |
|--------|---------|-----------|-----------|
| `kara-public` | Images de salles | Public (`allUsers: objectViewer`) | Cloud CDN (`https://cdn.kara.xxx/<key>`) |
| `kara-private` | Photos de profil | Privé (aucun accès public) | URL signée V4, TTL 15 min |

Le compte de service **Firebase** (variable `FIREBASE_CREDENTIALS_BASE64`) est réutilisé par GCS :
même projet GCP, et sa clé privée permet la signature des URL V4 du bucket privé.

Clés d'objet :
- Salle : `rooms/{roomId}/{uuid}.{ext}`
- Profil : `profiles/{userId}/{uuid}.{ext}`

Validation d'upload (côté API) : MIME ∈ {`image/jpeg`, `image/png`, `image/webp`, `image/avif`,
`image/heic`, `image/heif`}, 5 Mo max.

## Variables d'environnement

```
FIREBASE_CREDENTIALS_BASE64=<déjà défini pour Firebase>
GCS_BUCKET_PUBLIC=kara-public
GCS_BUCKET_PRIVATE=kara-private
GCS_CDN_BASE_URL=https://cdn.kara.xxx
```

## Provisionnement des buckets

```bash
# Public (salles) — lecture libre
gcloud storage buckets create gs://kara-public \
  --location=EUR4 --uniform-bucket-level-access
gcloud storage buckets add-iam-policy-binding gs://kara-public \
  --member=allUsers --role=roles/storage.objectViewer

# Privé (profils) — aucun binding public
gcloud storage buckets create gs://kara-private \
  --location=EUR4 --uniform-bucket-level-access

# Droits du compte de service Firebase sur les deux buckets
SA=$(gcloud iam service-accounts list --format='value(email)' | grep firebase-adminsdk)
for b in kara-public kara-private; do
  gcloud storage buckets add-iam-policy-binding gs://$b \
    --member="serviceAccount:$SA" --role=roles/storage.objectAdmin
done
```

## Cloud CDN devant `kara-public`

Load balancer HTTPS externe + backend bucket avec CDN activé :

```bash
# 1. IP externe globale
gcloud compute addresses create kara-cdn-ip --global

# 2. Backend bucket avec CDN
gcloud compute backend-buckets create kara-public-backend \
  --gcs-bucket-name=kara-public --enable-cdn

# 3. URL map -> backend bucket
gcloud compute url-maps create kara-cdn-map \
  --default-backend-bucket=kara-public-backend

# 4. Certificat managé + proxy HTTPS
gcloud compute ssl-certificates create kara-cdn-cert \
  --domains=cdn.kara.xxx --global
gcloud compute target-https-proxies create kara-cdn-proxy \
  --url-map=kara-cdn-map --ssl-certificates=kara-cdn-cert

# 5. Règle de transfert 443 sur l'IP réservée
gcloud compute forwarding-rules create kara-cdn-fr --global \
  --address=kara-cdn-ip --target-https-proxy=kara-cdn-proxy --ports=443
```

Puis créer l'enregistrement DNS `A` `cdn.kara.xxx` vers l'IP réservée
(`gcloud compute addresses describe kara-cdn-ip --global --format='value(address)'`).

Les clés d'objet sont uniques (uuid) → images immuables → la politique de cache CDN par défaut convient.
Le bucket privé n'est **pas** derrière le CDN : les URL signées pointent directement sur
`storage.googleapis.com`.
