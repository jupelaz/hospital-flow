package com.hospitalflow.beds.application.service;

import com.hospitalflow.beds.application.port.in.AssignBedCommand;
import com.hospitalflow.beds.application.port.in.AssignBedUseCase;
import com.hospitalflow.beds.application.port.in.MarkBedCleanedUseCase;
import com.hospitalflow.beds.application.port.in.QueryWardBedsUseCase;
import com.hospitalflow.beds.application.port.in.ReleaseBedUseCase;
import com.hospitalflow.beds.application.port.out.BedRepository;
import com.hospitalflow.beds.application.port.out.DomainEventPublisher;
import com.hospitalflow.beds.domain.exception.BedNotFoundException;
import com.hospitalflow.beds.domain.exception.NoBedAvailableInWardException;
import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.WardId;

import java.time.Clock;
import java.util.List;
import java.util.function.Consumer;

/**
 * Casos de uso de gestión de camas. Sin dependencias de framework:
 * la transaccionalidad la añade la infraestructura con un decorador.
 */
public class BedManagementService
        implements AssignBedUseCase, ReleaseBedUseCase, MarkBedCleanedUseCase, QueryWardBedsUseCase {

    private final BedRepository beds;
    private final DomainEventPublisher events;
    private final Clock clock;

    public BedManagementService(BedRepository beds, DomainEventPublisher events, Clock clock) {
        this.beds = beds;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public BedId assign(AssignBedCommand command) {
        Bed bed = beds.findFirstAvailableInWard(command.wardId())
                .orElseThrow(() -> new NoBedAvailableInWardException(command.wardId()));
        bed.assignTo(command.patientId(), clock.instant());
        persist(bed);
        return bed.id();
    }

    @Override
    public void release(BedId bedId) {
        mutate(bedId, bed -> bed.release(clock.instant()));
    }

    @Override
    public void markCleaned(BedId bedId) {
        mutate(bedId, bed -> bed.markCleaned(clock.instant()));
    }

    @Override
    public List<Bed> bedsOf(WardId wardId) {
        return beds.findByWard(wardId);
    }

    private void mutate(BedId bedId, Consumer<Bed> change) {
        Bed bed = beds.findById(bedId).orElseThrow(() -> new BedNotFoundException(bedId));
        change.accept(bed);
        persist(bed);
    }

    private void persist(Bed bed) {
        beds.save(bed);
        events.publish(bed.pullEvents());
    }
}
