-- ============================================================================
-- Schéma de production — source de vérité DDL (prod).
-- Miroir fidèle des classes @Entity de infrastructure/adapter/output/persistence
-- (types, nullabilité, defaults, contraintes uniques et index déclarés côté code).
-- Les clés étrangères ne sont pas exprimées par les entités (identifiants UUID nus)
-- mais sont ajoutées ici pour l'intégrité référentielle en production.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Observabilité : pg_stat_statements agrège les statistiques d'exécution des requêtes
-- (temps total, appels, lignes) pour détecter les requêtes lentes. La bibliothèque doit
-- être préchargée via shared_preload_libraries (cf. `command:` du service postgres).
-- Idempotent : ne recrée pas l'extension si elle existe déjà.
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- ---------------------------------------------------------------------------
-- Comptes utilisateurs (tous rôles : GUEST/CLIENT/SERVER/ADMIN).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                    VARCHAR(255) NOT NULL UNIQUE,
    password_hash            VARCHAR(255) NOT NULL,
    first_name               VARCHAR(100) NOT NULL,
    last_name                VARCHAR(100) NOT NULL,
    phone_number             VARCHAR(20)  NOT NULL,
    birth_date               DATE         NOT NULL,
    role                     VARCHAR(50)  NOT NULL,
    firebase_uid             VARCHAR(128) NOT NULL UNIQUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    email_verified           BOOLEAN      NOT NULL,
    deleted_at               TIMESTAMPTZ,
    deactivated_at           TIMESTAMPTZ,
    must_change_password     BOOLEAN      NOT NULL DEFAULT FALSE,
    temp_password_expires_at TIMESTAMPTZ,
    photo_object_key         VARCHAR(512),
    -- Identifiant client Stripe (créé paresseusement au 1er paiement). Jamais logué.
    stripe_customer_id       VARCHAR(255),
    -- Token d'appareil FCM (notifications push). Nullable tant qu'aucun appareil n'est enregistré.
    fcm_token                VARCHAR(512)
);

-- ---------------------------------------------------------------------------
-- Salles louables à l'heure.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL DEFAULT '',
    street      VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(100) NOT NULL,
    price_per_person_per_hour NUMERIC(10,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(10)  NOT NULL DEFAULT 'EUR',
    max_capacity              INT     NOT NULL DEFAULT 0,
    is_there_wifi             BOOLEAN NOT NULL DEFAULT FALSE,
    is_there_sono_pro         BOOLEAN NOT NULL DEFAULT FALSE,
    is_there_air_conditioning BOOLEAN NOT NULL DEFAULT FALSE,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    status      VARCHAR(50)  NOT NULL,
    opens_at    TIME,
    closes_at   TIME,
    time_zone   VARCHAR(64)  NOT NULL DEFAULT 'Europe/Paris',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Filtrage viewport (bbox) : sert le BETWEEN sur latitude/longitude (SELECT et COUNT).
CREATE INDEX IF NOT EXISTS idx_rooms_lat_lng ON rooms (latitude, longitude);

CREATE TABLE IF NOT EXISTS room_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    object_key VARCHAR(512) NOT NULL,
    position   INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Catalogue global des services réutilisables : forfaits fixes (indépendants du nombre de personnes
-- et de la durée). Un service n'est plus rattaché en dur à une seule salle.
CREATE TABLE IF NOT EXISTS services (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label       VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Liaison salle↔service : rattache un service du catalogue global à une salle. Le prix/label/description
-- vivent sur `services`, jamais sur la liaison.
CREATE TABLE IF NOT EXISTS room_services (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_services_room_service UNIQUE (room_id, service_id)
);

CREATE INDEX IF NOT EXISTS idx_room_services_room_id ON room_services (room_id);
CREATE INDEX IF NOT EXISTS idx_room_services_service_id ON room_services (service_id);

-- Catalogue générique des produits consommables : liste de référence indépendante de toute salle,
-- utilisée pour la gestion de stock et la consommation pendant une réservation. Prix unitaire fixe.
CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(10)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Stock par salle : rattache un produit du catalogue générique à une salle avec une quantité disponible.
-- Un produit absent de cette table (ou en quantité 0) ne peut pas être vendu pour la salle concernée.
CREATE TABLE IF NOT EXISTS room_products (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity   INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_products_room_product UNIQUE (room_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_room_products_room_id ON room_products (room_id);
CREATE INDEX IF NOT EXISTS idx_room_products_product_id ON room_products (product_id);

-- ============================================================================
-- Chat (messagerie temps réel) — MVP texte
-- ============================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Conversation rattachée à une réservation (nullable) : sert au verrou « chat fermé 30 min après ».
    booking_id UUID REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_conversations_booking_id ON conversations (booking_id) WHERE booking_id IS NOT NULL;

-- Participation d'un utilisateur à une conversation + état de lecture (last_read_at pilote les non-lus).
CREATE TABLE IF NOT EXISTS conversation_participants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_conversation_participants_conversation_user UNIQUE (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_participants_user_id ON conversation_participants (user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation_id ON conversation_participants (conversation_id);

CREATE TABLE IF NOT EXISTS messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,
    text            TEXT,
    reply_to_id     UUID REFERENCES messages(id) ON DELETE SET NULL,
    is_forwarded    BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_sent_at ON messages (conversation_id, sent_at);

-- Réaction (emoji) posée par un utilisateur sur un message. L'unicité (message, utilisateur, emoji)
-- garantit l'idempotence de la bascule.
CREATE TABLE IF NOT EXISTS message_reactions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji      VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_message_reactions_message_user_emoji UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX IF NOT EXISTS idx_message_reactions_message_id ON message_reactions (message_id);

-- ============================================================================
-- Réservations & règlement
-- ============================================================================

-- Réservations : un créneau réservé sur une salle par un client. Le prix total est figé à la création.
-- Le chevauchement de créneaux est contrôlé applicativement (statuts PENDING/CONFIRMED).
CREATE TABLE IF NOT EXISTS bookings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id          UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_at         TIMESTAMPTZ  NOT NULL,
    end_at           TIMESTAMPTZ  NOT NULL,
    number_of_people INT          NOT NULL,
    total_price      NUMERIC(10,2) NOT NULL,
    currency         VARCHAR(10)  NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    -- Mode de règlement : PAY_ALL (fenêtre 15 min) ou SHARED_POT (délai gouverné par la cagnotte).
    payment_mode     VARCHAR(20)  NOT NULL DEFAULT 'PAY_ALL',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bookings_room_id ON bookings (room_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings (user_id);

-- Options retenues au moment de la réservation (fige les identifiants d'options). Modelée sur room_services.
CREATE TABLE IF NOT EXISTS booking_options (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    option_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_booking_options_booking_option UNIQUE (booking_id, option_id)
);

CREATE INDEX IF NOT EXISTS idx_booking_options_booking_id ON booking_options (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_options_option_id ON booking_options (option_id);

CREATE TABLE IF NOT EXISTS booking_extensions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id         UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    additional_minutes INT           NOT NULL,
    previous_end_at    TIMESTAMPTZ   NOT NULL,
    new_end_at         TIMESTAMPTZ   NOT NULL,
    price              NUMERIC(10,2) NOT NULL,
    currency           VARCHAR(10)   NOT NULL,
    status             VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    payment_mode       VARCHAR(20)   NOT NULL DEFAULT 'PAY_ALL',
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    expires_at         TIMESTAMPTZ   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_booking_extensions_booking_id ON booking_extensions (booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_extensions_user_id ON booking_extensions (user_id);
CREATE INDEX IF NOT EXISTS idx_booking_extensions_status_expires ON booking_extensions (status, expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_booking_extensions_pending_booking
    ON booking_extensions (booking_id) WHERE status = 'PENDING';

-- Commandes de produits passées pendant une réservation active. Le prix unitaire est figé au tarif du produit
-- au moment de la commande ; total_price = unit_price × quantity. Le débit/crédit du moyen de paiement est
-- géré par la brique paiement (hors de cette table). status : PLACED (extensible).
CREATE TABLE IF NOT EXISTS orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity    INT           NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(10)   NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    status      VARCHAR(30)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_booking_id ON orders (booking_id);

-- Rappels de fin de réservation envoyés (idempotence des notifications push « fin imminente »).
-- Un rappel de type `kind` (TEN_MINUTES / TWO_MINUTES) n'est envoyé qu'une fois par réservation :
-- l'unicité (booking_id, kind) garantit qu'un tick de planificateur ne renotifie pas.
CREATE TABLE IF NOT EXISTS booking_end_reminders (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    kind       VARCHAR(20)  NOT NULL,
    sent_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_booking_end_reminders_booking_kind UNIQUE (booking_id, kind)
);

CREATE INDEX IF NOT EXISTS idx_booking_end_reminders_booking_id ON booking_end_reminders (booking_id);

-- Agenda serveurs : affectation d'un serveur à une salle sur un créneau [start_at, end_at). Édité par
-- l'ADMIN depuis le back-office. Deux créneaux d'un même serveur ne doivent pas se chevaucher (contrôle
-- applicatif). ON DELETE CASCADE : un serveur ou une salle supprimé purge ses créneaux.
CREATE TABLE IF NOT EXISTS server_shifts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    start_at   TIMESTAMPTZ NOT NULL,
    end_at     TIMESTAMPTZ NOT NULL,
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_server_shifts_server_id ON server_shifts (server_id);
CREATE INDEX IF NOT EXISTS idx_server_shifts_room_id ON server_shifts (room_id);
CREATE INDEX IF NOT EXISTS idx_server_shifts_start_at ON server_shifts (start_at);

-- Paiements « payer tout » (Stripe). Le webhook Stripe fait foi : payment_intent.succeeded → PAID + booking CONFIRMED.
CREATE TABLE IF NOT EXISTS payments (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id               UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    extension_id             UUID REFERENCES booking_extensions(id) ON DELETE CASCADE,
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount                   NUMERIC(10,2) NOT NULL,
    currency                 VARCHAR(10)  NOT NULL,
    status                   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255) NOT NULL UNIQUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments (user_id);

-- Cagnottes (règlement Stripe en autorisation à capture manuelle). Le montant cible est figé (= prix total
-- de la réservation). Rien n'est prélevé tant que toutes les parts ne sont pas autorisées ; à complétude,
-- toutes les autorisations sont capturées et la réservation passe CONFIRMED. Le délai (< 7 jours, validité
-- des autorisations Stripe) déclenche sinon l'annulation de toutes les autorisations (zéro prélèvement).
CREATE TABLE IF NOT EXISTS pools (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id        UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    extension_id      UUID REFERENCES booking_extensions(id) ON DELETE CASCADE,
    target_amount     NUMERIC(10,2) NOT NULL,
    currency          VARCHAR(10)  NOT NULL,
    status            VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    deadline          TIMESTAMPTZ  NOT NULL,
    global_link_token VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pools_booking_id ON pools (booking_id) WHERE extension_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_pools_extension_id ON pools (extension_id) WHERE extension_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_pools_global_link_token ON pools (global_link_token);
CREATE INDEX IF NOT EXISTS idx_pools_status_deadline ON pools (status, deadline);

-- Parts d'une cagnotte : le montant de chaque part est réglé par un PaymentIntent Stripe à capture manuelle.
-- La somme des parts égale le montant cible de la cagnotte (invariant maintenu applicativement).
CREATE TABLE IF NOT EXISTS pool_shares (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pool_id                  UUID NOT NULL REFERENCES pools(id) ON DELETE CASCADE,
    participant_name         VARCHAR(255) NOT NULL,
    email                    VARCHAR(255),
    amount                   NUMERIC(10,2) NOT NULL,
    status                   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255),
    unique_link_token        VARCHAR(255),
    payer_user_id            UUID REFERENCES users(id) ON DELETE SET NULL,
    is_creator_share         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pool_shares_pool_id ON pool_shares (pool_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pool_shares_unique_link_token ON pool_shares (unique_link_token);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pool_shares_stripe_payment_intent_id ON pool_shares (stripe_payment_intent_id);

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
