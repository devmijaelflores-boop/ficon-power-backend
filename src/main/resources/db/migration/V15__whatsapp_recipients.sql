CREATE TABLE public.whatsapp_recipients (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    label               varchar(120) NOT NULL,
    phone               varchar(15)  NOT NULL,
    site_id             uuid,
    notify_outage       boolean      NOT NULL DEFAULT true,
    notify_restored     boolean      NOT NULL DEFAULT true,
    is_active           boolean      NOT NULL DEFAULT true,
    last_test_status    varchar(20),
    last_test_message   varchar(500),
    last_test_at        timestamptz,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT fk_whatsapp_recipients_site
        FOREIGN KEY (site_id) REFERENCES public.sites(id) ON DELETE RESTRICT,
    CONSTRAINT chk_whatsapp_recipients_phone
        CHECK (phone ~ '^[1-9][0-9]{7,14}$'),
    CONSTRAINT chk_whatsapp_recipients_event
        CHECK (notify_outage OR notify_restored),
    CONSTRAINT chk_whatsapp_recipients_test_status
        CHECK (last_test_status IS NULL OR last_test_status IN ('SENT', 'FAILED'))
);

CREATE UNIQUE INDEX uq_whatsapp_recipients_phone_scope
    ON public.whatsapp_recipients (phone, COALESCE(site_id, '00000000-0000-0000-0000-000000000000'::uuid));

CREATE INDEX idx_whatsapp_recipients_active_site
    ON public.whatsapp_recipients (is_active, site_id);

CREATE TRIGGER trg_whatsapp_recipients_updated_at
BEFORE UPDATE ON public.whatsapp_recipients
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

COMMENT ON TABLE public.whatsapp_recipients IS
    'Destinatarios configurables de plantillas WhatsApp, globales o asociados a una sede.';
