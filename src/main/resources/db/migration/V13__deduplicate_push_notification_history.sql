WITH ranked_notifications AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY user_id, notification_key
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM public.notification_deliveries
    WHERE channel = 'PUSH'
      AND notification_key IS NOT NULL
)
DELETE FROM public.notification_deliveries delivery
USING ranked_notifications ranked
WHERE delivery.id = ranked.id
  AND ranked.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_deliveries_push_user_key
    ON public.notification_deliveries (user_id, notification_key)
    WHERE channel = 'PUSH'
      AND notification_key IS NOT NULL;

COMMENT ON INDEX public.ux_notification_deliveries_push_user_key IS
    'Evita duplicados visibles en el historial móvil para el mismo usuario y la misma alerta push.';
