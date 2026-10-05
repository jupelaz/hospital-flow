package com.hospitalflow.beds.application.port.in;

import com.hospitalflow.beds.domain.model.BedId;

public interface ReleaseBedUseCase {
    void release(BedId bedId);
}
