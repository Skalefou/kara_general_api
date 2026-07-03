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