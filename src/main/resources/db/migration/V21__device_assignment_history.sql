-- Preserve the physical device identity while recording every operational
-- placement. BIGINT is sufficient because assignments are internal history.
CREATE TABLE public.device_assignments (
    id                 bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id          uuid         NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    site_id            uuid         NOT NULL REFERENCES public.sites(id) ON DELETE RESTRICT,
    tuya_container_id  varchar(100),
    source_name        varchar(150),
    canonical_name     varchar(150) NOT NULL,
    assigned_at        timestamptz  NOT NULL DEFAULT now(),
    unassigned_at      timestamptz,
    active             boolean      NOT NULL DEFAULT true,
    CONSTRAINT chk_device_assignment_period
        CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at)
);

CREATE UNIQUE INDEX uq_device_assignments_active_device
    ON public.device_assignments (device_id) WHERE active;

CREATE INDEX idx_device_assignments_device_period
    ON public.device_assignments (device_id, assigned_at DESC);

CREATE INDEX idx_device_assignments_site_period
    ON public.device_assignments (site_id, assigned_at DESC);

INSERT INTO public.device_assignments (
    device_id, site_id, canonical_name, assigned_at, unassigned_at, active
)
SELECT d.id, d.site_id, d.name, d.created_at,
       CASE WHEN d.is_active THEN NULL ELSE d.updated_at END,
       d.is_active
FROM public.devices d;

ALTER TABLE public.events
    ADD COLUMN assignment_id bigint REFERENCES public.device_assignments(id) ON DELETE SET NULL,
    ADD COLUMN site_id uuid REFERENCES public.sites(id) ON DELETE RESTRICT;

ALTER TABLE public.incidents
    ADD COLUMN assignment_id bigint REFERENCES public.device_assignments(id) ON DELETE SET NULL,
    ADD COLUMN site_id uuid REFERENCES public.sites(id) ON DELETE RESTRICT;

UPDATE public.events e
SET assignment_id = a.id,
    site_id = a.site_id
FROM public.device_assignments a
WHERE a.device_id = e.device_id;

UPDATE public.incidents i
SET assignment_id = a.id,
    site_id = a.site_id
FROM public.device_assignments a
WHERE a.device_id = i.device_id;

CREATE INDEX idx_events_assignment_occurred
    ON public.events (assignment_id, occurred_at DESC);

CREATE INDEX idx_incidents_assignment_start
    ON public.incidents (assignment_id, start_time DESC);

COMMENT ON TABLE public.device_assignments IS
    'Periodos de instalación de un medidor físico en una sede.';
