package com.hospitalflow.beds.infrastructure.messaging.contract;

/** Versión de contrato desconocida: no se reintenta, va directa a la DLT. */
public class UnsupportedSchemaVersionException extends RuntimeException {
    public UnsupportedSchemaVersionException(int version) {
        super("Versión de esquema no soportada: " + version);
    }
}
