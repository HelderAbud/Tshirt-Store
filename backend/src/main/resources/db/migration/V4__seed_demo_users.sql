-- Demo users for local/portfolio bootstrap (NOT for production).
-- Password for both: Demo12345!
-- BCrypt hash generated with Spring BCryptPasswordEncoder.

INSERT INTO users (email, password_hash, role) VALUES
  ('admin@demo.local', '$2a$10$XMr2G1iQi35gx7RPXBYdUe5mniXBeeQ8VLNYAaI.RbjxbAAtODwgy', 'ADMIN'),
  ('customer@demo.local', '$2a$10$XMr2G1iQi35gx7RPXBYdUe5mniXBeeQ8VLNYAaI.RbjxbAAtODwgy', 'CUSTOMER');
