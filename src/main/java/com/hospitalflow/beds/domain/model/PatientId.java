package com.hospitalflow.beds.domain.model;

import java.util.Objects;

/**
 * Identificador seudonimizado del paciente. El servicio nunca guarda datos
 * clínicos ni identificativos: solo la referencia que da el sistema de admisión (HIS).
 */
public record PatientId(String value) {
    public PatientId {
        Objects.requireNonNull(value, "patientId");
        if (value.isBlank()) throw new IllegalArgumentException("patientId vacío");
    }
}
