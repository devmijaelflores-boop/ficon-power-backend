-- Plantillas editables de los mensajes de notificacion por WhatsApp (modo texto).
-- Los placeholders {dispositivo}, {fechaCorte} y {fechaRetorno} se reemplazan en el adaptador.

CREATE TABLE public.notification_templates (
    event_type  varchar(30)  PRIMARY KEY,
    body        text         NOT NULL,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_notification_templates_body_not_blank
        CHECK (btrim(body) <> '')
);

INSERT INTO public.notification_templates (event_type, body)
VALUES (
    'power_outage',
    E'DEVICE OFF\nDispositivo: {dispositivo}\nFecha y hora del corte: {fechaCorte}'
), (
    'power_restored',
    E'DEVICE ON\nDispositivo: {dispositivo}\nFecha y hora del corte: {fechaCorte}\nFecha y hora de retorno: {fechaRetorno}'
);

COMMENT ON TABLE public.notification_templates IS 'Plantillas editables de los mensajes WhatsApp en modo texto (no aplican al modo plantilla Meta).';
