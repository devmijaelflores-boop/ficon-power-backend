-- Coordenadas referenciales del centro urbano. Los dispositivos asociados a
-- estas sedes heredan la ubicación hasta contar con coordenadas más precisas.
INSERT INTO public.sites (
    name, district, province, department, latitude, longitude
)
SELECT 'Putina', 'Putina', 'San Antonio de Putina', 'Puno', -14.914100, -69.868480
WHERE NOT EXISTS (
    SELECT 1 FROM public.sites WHERE lower(btrim(name)) = 'putina'
);

INSERT INTO public.sites (
    name, district, province, department, latitude, longitude
)
SELECT 'Lampa', 'Lampa', 'Lampa', 'Puno', -15.364430, -70.366620
WHERE NOT EXISTS (
    SELECT 1 FROM public.sites WHERE lower(btrim(name)) = 'lampa'
);

UPDATE public.sites
SET district = 'Puno',
    province = COALESCE(NULLIF(btrim(province), ''), 'Puno'),
    department = 'Puno',
    latitude = COALESCE(latitude, -15.843300),
    longitude = COALESCE(longitude, -70.023600),
    updated_at = now()
WHERE lower(btrim(COALESCE(NULLIF(btrim(district), ''), name))) = 'puno'
   OR lower(btrim(name)) LIKE '% - puno';

UPDATE public.sites
SET district = 'Putina',
    province = 'San Antonio de Putina',
    department = 'Puno',
    latitude = COALESCE(latitude, -14.914100),
    longitude = COALESCE(longitude, -69.868480),
    updated_at = now()
WHERE lower(btrim(COALESCE(NULLIF(btrim(district), ''), name))) = 'putina'
   OR lower(btrim(name)) LIKE '% - putina';

UPDATE public.sites
SET district = 'Lampa',
    province = 'Lampa',
    department = 'Puno',
    latitude = COALESCE(latitude, -15.364430),
    longitude = COALESCE(longitude, -70.366620),
    updated_at = now()
WHERE lower(btrim(COALESCE(NULLIF(btrim(district), ''), name))) = 'lampa'
   OR lower(btrim(name)) LIKE '% - lampa';

-- El plan gratuito de Tuya tiene un único ciclo válido de seis meses.
UPDATE public.tuya_plans
SET renewal_interval_months = 6
WHERE renewal_interval_months <> 6;

ALTER TABLE public.tuya_plans
    DROP CONSTRAINT chk_tuya_plans_interval;

ALTER TABLE public.tuya_plans
    ADD CONSTRAINT chk_tuya_plans_interval
        CHECK (renewal_interval_months = 6);

-- Hace visible como hito completado la fecha inicial ya registrada en cada
-- plan, sin duplicarla si anteriormente se guardó en el historial.
INSERT INTO public.tuya_plan_renewal_history (
    plan_id, previous_date, renewed_on, next_renewal_on, notes
)
SELECT p.id,
       p.last_renewed_on,
       p.last_renewed_on,
       p.next_renewal_on,
       'Inicio de suscripción registrado en Tuya Developer'
FROM public.tuya_plans p
WHERE p.last_renewed_on IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM public.tuya_plan_renewal_history h
      WHERE h.plan_id = p.id
        AND h.renewed_on = p.last_renewed_on
  );
