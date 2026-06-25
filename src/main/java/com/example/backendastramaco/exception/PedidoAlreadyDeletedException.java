package com.example.backendastramaco.exception;

public class PedidoAlreadyDeletedException extends RuntimeException {
    public PedidoAlreadyDeletedException(String message) {
        super(message);
    }
}