package com.hospitalflow.beds.application.port.in;

import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;

import java.util.Objects;

public record AssignBedCommand(PatientId patientId, WardId wardId) {
    public AssignBedCommand {
        Objects.requireNonNull(patientId);
        Objects.requireNonNull(wardId);
    }
}
