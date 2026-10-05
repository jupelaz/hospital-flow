package com.hospitalflow.beds.domain.exception;

import com.hospitalflow.beds.domain.model.WardId;

public class NoBedAvailableInWardException extends DomainException {
    public NoBedAvailableInWardException(WardId ward) {
        super("No hay camas disponibles en la unidad " + ward.value());
    }
}
