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
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
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