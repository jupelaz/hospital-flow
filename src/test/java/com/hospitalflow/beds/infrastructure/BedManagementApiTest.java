package com.hospitalflow.beds.infrastructure;

import com.hospitalflow.beds.infrastructure.messaging.outbox.OutboxEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de extremo a extremo con H2 (sin Kafka): REST -> caso de uso -> JPA + outbox. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BedManagementApiTest {

    @Autowired MockMvc mvc;
    @Autowired OutboxEventJpaRepository outbox;

    private static String admission(String patient, String ward) {
        return "{\"patientId\":\"" + patient + "\",\"wardId\":\"" + ward + "\"}";
    }

    @Test
    void admission_assigns_bed_and_writes_outbox_event() throws Exception {
        mvc.perform(post("/api/admissions")
                        .header("X-Correlation-Id", "test-corr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(admission("P-1", "UCI")))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-corr-1"))
                .andExpect(jsonPath("$.bedId").value("UCI-01"));

        assertThat(outbox.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getEventType()).isEqualTo("BedAssigned");
            assertThat(e.getCorrelationId()).isEqualTo("test-corr-1");
            assertThat(e.getPublishedAt()).isNull();
        });
    }

    @Test
    void full_ward_returns_409_problem_detail() throws Exception {
        for (String p : new String[]{"P-1", "P-2"}) {
            mvc.perform(post("/api/admissions").contentType(MediaType.APPLICATION_JSON).content(admission(p, "UCI")))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/api/admissions").contentType(MediaType.APPLICATION_JSON).content(admission("P-3", "UCI")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No hay camas disponibles en la unidad UCI"));
    }

    @Test
    void release_then_clean_makes_bed_available_again() throws Exception {
        mvc.perform(post("/api/admissions").contentType(MediaType.APPLICATION_JSON).content(admission("P-1", "MED-INT")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/beds/MI-101/release")).andExpect(status().isNoContent());
        mvc.perform(post("/api/beds/MI-101/cleaned")).andExpect(status().isNoContent());

        mvc.perform(get("/api/wards/MED-INT/beds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bedId").value("MI-101"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void invalid_request_is_rejected() throws Exception {
        mvc.perform(post("/api/admissions").contentType(MediaType.APPLICATION_JSON).content("{\"wardId\":\"UCI\"}"))
                .andExpect(status().isBadRequest());
    }
}
