-- Device discovery needs a stable destination before an operator assigns a
-- real site. Restore and reactivate the sentinel if it was removed or renamed
-- with different casing.
INSERT INTO public.sites (name, is_active)
VALUES ('Sin sede asignada', true)
ON CONFLICT ((lower(name))) DO UPDATE
SET name = EXCLUDED.name,
    is_active = true,
    updated_at = now();
