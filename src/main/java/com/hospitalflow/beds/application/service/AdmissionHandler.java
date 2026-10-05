package com.hospitalflow.beds.application.service;

import com.hospitalflow.beds.application.port.in.AssignBedCommand;
import com.hospitalflow.beds.application.port.in.AssignBedUseCase;
import com.hospitalflow.beds.application.port.in.HandleAdmissionUseCase;
import com.hospitalflow.beds.application.port.out.ProcessedMessageStore;

/**
 * Consumidor idempotente: marca el mensaje como procesado y asigna la cama
 * dentro de la misma transacción. Si algo falla, se deshace todo y el reintento
 * vuelve a empezar limpio.
 */
public class AdmissionHandler implements HandleAdmissionUseCase {

    private final ProcessedMessageStore processed;
    private final AssignBedUseCase assignBed;

    public AdmissionHandler(ProcessedMessageStore processed, AssignBedUseCase assignBed) {
        this.processed = processed;
        this.assignBed = assignBed;
    }

    @Override
    public void handle(String messageId, AssignBedCommand command) {
        if (!processed.markProcessed(messageId)) {
            return; // duplicado: ya se asignó cama para esta admisión
        }
        assignBed.assign(command);
    }
}
