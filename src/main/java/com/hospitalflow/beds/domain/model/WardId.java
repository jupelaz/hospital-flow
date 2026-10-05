package com.hospitalflow.beds.domain.model;

import java.util.Objects;

/** Unidad de hospitalización (planta / servicio). */
public record WardId(String value) {
    public WardId {
        Objects.requireNonNull(value, "wardId");
        if (value.isBlank()) throw new IllegalArgumentException("wardId vacío");
    }
}
