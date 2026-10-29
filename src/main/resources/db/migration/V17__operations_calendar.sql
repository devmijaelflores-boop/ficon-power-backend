CREATE TABLE public.tuya_plans (
    id                       bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_name             varchar(120) NOT NULL,
    plan_name                varchar(120) NOT NULL,
    last_renewed_on          date,
    next_renewal_on          date         NOT NULL,
    renewal_interval_months  smallint     NOT NULL DEFAULT 6,
    reminder_days            smallint     NOT NULL DEFAULT 30,
    notes                    varchar(500),
    is_active                boolean      NOT NULL DEFAULT true,
    created_at               timestamptz  NOT NULL DEFAULT now(),
    updated_at               timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_tuya_plans_interval
        CHECK (renewal_interval_months BETWEEN 1 AND 60),
    CONSTRAINT chk_tuya_plans_reminder
        CHECK (reminder_days BETWEEN 0 AND 365)
);

CREATE UNIQUE INDEX uq_tuya_plans_project_name_ci
    ON public.tuya_plans (lower(project_name));

CREATE INDEX idx_tuya_plans_next_renewal
    ON public.tuya_plans (is_active, next_renewal_on);

CREATE TRIGGER trg_tuya_plans_updated_at
BEFORE UPDATE ON public.tuya_plans
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.tuya_plan_renewal_history (
    id                bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id           bigint      NOT NULL,
    previous_date     date        NOT NULL,
    renewed_on        date        NOT NULL,
    next_renewal_on   date        NOT NULL,
    notes             varchar(500),
    created_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_tuya_plan_history_plan
        FOREIGN KEY (plan_id) REFERENCES public.tuya_plans(id) ON DELETE CASCADE
);

CREATE INDEX idx_tuya_plan_history_renewed_on
    ON public.tuya_plan_renewal_history (renewed_on DESC);

CREATE OR REPLACE FUNCTION public.enqueue_operations_calendar_change()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
    change_operation text := CASE TG_OP
        WHEN 'INSERT' THEN 'created'
        WHEN 'UPDATE' THEN 'updated'
        WHEN 'DELETE' THEN 'deleted'
    END;
    changed_id text;
BEGIN
    IF TG_TABLE_NAME = 'tuya_plan_renewal_history' THEN
        changed_id := CASE TG_OP
            WHEN 'DELETE' THEN OLD.plan_id::text
            ELSE NEW.plan_id::text
        END;
    ELSE
        changed_id := CASE TG_OP
            WHEN 'DELETE' THEN OLD.id::text
            ELSE NEW.id::text
        END;
    END IF;

    INSERT INTO public.realtime_outbox (
        event_type, entity_type, entity_id, payload
    ) VALUES (
        'calendar.' || change_operation,
        'calendar',
        changed_id,
        jsonb_build_object('source', TG_TABLE_NAME)
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_realtime_tuya_plans
AFTER INSERT OR UPDATE OR DELETE ON public.tuya_plans
FOR EACH ROW EXECUTE FUNCTION public.enqueue_operations_calendar_change();

CREATE TRIGGER trg_realtime_tuya_plan_history
AFTER INSERT OR UPDATE OR DELETE ON public.tuya_plan_renewal_history
FOR EACH ROW EXECUTE FUNCTION public.enqueue_operations_calendar_change();

COMMENT ON TABLE public.tuya_plans IS
    'Planes o proyectos Tuya cuya renovación se supervisa desde el calendario operativo.';
COMMENT ON TABLE public.tuya_plan_renewal_history IS
    'Historial inmutable de renovaciones Tuya registradas por los administradores.';
