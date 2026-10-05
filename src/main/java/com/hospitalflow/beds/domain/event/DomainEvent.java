package com.hospitalflow.beds.domain.event;

import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.WardId;

import java.time.Instant;

public sealed interface DomainEvent permits BedAssigned, BedReleased, BedBecameAvailable {
    BedId bedId();
    WardId wardId();
    Instant occurredAt();
}
