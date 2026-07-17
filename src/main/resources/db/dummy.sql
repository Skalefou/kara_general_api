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

-- Catalogue global de services (forfaits fixes) ; UUID fixes pour les référencer dans la liaison --
INSERT INTO services (id, label, description, price, currency, created_at) VALUES
    ('22222222-2222-2222-2222-222222222221',
     'Ménage fin de soirée', 'Nettoyage complet après l''événement', 60.00, 'EUR', NOW()),
    ('22222222-2222-2222-2222-222222222222',
     'DJ Set (4h)', 'Prestation d''un DJ professionnel pendant 4 heures', 300.00, 'EUR', NOW()),
    ('22222222-2222-2222-2222-222222222223',
     'Agent de sécurité', 'Agent de sécurité présent sur l''événement', 25.00, 'EUR', NOW());

-- Rattachement des services à la salle de démonstration (comportement front-client inchangé) --
INSERT INTO room_services (id, room_id, service_id, created_at) VALUES
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111',
     '22222222-2222-2222-2222-222222222221', NOW()),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111111',
     '22222222-2222-2222-2222-222222222222', NOW()),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111',
     '22222222-2222-2222-2222-222222222223', NOW());