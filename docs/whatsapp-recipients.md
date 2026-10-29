# Destinatarios de WhatsApp

Los destinatarios se administran mediante `/api/whatsapp-recipients`. Pueden
tener alcance global (`siteId = null`) o quedar asociados a una sede, además de
seleccionar alertas de corte, reconexión o ambas.

La API permite listar, crear, modificar, eliminar y enviar una plantilla de
prueba. Solo los roles `ADMIN`, `SUPERADMIN` y `API_CLIENT` tienen acceso. Cada
operación queda registrada en la auditoría administrativa.

Este endpoint devuelve y administra únicamente destinatarios externos. Los
contactos asociados a una cuenta tienen `user_id` y se administran mediante
`/api/users`, con un solo interruptor que habilita cortes y restablecimientos.
Las cuentas revocadas no reciben mensajes.

Los intentos asociados a usuarios se consultan en
`GET /api/users/{id}/whatsapp-deliveries`; las pruebas se envían con
`POST /api/users/{id}/whatsapp-test`.

Los destinatarios son celulares peruanos. La API acepta el número local, con
`+51` o con separadores; por ejemplo `987654321`, `987 654 321` y
`+51 987-654-321` se almacenan siempre como `51987654321`. No se admite el
mismo número dos veces dentro del mismo alcance.

`WHATSAPP_PHONE` se conserva como respaldo únicamente mientras la tabla no
tenga ningún destinatario. Desde el primer registro, las preferencias de la
base de datos son autoritativas, incluso si todos los destinatarios están
desactivados.

Las credenciales de Meta, la URL y el número emisor continúan configurándose
solo en el backend. El dashboard nunca recibe esos secretos.
