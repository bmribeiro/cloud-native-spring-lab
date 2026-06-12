CREATE TABLE IF NOT EXISTS users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(160) UNIQUE NOT NULL,
  role VARCHAR(40) NOT NULL DEFAULT 'CUSTOMER',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO users (name, email, role) VALUES
  ('Ana Silva', 'ana@example.com', 'CUSTOMER'),
  ('Bruno Costa', 'bruno@example.com', 'CUSTOMER'),
  ('Carla Mendes', 'carla@example.com', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
