package com.hospitalflow.beds.application;

import com.hospitalflow.beds.application.port.in.AssignBedCommand;
import com.hospitalflow.beds.application.service.AdmissionHandler;
import com.hospitalflow.beds.application.service.BedManagementService;
import com.hospitalflow.beds.domain.event.BedAssigned;
import com.hospitalflow.beds.domain.event.BedReleased;
import com.hospitalflow.beds.domain.exception.BedNotFoundException;
import com.hospitalflow.beds.domain.exception.NoBedAvailableInWardException;
import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.BedStatus;
import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BedManagementServiceTest {

    private static final WardId UCI = new WardId("UCI");

    private final InMemoryAdapters.Beds beds = new InMemoryAdapters.Beds();
    private final InMemoryAdapters.Events events = new InMemoryAdapters.Events();
    private final InMemoryAdapters.Processed processed = new InMemoryAdapters.Processed();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T11:30:00Z"), ZoneOffset.UTC);

    private BedManagementService service;
    private AdmissionHandler admissions;

    @BeforeEach
    void setUp() {
        beds.add(Bed.newAvailable(new BedId("UCI-02"), UCI));
        beds.add(Bed.newAvailable(new BedId("UCI-01"), UCI));
        service = new BedManagementService(beds, events, clock);
        admissions = new AdmissionHandler(processed, service);
    }

    @Test
    void assigns_first_available_bed_of_the_ward_and_publishes_event() {
        BedId assigned = service.assign(new AssignBedCommand(new PatientId("P-1"), UCI));

        assertThat(assigned).isEqualTo(new BedId("UCI-01"));
        assertThat(beds.findById(assigned).orElseThrow().status()).isEqualTo(BedStatus.OCCUPIED);
        assertThat(events.published).singleElement().isInstanceOf(BedAssigned.class);
    }

    @Test
    void fails_when_ward_is_full() {
        service.assign(new AssignBedCommand(new PatientId("P-1"), UCI));
        service.assign(new AssignBedCommand(new PatientId("P-2"), UCI));

        assertThatThrownBy(() -> service.assign(new AssignBedCommand(new PatientId("P-3"), UCI)))
                .isInstanceOf(NoBedAvailableInWardException.class);
    }

    @Test
    void releasing_publishes_event_and_sends_bed_to_cleaning() {
        BedId bed = service.assign(new AssignBedCommand(new PatientId("P-1"), UCI));

        service.release(bed);

        assertThat(beds.findById(bed).orElseThrow().status()).isEqualTo(BedStatus.CLEANING);
        assertThat(events.published).last().isInstanceOf(BedReleased.class);
    }

    @Test
    void unknown_bed_is_reported() {
        assertThatThrownBy(() -> service.release(new BedId("NOPE"))).isInstanceOf(BedNotFoundException.class);
    }

    @Test
    void duplicated_admission_message_assigns_only_one_bed() {
        var command = new AssignBedCommand(new PatientId("P-1"), UCI);

        admissions.handle("ADM-42", command);
        admissions.handle("ADM-42", command); // reentrega del broker

        assertThat(service.bedsOf(UCI)).filteredOn(b -> b.status() == BedStatus.OCCUPIED).hasSize(1);
        assertThat(events.published).hasSize(1);
    }
}
