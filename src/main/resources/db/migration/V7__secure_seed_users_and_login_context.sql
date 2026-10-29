ALTER TABLE public.login_attempts
    ADD COLUMN IF NOT EXISTS user_id uuid,
    ADD COLUMN IF NOT EXISTS user_agent text;

ALTER TABLE public.login_attempts
    ADD CONSTRAINT fk_login_attempts_user
        FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_login_attempts_user_created
    ON public.login_attempts (user_id, created_at DESC);

UPDATE public.app_users
SET password_hash = CASE username
        WHEN 'user' THEN '{bcrypt}$2a$10$ngxEBpJL/7Q7KQioFDRuu.CYUvPiAXgHiabG1FjAUGC.E5EVa02sG'
        WHEN 'admin' THEN '{bcrypt}$2a$10$BYyGoChzVJXWpBDr.aOMe.yvqCeAB1b6n/rLkbFWkt0kB.ylPDd2m'
        WHEN 'superadmin' THEN '{bcrypt}$2a$10$Hz6oWAX3GhqQi8kyFdXd2exsdDPoAV93FTqzUTaUbsG4pw5CFHtbK'
        ELSE password_hash
    END
WHERE username IN ('user', 'admin', 'superadmin')
  AND password_hash LIKE '{noop}%';

COMMENT ON COLUMN public.login_attempts.user_id IS 'Usuario relacionado cuando el username existe; null en intentos contra usuarios inexistentes.';
COMMENT ON COLUMN public.login_attempts.user_agent IS 'Navegador o cliente desde donde se intento iniciar sesion.';
