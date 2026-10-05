package com.hospitalflow.beds.application.port.in;

import com.hospitalflow.beds.domain.model.BedId;

public interface MarkBedCleanedUseCase {
    void markCleaned(BedId bedId);
}
