-- Normalize locations, device identifiers, technician contacts and roles.
-- All operational timestamps remain timestamptz; America/Lima is applied by
-- application sessions and API responses so the represented instant is preserved.

CREATE TABLE public.sites (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(150) NOT NULL,
    address     varchar(250),
    district    varchar(100),
    province    varchar(100),
    department  varchar(100),
    latitude    numeric(9, 6),
    longitude   numeric(9, 6),
    time_zone   varchar(50)  NOT NULL DEFAULT 'America/Lima',
    is_active   boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_sites_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_sites_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE UNIQUE INDEX uq_sites_name_ci ON public.sites (lower(name));

INSERT INTO public.sites (name)
SELECT min(CASE WHEN btrim(site) = '' THEN 'Sin sede asignada' ELSE btrim(site) END)
FROM public.devices
GROUP BY lower(CASE WHEN btrim(site) = '' THEN 'Sin sede asignada' ELSE btrim(site) END);

INSERT INTO public.sites (name)
VALUES ('Sin sede asignada')
ON CONFLICT DO NOTHING;

ALTER TABLE public.devices
    RENAME COLUMN device_id TO external_id;

ALTER TABLE public.devices
    ADD COLUMN site_id uuid,
    ADD COLUMN provider varchar(30) NOT NULL DEFAULT 'tuya',
    ADD COLUMN category varchar(80),
    ADD COLUMN product_id varchar(100),
    ADD COLUMN model varchar(120),
    ADD COLUMN is_active boolean NOT NULL DEFAULT true,
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

UPDATE public.devices d
SET site_id = s.id
FROM public.sites s
WHERE lower(s.name) = lower(
    CASE WHEN btrim(d.site) = '' THEN 'Sin sede asignada' ELSE btrim(d.site) END
);

ALTER TABLE public.devices
    ALTER COLUMN site_id SET NOT NULL,
    ADD CONSTRAINT fk_devices_site
        FOREIGN KEY (site_id) REFERENCES public.sites(id) ON DELETE RESTRICT;

DROP INDEX IF EXISTS public.idx_devices_pending_offline;

ALTER TABLE public.devices
    DROP COLUMN site,
    DROP COLUMN pending_offline_since;

CREATE INDEX idx_devices_site_status
    ON public.devices (site_id, status, last_status_change DESC);

ALTER TABLE public.events
    ADD COLUMN device_uuid uuid;

UPDATE public.events e
SET device_uuid = d.id
FROM public.devices d
WHERE e.device_id = d.external_id;

ALTER TABLE public.events
    ALTER COLUMN device_uuid SET NOT NULL;

ALTER TABLE public.events
    DROP CONSTRAINT IF EXISTS events_device_id_fkey;

DROP INDEX IF EXISTS public.idx_events_device_received;

ALTER TABLE public.events
    DROP COLUMN device_id;

ALTER TABLE public.events
    RENAME COLUMN device_uuid TO device_id;

ALTER TABLE public.events
    ADD CONSTRAINT fk_events_device
        FOREIGN KEY (device_id) REFERENCES public.devices(id) ON DELETE CASCADE;

CREATE INDEX idx_events_device_occurred
    ON public.events (device_id, occurred_at DESC);

CREATE TABLE public.roles (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    code        varchar(30) NOT NULL UNIQUE,
    name        varchar(80) NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.roles (code, name)
VALUES ('admin', 'Administrador'), ('technician', 'Tecnico');

ALTER TABLE public.technicians
    ADD COLUMN role_id uuid,
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

UPDATE public.technicians t
SET role_id = r.id
FROM public.roles r
WHERE r.code = t.role;

ALTER TABLE public.technicians
    ALTER COLUMN role_id SET NOT NULL,
    ADD CONSTRAINT fk_technicians_role
        FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE RESTRICT;

CREATE TABLE public.technician_contacts (
    id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    technician_id  uuid        NOT NULL,
    contact_type   varchar(20) NOT NULL
                   CHECK (contact_type IN ('phone', 'email')),
    contact_value  varchar(255) NOT NULL,
    is_primary     boolean     NOT NULL DEFAULT false,
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_contacts_technician
        FOREIGN KEY (technician_id) REFERENCES public.technicians(id) ON DELETE CASCADE,
    CONSTRAINT uq_contact_type_value UNIQUE (contact_type, contact_value)
);

CREATE UNIQUE INDEX uq_technician_primary_contact_type
    ON public.technician_contacts (technician_id, contact_type)
    WHERE is_primary;

INSERT INTO public.technician_contacts (technician_id, contact_type, contact_value, is_primary)
SELECT id, 'phone', phone, true
FROM public.technicians
WHERE phone IS NOT NULL AND btrim(phone) <> '';

INSERT INTO public.technician_contacts (technician_id, contact_type, contact_value, is_primary)
SELECT id, 'email', email, true
FROM public.technicians
WHERE email IS NOT NULL AND btrim(email) <> '';

ALTER TABLE public.technicians
    DROP COLUMN phone,
    DROP COLUMN email,
    DROP COLUMN fcm_token,
    DROP COLUMN role;

ALTER TABLE public.incidents
    ADD COLUMN acknowledged_at timestamptz,
    ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now(),
    ADD CONSTRAINT chk_incidents_time_order
        CHECK (end_time IS NULL OR end_time >= start_time);

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sites_updated_at
BEFORE UPDATE ON public.sites
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_devices_updated_at
BEFORE UPDATE ON public.devices
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_technicians_updated_at
BEFORE UPDATE ON public.technicians
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_incidents_updated_at
BEFORE UPDATE ON public.incidents
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

COMMENT ON TABLE public.sites IS 'Ubicaciones normalizadas de los dispositivos.';
COMMENT ON COLUMN public.sites.time_zone IS 'Zona IANA usada para presentar las fechas de la sede.';
COMMENT ON COLUMN public.devices.external_id IS 'Identificador externo asignado por Tuya; no se muestra en WhatsApp.';
COMMENT ON COLUMN public.events.occurred_at IS 'Instante informado por Tuya, almacenado como timestamptz.';
