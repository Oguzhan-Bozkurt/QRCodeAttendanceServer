package com.example.server.common;

import com.example.server.user.DuplicateResourceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a
                ));
        return Map.of(
                "ts", Instant.now().toString(),
                "status", "error",
                "message", "Validation failed",
                "errors", errors
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateResourceException.class)
    public Map<String, Object> handleDuplicate(DuplicateResourceException ex) {
        return Map.of(
                "ts", Instant.now().toString(),
                "status", "error",
                "message", ex.getMessage()
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Map<String, Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        return Map.of(
                "ts", Instant.now().toString(),
                "status", "error",
                "message", "Veri bütünlüğü hatası (muhtemelen tekil alan çakışması)"
        );
    }
}
