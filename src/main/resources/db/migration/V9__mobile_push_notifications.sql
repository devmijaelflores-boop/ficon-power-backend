CREATE TABLE public.mobile_device_tokens (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid         NOT NULL,
    token        text         NOT NULL,
    platform     varchar(30)  NOT NULL DEFAULT 'android',
    device_name  varchar(120),
    app_version  varchar(40),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    last_seen_at timestamptz  NOT NULL DEFAULT now(),
    revoked_at   timestamptz,
    CONSTRAINT uq_mobile_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_mobile_device_tokens_user
        FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE CASCADE
);

CREATE TRIGGER trg_mobile_device_tokens_updated_at
BEFORE UPDATE ON public.mobile_device_tokens
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE INDEX idx_mobile_device_tokens_user_active
    ON public.mobile_device_tokens (user_id, revoked_at, last_seen_at DESC);

CREATE INDEX idx_mobile_device_tokens_active
    ON public.mobile_device_tokens (revoked_at, last_seen_at DESC);

CREATE TABLE public.notification_deliveries (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id         uuid,
    event_id            uuid,
    user_id             uuid,
    channel             varchar(30)  NOT NULL,
    provider            varchar(40)  NOT NULL,
    recipient           text,
    event_type          varchar(60),
    template_name       varchar(120),
    status              varchar(30)  NOT NULL,
    provider_message_id text,
    error_code          varchar(120),
    error_message       text,
    attempt_count       integer      NOT NULL DEFAULT 1,
    sent_at             timestamptz,
    delivered_at        timestamptz,
    read_at             timestamptz,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT fk_notification_deliveries_incident
        FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_deliveries_event
        FOREIGN KEY (event_id) REFERENCES public.events(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_deliveries_user
        FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE SET NULL,
    CONSTRAINT chk_notification_deliveries_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DELIVERED', 'READ', 'SKIPPED')),
    CONSTRAINT chk_notification_deliveries_channel
        CHECK (channel IN ('WHATSAPP', 'PUSH'))
);

CREATE INDEX idx_notification_deliveries_created
    ON public.notification_deliveries (created_at DESC);

CREATE INDEX idx_notification_deliveries_incident
    ON public.notification_deliveries (incident_id, created_at DESC);

CREATE INDEX idx_notification_deliveries_user
    ON public.notification_deliveries (user_id, created_at DESC);

COMMENT ON TABLE public.mobile_device_tokens IS 'Tokens FCM activos o revocados por usuario y dispositivo móvil.';
COMMENT ON TABLE public.notification_deliveries IS 'Historial de intentos de envío por WhatsApp o push móvil.';
