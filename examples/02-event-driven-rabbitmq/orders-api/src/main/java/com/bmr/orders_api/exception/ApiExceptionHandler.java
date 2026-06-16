package com.bmr.orders_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error(
                "Data integrity violation while creating order. path={}",
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        "DATA_INTEGRITY_VIOLATION",
                        "The order could not be created because it violates a data constraint.",
                        HttpStatus.CONFLICT.value(),
                        request.getRequestURI(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseUnavailable(
            DataAccessResourceFailureException ex,
            HttpServletRequest request
    ) {
        log.error(
                "Database unavailable while creating order. path={}",
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        "DATABASE_UNAVAILABLE",
                        "The order could not be accepted because the database is unavailable.",
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        request.getRequestURI(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleGenericDataAccess(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected database error while creating order. path={}",
                request.getRequestURI(),
                ex
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        "DATABASE_ERROR",
                        "The order could not be created due to a persistence error.",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI(),
                        Instant.now()
                ));
    }
}