-- Las cuentas de demostración de V6/V7 no deben existir en despliegues reales.
-- InitialAdminInitializer crea la única cuenta inicial desde ADMIN_* después de Flyway.
DELETE FROM public.app_users
WHERE username IN ('user', 'admin', 'superadmin');
