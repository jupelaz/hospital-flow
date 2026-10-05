package com.hospitalflow.beds.domain.exception;

import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.BedStatus;

public class BedNotAvailableException extends DomainException {
    public BedNotAvailableException(BedId bedId, BedStatus status) {
        super("La cama " + bedId.value() + " no está disponible (estado " + status + ")");
    }
}
