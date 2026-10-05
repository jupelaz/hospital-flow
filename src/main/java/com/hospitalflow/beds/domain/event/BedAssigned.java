package com.hospitalflow.beds.domain.event;

import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;

import java.time.Instant;

public record BedAssigned(BedId bedId, WardId wardId, PatientId patientId, Instant occurredAt) implements DomainEvent {}
