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
    email_verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at               TIMESTAMPTZ,
    deactivated_at           TIMESTAMPTZ,
    must_change_password     BOOLEAN      NOT NULL DEFAULT FALSE,
    temp_password_expires_at TIMESTAMPTZ,
    photo_object_key         VARCHAR(512),
    -- Identifiant client Stripe (créé paresseusement au 1er paiement). Jamais logué.
    stripe_customer_id       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    street      VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(100) NOT NULL,
    price_per_person_per_hour NUMERIC(10,2) NOT NULL,
    currency    VARCHAR(10)  NOT NULL,
    max_capacity              INT NOT NULL,
    is_there_wifi             BOOLEAN NOT NULL,
    is_there_sono_pro         BOOLEAN NOT NULL,
    is_there_air_conditioning BOOLEAN NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    status      VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Filtrage viewport (bbox) : sert le BETWEEN sur latitude/longitude (SELECT et COUNT).
CREATE INDEX IF NOT EXISTS idx_rooms_lat_lng ON rooms (latitude, longitude);

CREATE TABLE IF NOT EXISTS room_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    object_key VARCHAR(512) NOT NULL,
    position   INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_room_images_room_id ON room_images (room_id);

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
    type            VARCHAR(20) NOT NULL DEFAULT 'text',
    text            TEXT,
    reply_to_id     UUID REFERENCES messages(id) ON DELETE SET NULL,
    is_forwarded    BOOLEAN NOT NULL DEFAULT FALSE,
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
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
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount                   NUMERIC(10,2) NOT NULL,
    currency                 VARCHAR(10)  NOT NULL,
    status                   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255) NOT NULL UNIQUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments (user_id);
