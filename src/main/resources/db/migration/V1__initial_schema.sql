CREATE TABLE public.technicians (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(100) NOT NULL,
    phone       varchar(20)  NOT NULL UNIQUE,
    email       varchar(100) UNIQUE,
    fcm_token   text,
    role        varchar(30)  NOT NULL DEFAULT 'technician'
                CHECK (role IN ('admin', 'technician')),
    is_active   boolean      DEFAULT true,
    created_at  timestamptz  DEFAULT now()
);

CREATE TABLE public.devices (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id           varchar(50)  NOT NULL UNIQUE,
    site                varchar(100) NOT NULL,
    status              varchar(20)  NOT NULL DEFAULT 'online'
                        CHECK (status IN ('online', 'offline')),
    last_status_change  timestamptz,
    created_at          timestamptz  DEFAULT now()
);

CREATE INDEX idx_devices_status_last_change
    ON public.devices (status, last_status_change);

CREATE TABLE public.incidents (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id         uuid         NOT NULL,
    start_time        timestamptz  NOT NULL,
    end_time          timestamptz,
    status            varchar(20)  NOT NULL DEFAULT 'active'
                      CHECK (status IN ('active', 'acknowledged', 'resolved')),
    acknowledged_by   uuid,
    notes             text,
    created_at        timestamptz  DEFAULT now(),
    CONSTRAINT fk_incidents_device
        FOREIGN KEY (device_id) REFERENCES public.devices(id) ON DELETE CASCADE,
    CONSTRAINT fk_incidents_technician
        FOREIGN KEY (acknowledged_by) REFERENCES public.technicians(id) ON DELETE SET NULL
);

CREATE INDEX idx_incidents_device_status
    ON public.incidents (device_id, status);

CREATE INDEX idx_incidents_status_start_time
    ON public.incidents (status, start_time);

CREATE TABLE public.events (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id   varchar(50)  NOT NULL,
    event_type  varchar(100) NOT NULL,
    payload     jsonb        NOT NULL,
    received_at timestamptz  DEFAULT now(),
    CONSTRAINT events_device_id_fkey
        FOREIGN KEY (device_id) REFERENCES public.devices(device_id)
);

CREATE INDEX idx_events_device_received
    ON public.events (device_id, received_at);
