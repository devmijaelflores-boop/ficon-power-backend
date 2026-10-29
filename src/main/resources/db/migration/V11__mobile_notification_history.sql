ALTER TABLE public.notification_deliveries
    ADD COLUMN notification_key uuid,
    ADD COLUMN title varchar(160),
    ADD COLUMN body text,
    ADD COLUMN data jsonb NOT NULL DEFAULT '{}'::jsonb;

CREATE INDEX idx_notification_deliveries_user_unread
    ON public.notification_deliveries (user_id, read_at, created_at DESC)
    WHERE channel = 'PUSH';

CREATE INDEX idx_notification_deliveries_notification_key
    ON public.notification_deliveries (notification_key)
    WHERE notification_key IS NOT NULL;

COMMENT ON COLUMN public.notification_deliveries.notification_key IS
    'Agrupa los intentos del mismo mensaje enviados a varios tokens.';
COMMENT ON COLUMN public.notification_deliveries.data IS
    'Metadatos de navegación y contexto enviados mediante FCM.';
