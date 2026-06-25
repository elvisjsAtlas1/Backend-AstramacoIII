package com.example.backendastramaco.exception;

public class TransportistaAlreadyDeletedException extends RuntimeException {
    public TransportistaAlreadyDeletedException(String message) {
        super(message);
    }
}