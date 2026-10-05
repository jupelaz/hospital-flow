package com.hospitalflow.beds.domain.model;

import com.hospitalflow.beds.domain.event.BedAssigned;
import com.hospitalflow.beds.domain.event.BedBecameAvailable;
import com.hospitalflow.beds.domain.event.BedReleased;
import com.hospitalflow.beds.domain.event.DomainEvent;
import com.hospitalflow.beds.domain.exception.BedNotAvailableException;
import com.hospitalflow.beds.domain.exception.InvalidBedTransitionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado Cama. Toda transición de estado pasa por aquí y genera un evento de dominio.
 * La versión permite control de concurrencia optimista (dos admisiones a la vez
 * no pueden quedarse con la misma cama).
 */
public final class Bed {

    private final BedId id;
    private final WardId ward;
    private BedStatus status;
    private PatientId occupant;
    private final long version;
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    private Bed(BedId id, WardId ward, BedStatus status, PatientId occupant, long version) {
        this.id = Objects.requireNonNull(id);
        this.ward = Objects.requireNonNull(ward);
        this.status = Objects.requireNonNull(status);
        this.occupant = occupant;
        this.version = version;
        if (status == BedStatus.OCCUPIED && occupant == null) {
            throw new IllegalStateException("Cama ocupada sin paciente: " + id.value());
        }
    }

    public static Bed newAvailable(BedId id, WardId ward) {
        return new Bed(id, ward, BedStatus.AVAILABLE, null, 0L);
    }

    /** Reconstrucción desde persistencia: no genera eventos. */
    public static Bed rehydrate(BedId id, WardId ward, BedStatus status, PatientId occupant, long version) {
        return new Bed(id, ward, status, occupant, version);
    }

    public void assignTo(PatientId patient, Instant at) {
        Objects.requireNonNull(patient);
        if (status != BedStatus.AVAILABLE) {
            throw new BedNotAvailableException(id, status);
        }
        status = BedStatus.OCCUPIED;
        occupant = patient;
        pendingEvents.add(new BedAssigned(id, ward, patient, at));
    }

    /** Alta o traslado: la cama pasa a limpieza, no directamente a disponible. */
    public void release(Instant at) {
        if (status != BedStatus.OCCUPIED) {
            throw new InvalidBedTransitionException(id, status, BedStatus.CLEANING);
        }
        PatientId previous = occupant;
        occupant = null;
        status = BedStatus.CLEANING;
        pendingEvents.add(new BedReleased(id, ward, previous, at));
    }

    public void markCleaned(Instant at) {
        if (status != BedStatus.CLEANING) {
            throw new InvalidBedTransitionException(id, status, BedStatus.AVAILABLE);
        }
        status = BedStatus.AVAILABLE;
        pendingEvents.add(new BedBecameAvailable(id, ward, at));
    }

    /** Devuelve y vacía los eventos pendientes (se publican en la misma transacción vía outbox). */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public BedId id() { return id; }
    public WardId ward() { return ward; }
    public BedStatus status() { return status; }
    public Optional<PatientId> occupant() { return Optional.ofNullable(occupant); }
    public long version() { return version; }
}
