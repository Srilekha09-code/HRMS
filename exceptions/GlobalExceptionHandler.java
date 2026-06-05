package com.hrms.project.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ CUSTOM EXCEPTION CLASS (THIS IS ApiException)
    public static class ApiException extends RuntimeException {

        private final String error;
        private final HttpStatus status;

        public ApiException(String error, HttpStatus status, String message) {
            super(message);
            this.error = error;
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    // ✅ HANDLE YOUR CUSTOM EXCEPTION
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getError());
        body.put("message", ex.getMessage());
        body.put("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ✅ HANDLE GENERIC EXCEPTION
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "INTERNAL_ERROR");
        body.put("message", "Unexpected server error");
        body.put("timestamp", OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}