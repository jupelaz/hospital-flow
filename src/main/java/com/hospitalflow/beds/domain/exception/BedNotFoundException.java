package com.hospitalflow.beds.domain.exception;

import com.hospitalflow.beds.domain.model.BedId;

public class BedNotFoundException extends DomainException {
    public BedNotFoundException(BedId bedId) {
        super("Cama no encontrada: " + bedId.value());
    }
}
