INSERT INTO auditoriums (id, name, type, capacity, is_extra, status, created_at, updated_at)
VALUES
  ('00000000-0000-4000-8000-000000000001', 'Cinema 1', 'IMAX', 496, false, 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000002', 'Cinema 2', 'SCREENX', 180, false, 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000003', 'Cinema 3', 'STANDARD', 160, false, 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000004', 'Cinema 4', 'STANDARD', 145, false, 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000005', 'Cinema 5', 'STANDARD', 130, false, 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000006', 'Cinema 6', 'STANDARD', 112, false, 'ACTIVE', now(), now()),
  ('00000000-0000-4000-8000-000000000007', 'Cinema 7', 'GOLD', 32, false, 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;
