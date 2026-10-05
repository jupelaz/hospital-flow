package com.hospitalflow.beds.domain;

import com.hospitalflow.beds.domain.event.BedAssigned;
import com.hospitalflow.beds.domain.event.BedBecameAvailable;
import com.hospitalflow.beds.domain.event.BedReleased;
import com.hospitalflow.beds.domain.exception.BedNotAvailableException;
import com.hospitalflow.beds.domain.exception.InvalidBedTransitionException;
import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.BedStatus;
import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BedTest {

    private static final Instant NOW = Instant.parse("2026-10-06T11:30:00Z");
    private final PatientId patient = new PatientId("P-1");

    private Bed bed() {
        return Bed.newAvailable(new BedId("UCI-01"), new WardId("UCI"));
    }

    @Test
    void assigning_an_available_bed_occupies_it_and_emits_event() {
        Bed bed = bed();

        bed.assignTo(patient, NOW);

        assertThat(bed.status()).isEqualTo(BedStatus.OCCUPIED);
        assertThat(bed.occupant()).contains(patient);
        assertThat(bed.pullEvents()).containsExactly(new BedAssigned(bed.id(), bed.ward(), patient, NOW));
    }

    @Test
    void an_occupied_bed_cannot_be_assigned_again() {
        Bed bed = bed();
        bed.assignTo(patient, NOW);

        assertThatThrownBy(() -> bed.assignTo(new PatientId("P-2"), NOW))
                .isInstanceOf(BedNotAvailableException.class);
    }

    @Test
    void full_cycle_occupied_cleaning_available() {
        Bed bed = bed();
        bed.assignTo(patient, NOW);
        bed.release(NOW);
        assertThat(bed.status()).isEqualTo(BedStatus.CLEANING);
        assertThat(bed.occupant()).isEmpty();

        bed.markCleaned(NOW);

        assertThat(bed.status()).isEqualTo(BedStatus.AVAILABLE);
        assertThat(bed.pullEvents()).hasExactlyElementsOfTypes(
                BedAssigned.class, BedReleased.class, BedBecameAvailable.class);
    }

    @Test
    void pulling_events_empties_the_queue() {
        Bed bed = bed();
        bed.assignTo(patient, NOW);
        bed.pullEvents();

        assertThat(bed.pullEvents()).isEmpty();
    }

    @Test
    void a_free_bed_cannot_be_released() {
        assertThatThrownBy(() -> bed().release(NOW)).isInstanceOf(InvalidBedTransitionException.class);
    }

    @Test
    void rehydrated_occupied_bed_requires_a_patient() {
        assertThatThrownBy(() -> Bed.rehydrate(new BedId("X"), new WardId("W"), BedStatus.OCCUPIED, null, 3))
                .isInstanceOf(IllegalStateException.class);
    }
}
