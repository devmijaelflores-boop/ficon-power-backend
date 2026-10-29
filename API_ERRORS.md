# Contrato de errores de la API

Todas las respuestas de error generadas por controladores y por Spring
Security usan esta estructura:

```json
{
  "code": "validation_error",
  "message": "Revisa los datos indicados.",
  "fieldErrors": [
    {
      "field": "password",
      "message": "La contraseña debe tener entre 8 y 120 caracteres."
    }
  ],
  "traceId": "7be36b9d-87c7-4b54-a617-c45295fa60e6"
}
```

- `code`: identificador estable para que el cliente clasifique el fallo.
- `message`: explicación segura para el usuario.
- `fieldErrors`: validaciones asociadas a campos; queda vacío en otros casos.
- `traceId`: referencia que permite encontrar el error en logs.

El header `X-Trace-Id` contiene la misma referencia. Si el cliente proporciona
un `X-Request-Id` válido, se conserva; en caso contrario se genera un UUID.
Los detalles de excepciones inesperadas se escriben únicamente en el log del
backend junto con esa referencia.
