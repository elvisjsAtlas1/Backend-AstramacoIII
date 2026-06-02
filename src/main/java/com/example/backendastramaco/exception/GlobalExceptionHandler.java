package com.example.backendastramaco.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Constantes
    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String NOT_FOUND = "Not Found";

    // ✅ AGREGAR ESTE MANEJADOR (es el que falta)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();

        // Obtener el primer error de validación
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Error de validación en los datos de entrada");

        response.put(TIMESTAMP, LocalDateTime.now());
        response.put(MESSAGE, errorMessage);
        response.put(STATUS, HttpStatus.BAD_REQUEST.value());
        response.put(ERROR, BAD_REQUEST);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, String> response = new HashMap<>();

        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            response.put(ERROR, "El registro o nombre de usuario ya existe en el sistema.");
        } else {
            response.put(ERROR, "Violación de integridad de datos en la base de datos.");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP, LocalDateTime.now());
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.BAD_REQUEST.value());
        response.put(ERROR, BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP, LocalDateTime.now());
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.BAD_REQUEST.value());
        response.put(ERROR, BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP, LocalDateTime.now());
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.NOT_FOUND.value());
        response.put(ERROR, NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}