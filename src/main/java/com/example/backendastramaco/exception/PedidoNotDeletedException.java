package com.example.backendastramaco.exception;

public class PedidoNotDeletedException extends RuntimeException {
    public PedidoNotDeletedException(String message) {
        super(message);
    }
}