package com.hospitalflow.beds.domain.exception;

/** Violación de una regla de negocio: no tiene sentido reintentarla. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
