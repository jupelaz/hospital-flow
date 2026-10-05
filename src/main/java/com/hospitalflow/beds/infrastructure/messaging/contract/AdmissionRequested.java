package com.hospitalflow.beds.infrastructure.messaging.contract;

import java.time.Instant;

/**
 * Forma canónica (v2) del evento de admisión que publica el sistema de admisiones (HIS/ADT).
 * Los mensajes antiguos (v1) se convierten a esta forma con {@link AdmissionContractReader}.
 */
public record AdmissionRequested(String admissionId, String patientId, String wardCode, Instant requestedAt) {}
