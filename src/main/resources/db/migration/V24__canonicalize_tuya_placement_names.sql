-- Apply the same presentation convention used by TuyaSpaceNameNormalizer to
-- assignments created before title-case normalization was introduced.
WITH normalized AS (
    SELECT id,
           regexp_replace(
               initcap(lower(btrim(canonical_name))),
               '\mTimax\M',
               'TIMAX',
               'g'
           ) AS canonical_name
    FROM public.device_assignments
)
UPDATE public.device_assignments a
SET canonical_name = normalized.canonical_name
FROM normalized
WHERE normalized.id = a.id
  AND a.canonical_name IS DISTINCT FROM normalized.canonical_name;

UPDATE public.devices d
SET site_id = a.site_id,
    name = a.canonical_name,
    name_customized = true,
    updated_at = now()
FROM public.device_assignments a
WHERE a.device_id = d.id
  AND a.active = true
  AND (
      d.site_id IS DISTINCT FROM a.site_id
      OR d.name IS DISTINCT FROM a.canonical_name
      OR d.name_customized = false
  );
