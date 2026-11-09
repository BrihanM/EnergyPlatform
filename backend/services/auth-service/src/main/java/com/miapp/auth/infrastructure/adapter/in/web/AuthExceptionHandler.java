package com.miapp.auth.infrastructure.adapter.in.web;

import com.miapp.auth.domain.exception.DuplicateEmailException;
import com.miapp.auth.domain.exception.DuplicateUsernameException;
import com.miapp.auth.domain.exception.InvalidRegistrationException;
import com.miapp.auth.domain.exception.InvalidCredentialsException;
import com.miapp.auth.domain.exception.InvalidSessionException;
import com.miapp.auth.domain.exception.RegistrationConflictException;
import com.miapp.auth.domain.exception.InvalidRoleOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Traduce errores funcionales sin exponer detalles técnicos o credenciales.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler({DuplicateUsernameException.class, DuplicateEmailException.class,
            RegistrationConflictException.class})
    ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException exception,
                                                     HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidRegistrationException.class, MethodArgumentNotValidException.class,
            IllegalArgumentException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<ApiErrorResponse> handleValidation(Exception exception,
                                                       HttpServletRequest request) {
        String message = exception instanceof InvalidRegistrationException
                ? exception.getMessage() : "Request data is invalid";
        return response(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidBody(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Bad Request",
                "Request data is invalid", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type",
                "Content-Type must be application/json", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidCredentials(HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Credenciales inválidas.", request);
    }

    @ExceptionHandler(InvalidRoleOperationException.class)
    ResponseEntity<ApiErrorResponse> handleRoleOperation(InvalidRoleOperationException exception,
                                                         HttpServletRequest request) {
        int status = ("Administrator role required".equals(exception.getMessage())
                || "Auditor role required".equals(exception.getMessage()))
                ? HttpStatus.FORBIDDEN.value() : HttpStatus.BAD_REQUEST.value();
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        return response(httpStatus, httpStatus == HttpStatus.FORBIDDEN ? "Forbidden" : "Bad Request",
                exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidSessionException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidSession(HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Sesión inválida o expirada.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> handlePersistence(HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Conflict",
                "The registration could not be completed", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected auth-service error for {} {}",
                request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "The request could not be completed", request);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String error,
                                                       String message, HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), error, message, request.getRequestURI(), correlationId));
    }
}
