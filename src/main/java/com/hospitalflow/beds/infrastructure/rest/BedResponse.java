package com.hospitalflow.beds.infrastructure.rest;

import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.PatientId;

public record BedResponse(String bedId, String wardId, String status, String patientId) {

    static BedResponse from(Bed bed) {
        return new BedResponse(
                bed.id().value(),
                bed.ward().value(),
                bed.status().name(),
                bed.occupant().map(PatientId::value).orElse(null));
    }
}
