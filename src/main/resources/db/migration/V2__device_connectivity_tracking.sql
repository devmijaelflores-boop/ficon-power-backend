ALTER TABLE public.devices
    ADD COLUMN name varchar(150),
    ADD COLUMN pending_offline_since timestamptz;

UPDATE public.devices
SET name = site
WHERE name IS NULL OR btrim(name) = '';

ALTER TABLE public.devices
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE public.events
    ADD COLUMN occurred_at timestamptz;

UPDATE public.events
SET occurred_at = received_at
WHERE occurred_at IS NULL;

ALTER TABLE public.events
    ALTER COLUMN occurred_at SET NOT NULL;

CREATE INDEX idx_devices_pending_offline
    ON public.devices (pending_offline_since)
    WHERE pending_offline_since IS NOT NULL;

CREATE UNIQUE INDEX uq_incidents_open_device
    ON public.incidents (device_id)
    WHERE status IN ('active', 'acknowledged');
