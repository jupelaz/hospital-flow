package com.hospitalflow.beds.application.port.in;

/**
 * Procesa una admisión llegada por mensajería. Debe ser idempotente:
 * el broker garantiza "al menos una vez", así que el mismo mensaje puede llegar repetido.
 */
public interface HandleAdmissionUseCase {
    void handle(String messageId, AssignBedCommand command);
}
