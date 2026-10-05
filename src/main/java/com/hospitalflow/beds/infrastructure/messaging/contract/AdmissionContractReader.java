package com.hospitalflow.beds.infrastructure.messaging.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Evolución de contratos entre sistemas ("upcasting"): el productor puede seguir enviando v1
 * mientras migra; aquí todo se normaliza a v2 antes de llegar a la aplicación.
 *
 * v1: {"patient": "...", "ward": "...", "timestamp": "..."}            (sin id de admisión)
 * v2: {"admissionId": "...", "patientId": "...", "wardCode": "...", "requestedAt": "..."}
 */
@Component
public class AdmissionContractReader {

    public static final int CURRENT_VERSION = 2;

    private final ObjectMapper mapper;

    public AdmissionContractReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public AdmissionRequested read(String json, int schemaVersion, String messageId) {
        JsonNode node = parse(json);
        return switch (schemaVersion) {
            case 1 -> new AdmissionRequested(
                    messageId, // v1 no traía id de admisión: usamos el id del mensaje
                    required(node, "patient"),
                    required(node, "ward"),
                    instant(node, "timestamp"));
            case 2 -> new AdmissionRequested(
                    required(node, "admissionId"),
                    required(node, "patientId"),
                    required(node, "wardCode"),
                    instant(node, "requestedAt"));
            default -> throw new UnsupportedSchemaVersionException(schemaVersion);
        };
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("JSON no válido", e);
        }
    }

    private static String required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new InvalidMessageException("Falta el campo obligatorio '" + field + "'");
        }
        return value.asText();
    }

    private static Instant instant(JsonNode node, String field) {
        try {
            return Instant.parse(required(node, field));
        } catch (DateTimeParseException e) {
            throw new InvalidMessageException("Fecha no válida en '" + field + "'", e);
        }
    }
}
