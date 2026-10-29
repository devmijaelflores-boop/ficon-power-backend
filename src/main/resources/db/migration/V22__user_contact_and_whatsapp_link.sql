ALTER TABLE public.app_users
    ADD COLUMN email varchar(254);

CREATE UNIQUE INDEX uq_app_users_email
    ON public.app_users (lower(email))
    WHERE email IS NOT NULL;

ALTER TABLE public.whatsapp_recipients
    ADD COLUMN user_id uuid,
    ADD CONSTRAINT fk_whatsapp_recipients_user
        FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uq_whatsapp_recipients_user
    ON public.whatsapp_recipients (user_id)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_notification_deliveries_user_whatsapp
    ON public.notification_deliveries (user_id, created_at DESC)
    WHERE channel = 'WHATSAPP';

COMMENT ON COLUMN public.app_users.email IS
    'Correo opcional del usuario, normalizado en minúsculas por la aplicación.';

COMMENT ON COLUMN public.whatsapp_recipients.user_id IS
    'Usuario FICON asociado. NULL identifica un destinatario externo.';
