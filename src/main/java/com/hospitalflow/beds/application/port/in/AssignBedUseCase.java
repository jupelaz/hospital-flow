package com.hospitalflow.beds.application.port.in;

import com.hospitalflow.beds.domain.model.BedId;

public interface AssignBedUseCase {
    BedId assign(AssignBedCommand command);
}
