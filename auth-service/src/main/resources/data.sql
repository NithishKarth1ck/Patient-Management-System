-- Ensure the 'users' table exists
-- CREATE TABLE IF NOT EXISTS users (
--     id UUID PRIMARY KEY,
--     name VARCHAR(100) NOT NULL,
--     email VARCHAR(255) UNIQUE NOT NULL,
--     password VARCHAR(255) NOT NULL,
--     role VARCHAR(50) NOT NULL
-- );

-- Insert the user if no existing user with the same id or email exists
INSERT INTO users (id, name, email, password, role)
SELECT 
    '223e4567-e89b-12d3-a456-426614174006'::uuid,
    'Test User',
    'testuser@test.com',
    '$2a$10$bVJ4OHpWL0LmGxPvNOKnyuLaQNOaqMFQ56UuYx6R/.z14PvH.kL9y',
    'ADMIN'
WHERE NOT EXISTS (
    SELECT 1
    FROM "users"
    WHERE id = '223e4567-e89b-12d3-a456-426614174006'::uuid
       OR email = 'testuser@test.com'
);