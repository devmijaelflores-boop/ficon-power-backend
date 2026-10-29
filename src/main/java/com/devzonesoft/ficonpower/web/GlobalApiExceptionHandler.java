package com.devzonesoft.ficonpower.web;

import com.devzonesoft.ficonpower.web.ApiErrorResponse.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "Revisa los datos indicados.",
                fields,
                request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> status(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = exception.getReason();
        if (message == null || message.isBlank()) message = defaultMessage(status);
        return response(status, code(status), message, List.of(), request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    ResponseEntity<ApiErrorResponse> malformedRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "invalid_request",
                "La solicitud contiene datos inválidos.",
                List.of(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> conflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "conflict",
                "No se pudo guardar porque existe un registro relacionado.",
                List.of(),
                request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> unexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = ApiErrorResponse.of(
                "internal_error",
                "El servidor no pudo completar la operación. Inténtalo nuevamente.",
                List.of(),
                request);
        logger.error("Unhandled API error traceId={} path={}",
                body.traceId(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, body);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            List<ApiFieldError> fields,
            HttpServletRequest request
    ) {
        return response(status, ApiErrorResponse.of(code, message, fields, request));
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, ApiErrorResponse body) {
        return ResponseEntity.status(status)
                .header("X-Trace-Id", body.traceId())
                .body(body);
    }

    private ApiFieldError fieldError(FieldError error) {
        String field = error.getField();
        String label = fieldLabel(field);
        String message = switch (error.getCode() == null ? "" : error.getCode()) {
            case "NotBlank", "NotNull" -> label + " es obligatorio.";
            case "Size" -> sizeMessage(field, label);
            case "Email" -> "Ingresa un correo válido.";
            default -> "Revisa " + label.toLowerCase() + ".";
        };
        return new ApiFieldError(field, message);
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "username" -> "El usuario";
            case "password" -> "La contraseña";
            case "displayName" -> "El nombre visible";
            case "deviceName" -> "El nombre del dispositivo";
            case "appVersion" -> "La versión de la aplicación";
            case "role" -> "El rol";
            case "name" -> "El nombre";
            case "district" -> "El distrito";
            case "province" -> "La provincia";
            case "siteId" -> "La sede";
            case "token" -> "El token";
            case "deviceToken" -> "El token del dispositivo";
            case "phone" -> "El número de WhatsApp";
            case "whatsappPhone" -> "El número de WhatsApp";
            case "email" -> "El correo";
            case "label" -> "El nombre del destinatario";
            case "platform" -> "La plataforma";
            case "refreshToken" -> "La sesión";
            default -> "El campo " + field;
        };
    }

    private String sizeMessage(String field, String label) {
        return switch (field) {
            case "password" -> "La contraseña debe tener entre 8 y 120 caracteres.";
            case "username" -> "El usuario no puede superar los 60 caracteres.";
            case "displayName", "deviceName" -> label + " no puede superar los 120 caracteres.";
            case "name" -> "El nombre no puede superar los 150 caracteres.";
            case "district", "province" -> label + " no puede superar los 100 caracteres.";
            case "platform" -> "La plataforma no puede superar los 30 caracteres.";
            case "appVersion" -> "La versión de la aplicación no puede superar los 40 caracteres.";
            case "email" -> "El correo no puede superar los 254 caracteres.";
            case "whatsappPhone" -> "El número de WhatsApp no es válido.";
            case "token", "refreshToken", "deviceToken" -> label + " no es válido.";
            default -> label + " no tiene una longitud válida.";
        };
    }

    private String code(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "invalid_request";
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            case NOT_FOUND -> "not_found";
            case CONFLICT -> "conflict";
            case TOO_MANY_REQUESTS -> "too_many_requests";
            default -> status.is5xxServerError() ? "internal_error" : "request_failed";
        };
    }

    private String defaultMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "La solicitud contiene datos inválidos.";
            case UNAUTHORIZED -> "La sesión no es válida. Vuelve a iniciar sesión.";
            case FORBIDDEN -> "No tienes permisos para realizar esta acción.";
            case NOT_FOUND -> "No se encontró el recurso solicitado.";
            case CONFLICT -> "No se pudo completar porque existe un conflicto.";
            case TOO_MANY_REQUESTS -> "Demasiados intentos. Espera unos minutos.";
            default -> "No se pudo completar la operación.";
        };
    }
}
