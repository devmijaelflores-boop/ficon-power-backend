-- A Tuya binding is authoritative for the current display name and site.
-- Reconcile rows affected by the former name_customized guard without
-- changing historical assignments, events or incidents.
UPDATE public.devices d
SET site_id = a.site_id,
    name = a.canonical_name,
    name_customized = true,
    is_active = true,
    updated_at = now()
FROM public.device_assignments a
WHERE a.device_id = d.id
  AND a.active = true
  AND (
      d.site_id IS DISTINCT FROM a.site_id
      OR d.name IS DISTINCT FROM a.canonical_name
      OR d.name_customized = false
      OR d.is_active = false
  );

COMMENT ON COLUMN public.devices.name_customized IS
    'Protege el nombre visible de la sincronizacion general con Tuya; una vinculacion Spatial explicita siempre aplica su nombre canonico.';
