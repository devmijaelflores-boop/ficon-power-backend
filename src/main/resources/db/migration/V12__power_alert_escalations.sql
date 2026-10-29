CREATE TABLE public.power_alert_escalations (
    incident_id     uuid PRIMARY KEY,
    attempt_count   integer     NOT NULL DEFAULT 1,
    last_alert_at   timestamptz,
    next_alert_at   timestamptz NOT NULL,
    reviewed_at     timestamptz,
    completed_at    timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_power_alert_escalations_incident
        FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE CASCADE,
    CONSTRAINT chk_power_alert_escalations_attempts
        CHECK (attempt_count >= 1)
);

CREATE TRIGGER trg_power_alert_escalations_updated_at
BEFORE UPDATE ON public.power_alert_escalations
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_power_alert_escalations_due
    ON public.power_alert_escalations (next_alert_at)
    WHERE reviewed_at IS NULL AND completed_at IS NULL;

COMMENT ON TABLE public.power_alert_escalations IS
    'Controla reintentos de alertas push para incidentes activos de corte.';
COMMENT ON COLUMN public.power_alert_escalations.attempt_count IS
    'Cantidad de alertas programadas o enviadas para el incidente. El intento 1 corresponde al aviso inicial.';
