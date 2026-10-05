package com.hospitalflow.beds.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;

public record AdmissionRequest(@NotBlank String patientId, @NotBlank String wardId) {}
