package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.exception.EnergyTransactionDomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class EnergyMarketExceptionHandler {

    @ExceptionHandler(EnergyOfferDomainException.class)
    ResponseEntity<ApiErrorResponse> handleDomain(EnergyOfferDomainException exception,
                                                  HttpServletRequest request) {
        HttpStatus status = switch (exception.getMessage()) {
            case "Offer was not found" -> HttpStatus.NOT_FOUND;
            case "Authenticated user is required" -> HttpStatus.UNAUTHORIZED;
            case "NORMAL_USER role is required" -> HttpStatus.FORBIDDEN;
            case "Only active offers can be modified",
                 "Only active offers can be cancelled",
                 "Offer changed or is no longer editable",
                 "Offer is no longer available",
                 "Offer is expired" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return response(status, exception.getMessage(), request);
    }

    @ExceptionHandler(EnergyTransactionDomainException.class)
    ResponseEntity<ApiErrorResponse> handleTransaction(EnergyTransactionDomainException exception,
                                                       HttpServletRequest request) {
        HttpStatus status = switch (exception.getMessage()) {
            case "Transaction was not found" -> HttpStatus.NOT_FOUND;
            case "Idempotency key was used for another offer",
                 "Idempotency conflict", "Offer is no longer available",
                 "Offer is expired" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return response(status, exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Request data is invalid", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed", request);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message,
                                                      HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(),
                request.getHeader("X-Correlation-ID")));
    }

    public record ApiErrorResponse(Instant timestamp, int status, String error, String message,
                                   String path, String correlationId) {
    }
}
