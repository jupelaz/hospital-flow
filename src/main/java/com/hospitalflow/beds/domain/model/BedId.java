package com.hospitalflow.beds.domain.model;

import java.util.Objects;

public record BedId(String value) {
    public BedId {
        Objects.requireNonNull(value, "bedId");
        if (value.isBlank()) throw new IllegalArgumentException("bedId vacío");
    }
}
