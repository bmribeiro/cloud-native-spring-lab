INSERT INTO orders (id, user_id, total, status) VALUES
    (1, 1, 49.90, 'PAID'),
    (2, 1, 19.50, 'PENDING'),
    (3, 2, 199.99, 'SHIPPED')
ON CONFLICT (id) DO NOTHING;

SELECT setval('orders_id_seq', COALESCE((SELECT MAX(id) FROM orders), 1), true);
