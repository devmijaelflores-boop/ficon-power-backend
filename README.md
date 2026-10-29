# FICON Power Backend

Backend en Java 21 y Spring Boot para el sistema de gestión de continuidad eléctrica y alerta temprana de Grupo FICON S.A.C.

## Tecnologías

- Java 21
- Spring Boot 4.1
- PostgreSQL / Supabase
- Flyway
- Maven
- Docker
- Tuya Pulsar SDK / Apache Pulsar
- Zona horaria operativa `America/Lima`

## Versiones y requisitos

| Componente | Versión | Fuente |
|------------|---------|--------|
| Java (JDK) | `21` | `pom.xml` |
| Maven Wrapper | `3.9.16` | `.mvn/wrapper/maven-wrapper.properties` |
| Spring Boot | `4.1.0` | `pom.xml` |
| Apache Pulsar Client | `4.1.3` | `pom.xml` |
| PostgreSQL | Compatible con el driver administrado por Spring Boot 4.1 | Ejecución normal |
| Docker Engine + Compose | Compose v2 (`docker compose`) | Ejecución en contenedor |

Maven administra también las versiones compatibles de Spring Web, RestClient, Security, Validation, JDBC, Flyway, PostgreSQL y H2. Las dependencias de prueba se obtienen mediante `spring-boot-starter-test`.

## Instalación de dependencias

Con un JDK 21 disponible en `JAVA_HOME` y en el `PATH`, use el wrapper incluido; no hace falta instalar Maven globalmente:

```bash
java -version
./mvnw --version
./mvnw dependency:go-offline
```

En Windows:

```powershell
java -version
.\mvnw.cmd --version
.\mvnw.cmd dependency:go-offline
```

`dependency:go-offline` descarga plugins y dependencias de compilación, ejecución y pruebas declaradas en `pom.xml`. Como alternativa, `docker compose build` realiza esta instalación dentro de la etapa Maven de la imagen, con JDK 21, sin requerir Java ni Maven en el host.

## Configuración

Copie `.env.example` como `.env` y complete las credenciales. Se admite `DATABASE_URL` en formato de PostgreSQL (`postgres://...`) o JDBC (`jdbc:postgresql://...`). También se pueden usar `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `DB_SSLMODE`.

## Ejecución local

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Docker

```bash
docker compose up --build -d
docker compose logs -f app
```

El servicio escucha en `http://localhost:8080`.

Para detenerlo:

```bash
docker compose down
```

### Sistema completo y prueba de conexión con el frontend

El `docker-compose.yml` del directorio padre construye ambos proyectos en una red compartida. El backend se identifica internamente como `ficon-power-backend`; nginx, dentro del frontend, reenvía `/api/*` a `http://ficon-power-backend:8080`.

```bash
cd ..
cp ficon-power-backend/.env.example ficon-power-backend/.env
# Complete ficon-power-backend/.env.
docker compose build
docker compose up -d
docker compose ps

# Prueba directa desde el contenedor frontend al backend.
docker compose exec frontend wget -qO- http://ficon-power-backend:8080/

# Prueba del recorrido usado por el navegador: host -> nginx -> backend.
curl -i http://localhost:3000/api/sites
```

La primera prueba debe devolver JSON con `"service":"ficon-power-backend"` y `"status":"UP"`; la segunda debe responder HTTP 200. Si se configura `API_KEY` en `ficon-power-backend/.env`, el mismo valor debe definirse en el `.env` del directorio padre para que nginx envíe el header `X-API-Key`.

Los cambios de conectividad se notifican por WhatsApp usando plantillas aprobadas en Meta. El backend no envía texto libre ni permite editar mensajes desde el dashboard; el texto aprobado se administra en Meta Business y el backend solo envía los parámetros configurados.

### Firebase Cloud Messaging

El backend envía alertas FCM a los tokens registrados por la APK, incluso
cuando la aplicación está cerrada. La cuenta de servicio se guarda fuera de
Git en `secrets/firebase-service-account.json`; el compose del frontend monta
esa carpeta dentro del contenedor como `/app/secrets:ro`.

```env
FCM_ENABLED=true
FCM_PROJECT_ID=ficon-power-prod
FCM_CREDENTIALS_PATH=/app/secrets/firebase-service-account.json
FCM_CREDENTIALS_JSON=
FCM_DRY_RUN=false
```

`FCM_PROJECT_ID` debe ser el `project_id` de Firebase y no el App ID Android.
La ruta de credenciales corresponde al contenedor, no a la ruta absoluta del
VPS. La guía de registro Android, build y diagnóstico se encuentra en
`../ficon-power-frontend/docs/firebase-mobile.md` cuando ambos repositorios se
clonan como directorios hermanos.

### Plantillas Meta de WhatsApp

Ejemplo para plantillas aprobadas con variables:

```env
WHATSAPP_TEMPLATE_LANGUAGE=en
WHATSAPP_OUTAGE_TEMPLATE_NAME=power_outage_alert
WHATSAPP_OUTAGE_TEMPLATE_PARAMS=dispositivo,fechaCorte
WHATSAPP_RESTORED_TEMPLATE_NAME=power_restored_alert
WHATSAPP_RESTORED_TEMPLATE_PARAMS=dispositivo,fechaCorte,fechaRetorno
```

Si falta una plantilla para un evento, la notificación de WhatsApp se omite y queda advertida en logs para evitar enviar texto libre fuera de la ventana permitida por WhatsApp.

### Auditoría y credenciales locales

La base de datos registra:

- `login_attempts`: usuario, usuario relacionado cuando existe, IP, navegador, éxito/fallo, motivo y fecha.
- `audit_logs`: cambios operativos, usuario, rol, entidad afectada, IP, navegador y metadata.

Las contraseñas se almacenan con hash BCrypt mediante el `PasswordEncoder` de Spring Security. Para pruebas locales, las credenciales fuertes de ejemplo están en `.local/credentials.local.md`; esa ruta está ignorada por Git y no debe publicarse.

El orden de `*_TEMPLATE_PARAMS` debe coincidir con el orden de variables aprobado en Meta. Para una plantilla de corte como `power_outage_alert` con dos variables, el backend envía:

```json
{
  "messaging_product": "whatsapp",
  "to": "51999999999",
  "type": "template",
  "template": {
    "name": "power_outage_alert",
    "language": {
      "code": "en"
    },
    "components": [
      {
        "type": "body",
        "parameters": [
          {
            "type": "text",
            "text": "Medidor principal"
          },
          {
            "type": "text",
            "text": "06/08/2026 10:30:00"
          }
        ]
      }
    ]
  }
}
```

En ese ejemplo, `WHATSAPP_OUTAGE_TEMPLATE_PARAMS=dispositivo,fechaCorte` significa que `dispositivo` llena `{{1}}` y `fechaCorte` llena `{{2}}`. Para retorno, `WHATSAPP_RESTORED_TEMPLATE_PARAMS=dispositivo,fechaCorte,fechaRetorno` llena `{{1}}`, `{{2}}` y `{{3}}`.

Si la plantilla aprobada no tiene variables, deje vacío el listado de parámetros:

```env
WHATSAPP_OUTAGE_TEMPLATE_NAME=power_outage_alert
WHATSAPP_OUTAGE_TEMPLATE_PARAMS=
```

Parámetros disponibles: `dispositivo`, `fechaCorte`, `fechaRetorno`. También se aceptan alias en `.env`: `device`, `outage_at`, `restored_at`, `fecha_corte`, `fecha_retorno`.

## API de monitoreo

- `GET /` — estado del servicio y hora actual de Perú.
- `GET /api/sites` — sedes, ubicación, zona horaria y cantidad de dispositivos.
- `GET /api/devices` — catálogo Tuya, sede, estado, conteos e indisponibilidad acumulada.
- `GET /api/incidents?limit=100` — cortes, duración, dispositivo, sede y técnico.
- `GET /api/events?limit=100` — eventos Tuya, latencia de entrega y payload original.
- `GET /api/notification-templates` — configuración de plantillas Meta por evento (`power_outage`, `power_restored`).
- `PUT /api/devices/{id}` — actualiza el nombre y la sede de un dispositivo. Body JSON: `{"name": "...", "siteId": "..."}`. El nombre editado queda protegido: la sincronización con Tuya ya no lo sobrescribe (columna `name_customized`).
- `POST /api/sites` — crea una sede. Body JSON: `{"name": "...", "district": "...", "province": "..."}` (solo `name` es obligatorio; nombre duplicado devuelve 409).

Las fechas de la API se entregan como ISO-8601 con el offset de Perú (`-05:00`).

## Seguridad de la API

- El dashboard usa login por token Bearer contra `POST /api/auth/login`.
- Las credenciales locales de ejemplo están en `.local/credentials.local.md`, archivo ignorado por Git.
- Roles:
  - `USER`: solo lectura.
  - `ADMIN`: lectura y edición operativa de dispositivos y sedes.
  - `SUPERADMIN`: controla usuarios además de las funciones de admin.
- `API_KEY` queda como acceso alterno para integraciones máquina-a-máquina mediante `X-API-Key`.
- Después de 5 intentos fallidos desde la misma IP en 10 minutos, el login responde HTTP 429 temporalmente.
- La tabla `login_attempts` registra inicios de sesión con usuario, IP, navegador (`User-Agent`), éxito/fallo y motivo.
- La tabla `audit_logs` registra cambios con usuario, rol, IP, navegador (`User-Agent`), acción y entidad afectada.
- Los parámetros `limit` se validan con Bean Validation (`1..500`); valores fuera de rango devuelven HTTP 400.
- La aplicación envía cabeceras de seguridad por defecto (`X-Content-Type-Options`, `Content-Security-Policy`, etc.) y no usa sesiones ni cookies (CSRF deshabilitado).
- CORS está deshabilitado por defecto. Se activa solo si `CORS_ALLOWED_ORIGINS` tiene valores.
- El hook `.githooks/pre-commit` bloquea commits que incluyan `.env`. Se activa con `git config core.hooksPath .githooks`.

## Tuya Pulsar

El backend también puede recibir eventos persistentes mediante el servicio de
mensajes Pulsar de Tuya. El consumidor valida la firma MD5 de Tuya, descifra
mensajes AES-128 en modo ECB o GCM y procesa los cambios de conectividad y de
vinculación de dispositivos.

Las reglas de producción recomendadas para un proyecto administrado con Tuya
Spatial son:

- `deviceOnline` y `deviceOffline` para conectividad.
- `industryAssetDeviceBind` para registrar o reactivar inmediatamente el
  dispositivo agregado a un activo.
- `industryAssetDeviceUnbind` para marcarlo inactivo sin borrar su historial.

También se admiten los formatos compatibles `deviceBindSpace`,
`deviceUnbindSpace` y los mensajes legados `device` con `eventType` igual a
`bind`, `unbind` o `reset`.

Al vincular un medidor, el backend consulta el nombre del activo o espacio de
Tuya y admite `_`, `-` y guiones tipográficos como separadores, con o sin
espacios y sin distinguir mayúsculas. Por ejemplo, `TIMAX_puno`,
`timax- juliaca` y `Timax – Puno` se presentan respectivamente como
`TIMAX - Puno`, `TIMAX - Juliaca` y `TIMAX - Puno`, con su distrito canónico.
Antes de construir el nombre se recortan los extremos, se compactan los
espacios, se unifican los separadores y se normaliza el uso de mayúsculas. Las
palabras comunes quedan capitalizadas y las marcas conocidas, como `TIMAX`,
conservan su grafía oficial.
La vinculación Spatial es autoritativa: actualiza nombre y sede incluso si el
nombre anterior estaba protegido frente a la sincronización general de Tuya.
Actualmente se reconocen
Puno, Juliaca, Putina y Lampa. Si la sede no existe se crea con coordenadas
referenciales del centro urbano; una sede anterior con un nombre como
`Timax - Juliaca` también se reconoce y normaliza.

La identidad física del medidor sigue siendo su mismo `external_id`. Cada
cambio de espacio cierra su asignación vigente y abre un periodo en
`device_assignments`; los eventos e incidentes guardan además la asignación y
la sede que tenían al producirse. Así se conserva el historial físico, pero la
ficha y los indicadores operativos del destino nuevo no mezclan cortes de la
sede anterior. Un `unbind` cierra el periodo y deja el dispositivo inactivo;
si luego se vuelve a vincular, se reactiva sin crear otro medidor.
Los mensajes `unbind` retrasados de un activo anterior se ignoran cuando el
medidor ya está vinculado a otro, evitando que un traslado válido lo vuelva a
desactivar por desorden de entrega.
Durante una vinculación, una instantánea `offline` del catálogo no degrada un
estado `online` ya confirmado por mensajería; una instantánea `online` sí puede
promoverlo. Los eventos de conectividad con una fecha anterior al último cambio
se conservan en el historial, pero no alteran el estado actual ni los
incidentes.

Antes de activarlo, habilite **Message Service** en el proyecto de Tuya y configure:

```dotenv
TUYA_PULSAR_ENABLED=true
TUYA_PULSAR_URL=pulsar+ssl://mqe.tuyaus.com:7285/
TUYA_ACCESS_ID=TU_ACCESS_ID
TUYA_ACCESS_KEY=TU_ACCESS_SECRET
TUYA_PULSAR_ENV=prod
TUYA_PULSAR_DIRECT_NOTIFICATIONS=false
TUYA_PULSAR_TLS_TRUST_CERTS_PATH=/app/certs/tuya-pulsar-ca-bundle.pem
```

El contenedor incluye temporalmente el certificado público intermedio
`RapidSSL TLS RSA CA G1`, requerido cuando el broker de Tuya no entrega la
cadena TLS completa. El archivo procede de la URL AIA oficial de RapidSSL y su
huella SHA-256 verificada es
`44:22:E9:63:EE:53:CD:58:CC:9F:85:CD:40:BF:5F:FE:C0:09:5F:DF:1A:15:45:35:66:1C:1C:06:BC:AD:C6:9B`.
El bundle también contiene la raíz pública `DigiCert Global Root G2`, con
huella SHA-256
`CB:3C:CB:B7:60:31:E5:E0:13:8F:8D:D3:9A:23:F9:DE:47:FF:C3:5E:43:C1:14:4C:EA:27:D4:6A:5A:B1:CB:5F`.
Esta mitigación conserva la validación TLS y no contiene claves privadas.

Si Tuya no está disponible durante el arranque, la API permanece activa y el
consumidor reintenta en segundo plano a los 5, 10, 20, 30 y, posteriormente,
cada 60 segundos. Su estado puede consultarse sin exponer credenciales:

```bash
curl http://localhost:8080/health/tuya-pulsar
```

Diagnóstico reproducible para adjuntar a un caso de soporte de Tuya:

```bash
bash scripts/diagnose-tuya-pulsar-tls.sh mqe-ueaz.tuyaus.com 7285
```

El reporte debe solicitar que el broker entregue la cadena completa
`*.tuyaus.com -> RapidSSL TLS RSA CA G1 -> DigiCert Global Root G2` e incluir
la salida `Verify return code` generada por el script.

Use `TUYA_PULSAR_ENV=test` únicamente si el dispositivo está configurado en el canal de pruebas de Tuya. Para China o Europa, cambie `TUYA_PULSAR_URL` por el endpoint correspondiente de su proyecto.

Para una prueba sin Supabase, use una base H2 temporal y envíe las notificaciones directamente:

```dotenv
DATABASE_URL=jdbc:h2:mem:ficon;DB_CLOSE_DELAY=-1
DB_USER=sa
DB_PASSWORD=
SPRING_FLYWAY_ENABLED=false
TUYA_PULSAR_ENV=test
TUYA_PULSAR_DIRECT_NOTIFICATIONS=true
```

En producción, use PostgreSQL/Supabase, `TUYA_CLOUD_ENABLED=true` y `TUYA_PULSAR_DIRECT_NOTIFICATIONS=false`. El catálogo de Tuya se sincroniza con `devices`; los dispositivos nuevos se asignan temporalmente a `Sin sede asignada`, que el repositorio restaura de forma atómica si no existe. Un fallo puntual de esta sincronización inicial se registra sin detener la API. Cada evento se guarda en `events`. Un evento offline abre un incidente y notifica inmediatamente después de ser recibido desde Tuya. Al volver después de un corte confirmado, el incidente se resuelve y WhatsApp informa el nombre, la hora del corte y la hora de retorno.

## Esquema normalizado

Flyway aplica `V3__normalize_monitoring_schema.sql`. La migración separa las sedes en `sites`, normaliza roles y contactos técnicos, relaciona `events` con la clave UUID interna del dispositivo y agrega metadatos Tuya (`provider`, `category`, `product_id`, `model`). Los campos operativos usan `timestamptz`; las conexiones y respuestas se presentan en `America/Lima`.
