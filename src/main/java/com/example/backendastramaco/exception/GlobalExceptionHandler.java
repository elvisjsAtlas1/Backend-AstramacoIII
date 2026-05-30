package com.example.backendastramaco.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // SonarQube aprueba centralizar excepciones aquí para limpieza de código
public class GlobalExceptionHandler {

    // 🔥 CORRECCIÓN: Atrapa los errores de llaves duplicadas de la DB y responde con un 400 Bad Request limpio
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, String> response = new HashMap<>();
        response.clear();

        // Evaluamos si el fallo es por un registro duplicado
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            response.put("error", "El registro o nombre de usuario ya existe en el sistema.");
        } else {
            response.put("error", "Violación de integridad de datos en la base de datos.");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Puedes tener aquí otros métodos para IllegalArgumentException, EntityNotFoundException, etc.
}