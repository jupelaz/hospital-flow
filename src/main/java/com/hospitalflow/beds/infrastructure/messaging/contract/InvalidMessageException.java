package com.hospitalflow.beds.infrastructure.messaging.contract;

/** Mensaje mal formado: no se reintenta, va directo a la DLT. */
public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
