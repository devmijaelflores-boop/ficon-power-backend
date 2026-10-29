-- Los espacios operativos suelen nombrarse en Tuya Spatial como
-- "Organizacion - Distrito". Conservamos el nombre de sede como identificador
-- interno y completamos solo los distritos todavía vacíos.
UPDATE public.sites
SET district = left(
        btrim(regexp_replace(name, '^.*[[:space:]]+-[[:space:]]+', '')),
        100
    ),
    updated_at = now()
WHERE (district IS NULL OR btrim(district) = '')
  AND name ~ '[[:space:]]+-[[:space:]]+[^[:space:]]';

COMMENT ON COLUMN public.sites.district IS
    'Distrito explícito o inferido del último segmento de un nombre tipo Organizacion - Distrito.';
