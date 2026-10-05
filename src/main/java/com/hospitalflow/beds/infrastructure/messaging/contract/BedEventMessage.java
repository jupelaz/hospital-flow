package com.hospitalflow.beds.infrastructure.messaging.contract;

import java.time.Instant;

/**
 * Contrato público (v1) de los eventos de cama que consume el resto de servicios
 * (cuadro de mando de ocupación, limpieza, planificación de altas...).
 * Cambios compatibles (añadir campos opcionales) mantienen la versión; los incompatibles la suben.
 */
public record BedEventMessage(
        String eventId,
        String eventType,
        String bedId,
        String wardId,
        String patientId,
        Instant occurredAt) {

    public static final int SCHEMA_VERSION = 1;
}
