package com.hospitalflow.beds.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalflow.beds.infrastructure.messaging.contract.AdmissionContractReader;
import com.hospitalflow.beds.infrastructure.messaging.contract.AdmissionRequested;
import com.hospitalflow.beds.infrastructure.messaging.contract.InvalidMessageException;
import com.hospitalflow.beds.infrastructure.messaging.contract.UnsupportedSchemaVersionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdmissionContractReaderTest {

    private final AdmissionContractReader reader = new AdmissionContractReader(new ObjectMapper());

    @Test
    void reads_v2() {
        String json = """
                {"admissionId":"ADM-1","patientId":"P-1","wardCode":"UCI","requestedAt":"2026-10-06T11:30:00Z"}
                """;

        assertThat(reader.read(json, 2, "msg-1")).isEqualTo(
                new AdmissionRequested("ADM-1", "P-1", "UCI", Instant.parse("2026-10-06T11:30:00Z")));
    }

    @Test
    void upcasts_v1_using_message_id_as_admission_id() {
        String json = """
                {"patient":"P-1","ward":"UCI","timestamp":"2026-10-06T11:30:00Z"}
                """;

        AdmissionRequested result = reader.read(json, 1, "msg-7");

        assertThat(result.admissionId()).isEqualTo("msg-7");
        assertThat(result.wardCode()).isEqualTo("UCI");
    }

    @Test
    void rejects_unknown_versions() {
        assertThatThrownBy(() -> reader.read("{}", 3, "m")).isInstanceOf(UnsupportedSchemaVersionException.class);
    }

    @Test
    void rejects_missing_fields() {
        assertThatThrownBy(() -> reader.read("{\"patientId\":\"P-1\"}", 2, "m"))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("admissionId");
    }
}
