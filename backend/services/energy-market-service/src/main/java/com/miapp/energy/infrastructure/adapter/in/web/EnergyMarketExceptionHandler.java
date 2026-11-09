package com.miapp.energy.infrastructure.adapter.in.web;

import com.miapp.energy.domain.exception.EnergyOfferDomainException;
import com.miapp.energy.domain.exception.EnergyTransactionDomainException;
import com.miapp.energy.application.service.MeterException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class EnergyMarketExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EnergyMarketExceptionHandler.class);

    @ExceptionHandler(MeterException.class)
    ResponseEntity<ApiErrorResponse> handleMeter(MeterException exception, HttpServletRequest request) {
        HttpStatus status;
        switch (exception.kind()) {
            case UNAUTHORIZED:
                status = HttpStatus.UNAUTHORIZED;
                break;
            case FORBIDDEN:
                status = HttpStatus.FORBIDDEN;
                break;
            case NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case CONFLICT:
                status = HttpStatus.CONFLICT;
                break;
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return response(status, status.getReasonPhrase(), request);
    }

    @ExceptionHandler(EnergyOfferDomainException.class)
    ResponseEntity<ApiErrorResponse> handleDomain(EnergyOfferDomainException exception,
                                                  HttpServletRequest request) {
            HttpStatus status;
            switch (exception.getMessage()) {
                case "Offer was not found":
                    status = HttpStatus.NOT_FOUND;
                    break;
                case "Authenticated user is required":
                    status = HttpStatus.UNAUTHORIZED;
                    break;
                case "NORMAL_USER role is required":
                    status = HttpStatus.FORBIDDEN;
                    break;
                case "Only active offers can be modified":
                case "Only active offers can be cancelled":
                case "Offer changed or is no longer editable":
                case "Offer is no longer available":
                case "Offer is expired":
                    status = HttpStatus.CONFLICT;
                    break;
                default:
                    status = HttpStatus.BAD_REQUEST;
            }
            return response(status, exception.getMessage(), request);
    }

    @ExceptionHandler(EnergyTransactionDomainException.class)
    ResponseEntity<ApiErrorResponse> handleTransaction(EnergyTransactionDomainException exception,
                                                       HttpServletRequest request) {
        HttpStatus status;
        switch (exception.getMessage()) {
            case "Transaction was not found":
                status = HttpStatus.NOT_FOUND;
                break;
            case "Idempotency key was used for another offer":
            case "Idempotency conflict":
            case "Offer is no longer available":
            case "Offer is expired":
                status = HttpStatus.CONFLICT;
                break;
            default:
                status = HttpStatus.BAD_REQUEST;
        }
        return response(status, exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Request data is invalid", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOG.atError().addKeyValue("event", "market_request_failed")
                .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                .log("Market request failed");
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
