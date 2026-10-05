package com.hospitalflow.beds.domain.exception;

import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.BedStatus;

public class InvalidBedTransitionException extends DomainException {
    public InvalidBedTransitionException(BedId bedId, BedStatus from, BedStatus to) {
        super("Transición no permitida para la cama " + bedId.value() + ": " + from + " -> " + to);
    }
}
