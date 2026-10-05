package com.hospitalflow.beds.domain.event;

import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.WardId;

import java.time.Instant;

public record BedBecameAvailable(BedId bedId, WardId wardId, Instant occurredAt) implements DomainEvent {}
