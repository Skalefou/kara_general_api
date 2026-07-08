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
    photo_object_key         VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    street      VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(100) NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS room_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id    UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    object_key VARCHAR(512) NOT NULL,
    position   INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_room_images_room_id ON room_images (room_id);