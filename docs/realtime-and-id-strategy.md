# Actualizaciones en tiempo real e identificadores

## Decisión de arquitectura

PostgreSQL/Supabase continúa siendo la base de datos administrada y Spring Boot
continúa siendo el backend de la aplicación. El frontend no se conecta
directamente a Supabase: conserva la autenticación y las reglas de autorización
de Spring.

Cada cambio relevante crea, dentro de la misma transacción, una fila mínima en
`realtime_outbox`. Si la transacción se revierte, el aviso también se revierte.
El backend reclama esas filas con `FOR UPDATE SKIP LOCKED`, publica una
invalidación mediante `GET /api/realtime/stream` (SSE) y después las marca como
publicadas.

SSE no transporta el estado completo ni reemplaza la API REST. Le indica al
cliente qué entidad cambió para que consulte nuevamente la vista correspondiente.
Esto evita consultas cada 5 o 10 segundos y mantiene una recarga de seguridad
mucho menos frecuente.

## Privacidad

Hay tres audiencias:

- `AUTHENTICATED`: dispositivos, sedes, incidentes y eventos.
- `ADMIN`: usuarios, sesiones, intentos de acceso, auditoría, plantillas y
  destinatarios de WhatsApp.
- `USER`: historial de notificaciones; solo lo recibe el usuario propietario.

El payload nunca incluye tokens, contraseñas, teléfonos, metadatos de auditoría
ni contenido de notificaciones.

## Estrategia de identificadores

Se mantienen UUID en entidades públicas o de negocio como usuarios,
dispositivos, sedes, incidentes y sesiones. La migración `V16` cambia a
`bigint identity` los identificadores internos de tablas históricas de crecimiento
continuo:

- `events.id`
- `login_attempts.id`
- `audit_logs.id`
- `notification_deliveries.id`
- `notification_deliveries.event_id`, por ser FK de `events.id`
- `realtime_outbox.id`

`notification_deliveries.notification_key` sigue siendo UUID porque es el
identificador público que utiliza la APK para leer una notificación. Las APIs
serializan los `bigint` visibles como texto para no perder precisión en clientes
JavaScript.

La migración crea las columnas secuenciales, copia la relación evento-entrega y
reconstruye las restricciones; no elimina filas históricas.

## Operación

Variables opcionales y valores por defecto:

```properties
REALTIME_POLL_DELAY=750
REALTIME_HEARTBEAT_DELAY=20000
REALTIME_BATCH_SIZE=100
REALTIME_RETENTION_DAYS=3
REALTIME_CLEANUP_CRON=0 17 3 * * *
```

Las filas publicadas del outbox se conservan tres días para diagnóstico. Un
cliente que se reconecta vuelve a consultar REST, por lo que no necesita
reproducir todo el historial SSE.

## Comprobación rápida

Con una sesión válida:

```bash
curl -N \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: text/event-stream" \
  http://localhost:8080/api/realtime/stream
```

En otra terminal, actualice un dispositivo o inserte un evento. El stream debe
mostrar `device.updated` o `event.created` y la fila correspondiente debe tener
`published_at` en `realtime_outbox`.
