# Ficha detallada del dispositivo

## Endpoint

`GET /api/devices/{id}/detail`

Devuelve una ficha calculada para un dispositivo exacto, identificado por UUID. Si el
dispositivo no existe responde `404` con el mensaje `Dispositivo no encontrado`.

## Datos incluidos

- Datos operativos y de ubicación del dispositivo.
- Incidentes de los últimos 7, 30 y 90 días.
- Duración promedio, mayor duración e indisponibilidad acumulada.
- Disponibilidad porcentual de los últimos 30 días.
- Series de incidentes por día, semana, mes, día de la semana y franja horaria.
- Los 10 incidentes más recientes del dispositivo.
- Los 10 eventos más recientes del dispositivo.

La disponibilidad considera la intersección real de cada incidente con la ventana de
30 días. Un incidente aún abierto usa la hora actual como final provisional.

## Consumo

La pantalla de dispositivos consulta este endpoint únicamente al abrir una
ficha y luego la actualiza cada 3 minutos o ante un cambio relevante en tiempo
real. La lista general continúa usando `GET /api/devices`.
