-- La ubicación referencial de las sedes de Puno pasa del centro de la ciudad
-- al centro poblado de Alto Puno. La migración actualiza bases donde V18 ya
-- fue aplicada; las coordenadas podrán editarse posteriormente si se obtiene
-- la ubicación exacta del dispositivo.
UPDATE public.sites
SET latitude = -15.811540,
    longitude = -70.032300,
    updated_at = now()
WHERE lower(btrim(COALESCE(NULLIF(btrim(district), ''), name))) = 'puno'
   OR lower(btrim(name)) LIKE '% - puno';
