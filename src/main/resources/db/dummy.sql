-- Utilisateur admin --
INSERT INTO users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    phone_number,
    birth_date,
    role,
    firebase_uid,
    created_at,
    email_verified,
    deleted_at
) VALUES (
             gen_random_uuid(),
             'admin@karapi.com',
             '$2a$12$sYPv70Ti0qvvYT4jUqgH1.0Byy17FCwZvdr39CsJVOKc5FVMQcfCq', -- mdp = "password"
             'Jean',
             'Admin',
             '+33612345678',
             '1990-01-01',
             'ADMIN',
             'nGqU5Iz1qtU7VRmuvAKvVXKS6oF3',
             NOW(),
             TRUE,
             NULL
         );

-- Salle de démonstration (UUID fixe pour référencer ses options ci-dessous) --
INSERT INTO rooms (
    id,
    name,
    description,
    street,
    city,
    postal_code,
    country,
    price_per_person_per_hour,
    currency,
    max_capacity,
    is_there_wifi,
    is_there_sono_pro,
    is_there_air_conditioning,
    latitude,
    longitude,
    status,
    created_at
) VALUES (
             '11111111-1111-1111-1111-111111111111',
             'Salle Étoile',
             'Grande salle lumineuse avec terrasse',
             '12 rue de la Paix',
             'Paris',
             '75002',
             'France',
             12.50,
             'EUR',
             50,
             TRUE,
             TRUE,
             FALSE,
             48.8566,
             2.3522,
             'OPEN',
             NOW()
         );

-- Options tarifées (forfaits fixes) de la salle de démonstration ; même devise (EUR) que la salle --
INSERT INTO room_options (id, room_id, label, description, price, currency, created_at) VALUES
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111',
     'Ménage fin de soirée', 'Nettoyage complet après l''événement', 60.00, 'EUR', NOW()),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
     'DJ Set (4h)', 'Prestation d''un DJ professionnel pendant 4 heures', 300.00, 'EUR', NOW()),
    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111',
     'Agent de sécurité', 'Agent de sécurité présent sur l''événement', 25.00, 'EUR', NOW());