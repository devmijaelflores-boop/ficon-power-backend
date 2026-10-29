# Calendario operativo

El calendario reúne los cortes eléctricos y el control de renovación de los
planes de Tuya IoT. La consulta recibe un rango visible para evitar descargar
todo el historial.

## Identificadores y datos

- `incidents`, `devices` y `sites` conservan sus UUID porque son entidades
  públicas existentes.
- `tuya_plans` y `tuya_plan_renewal_history` usan `BIGINT GENERATED AS
  IDENTITY`: son tablas internas y pequeñas, por lo que no generan UUID.
- La migración `V17__operations_calendar.sql` crea planes, historial, índices y
  eventos del outbox en tiempo real.

## API

| Método | Ruta | Uso |
| --- | --- | --- |
| `GET` | `/api/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD` | Incidentes que se solapan con el rango, renovaciones previstas e historial completado. Máximo: 370 días. |
| `GET` | `/api/calendar/tuya-plans` | Lista de planes configurados. |
| `POST` | `/api/calendar/tuya-plans` | Crea un seguimiento desde una fecha confirmada en Tuya. |
| `PUT` | `/api/calendar/tuya-plans/{id}` | Actualiza solo los metadatos del seguimiento. |
| `POST` | `/api/calendar/tuya-plans/{id}/renew` | Registra la renovación y calcula la próxima fecha. |
| `DELETE` | `/api/calendar/tuya-plans/{id}` | Elimina el plan y su historial. |

La lectura requiere sesión autenticada. Las mutaciones requieren rol `ADMIN`,
`SUPERADMIN` o cliente API. Todas las mutaciones administrativas se auditan.

## Reglas de renovación

El plan gratuito usa un ciclo fijo de seis meses y una anticipación de 0 a 365
días. Sus fechas son inmutables porque proceden de Tuya Developer; una nueva
fecha solo se incorpora al registrar una renovación confirmada. Si se renueva
antes del vencimiento, el ciclo siguiente parte de la fecha de
vencimiento existente; si ya venció, parte de la fecha real de renovación. Una
renovación no admite una fecha futura.

La migración `V18` incorpora Putina y Lampa, completa Puno y asigna como
ubicación referencial el centro urbano de cada ciudad. También convierte la
primera fecha ya registrada de cada suscripción en un hito visible, sin
duplicar entradas existentes.

La migración `V20` mueve la ubicación referencial de las sedes de Puno al
centro poblado de Alto Puno (`-15.811540, -70.032300`).

Los estados visuales son `SCHEDULED`, `DUE_SOON`, `OVERDUE` y `COMPLETED`.
Los cambios publican `entity_type=calendar` en `realtime_outbox`, de modo que el
frontend vuelve a consultar únicamente el rango visible.
