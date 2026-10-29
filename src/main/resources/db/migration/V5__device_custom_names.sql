-- Permite proteger el nombre de un dispositivo editado desde la app:
-- la sincronizacion con Tuya solo actualiza el nombre mientras no haya
-- sido personalizado por un usuario.

ALTER TABLE public.devices
    ADD COLUMN name_customized boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.devices.name_customized IS 'true cuando el nombre fue editado manualmente; la sincronizacion con Tuya no lo sobrescribe.';
