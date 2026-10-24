-- V4 inserted demo users into every database. Remove them and their rows.
-- The local profile recreates the users when that profile is active.

DELETE FROM payment_idempotency_keys
WHERE user_id IN (
  SELECT id FROM users WHERE email IN ('admin@demo.local', 'customer@demo.local')
);

DELETE FROM order_items
WHERE order_id IN (
  SELECT id FROM orders
  WHERE user_id IN (
    SELECT id FROM users WHERE email IN ('admin@demo.local', 'customer@demo.local')
  )
);

DELETE FROM orders
WHERE user_id IN (
  SELECT id FROM users WHERE email IN ('admin@demo.local', 'customer@demo.local')
);

DELETE FROM cart_items
WHERE cart_id IN (
  SELECT id FROM carts
  WHERE user_id IN (
    SELECT id FROM users WHERE email IN ('admin@demo.local', 'customer@demo.local')
  )
);

DELETE FROM carts
WHERE user_id IN (
  SELECT id FROM users WHERE email IN ('admin@demo.local', 'customer@demo.local')
);

DELETE FROM users
WHERE email IN ('admin@demo.local', 'customer@demo.local');
