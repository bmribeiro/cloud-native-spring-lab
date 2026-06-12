INSERT INTO users (name, email, role) VALUES
('Ana Silva', 'ana@example.com', 'CUSTOMER'),
('Bruno Costa', 'bruno@example.com', 'CUSTOMER'),
('Carla Mendes', 'carla@example.com', 'ADMIN')
ON CONFLICT (email) DO NOTHING;