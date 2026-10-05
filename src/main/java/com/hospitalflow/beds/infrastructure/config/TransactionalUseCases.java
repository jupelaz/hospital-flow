package com.hospitalflow.beds.infrastructure.config;

import com.hospitalflow.beds.application.port.in.AssignBedCommand;
import com.hospitalflow.beds.application.port.in.AssignBedUseCase;
import com.hospitalflow.beds.application.port.in.HandleAdmissionUseCase;
import com.hospitalflow.beds.application.port.in.MarkBedCleanedUseCase;
import com.hospitalflow.beds.application.port.in.QueryWardBedsUseCase;
import com.hospitalflow.beds.application.port.in.ReleaseBedUseCase;
import com.hospitalflow.beds.application.service.AdmissionHandler;
import com.hospitalflow.beds.application.service.BedManagementService;
import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.WardId;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Decorador que abre una transacción por caso de uso. Así dominio y aplicación
 * no dependen de Spring y se testean con fakes en memoria.
 */
public class TransactionalUseCases implements AssignBedUseCase, ReleaseBedUseCase, MarkBedCleanedUseCase,
        QueryWardBedsUseCase, HandleAdmissionUseCase {

    private final BedManagementService core;
    private final AdmissionHandler admissions;
    private final TransactionTemplate tx;
    private final TransactionTemplate readOnlyTx;

    TransactionalUseCases(BedManagementService core, AdmissionHandler admissions, PlatformTransactionManager tm) {
        this.core = core;
        this.admissions = admissions;
        this.tx = new TransactionTemplate(tm);
        this.readOnlyTx = new TransactionTemplate(tm);
        this.readOnlyTx.setReadOnly(true);
    }

    @Override
    public BedId assign(AssignBedCommand command) {
        return tx.execute(status -> core.assign(command));
    }

    @Override
    public void release(BedId bedId) {
        tx.executeWithoutResult(status -> core.release(bedId));
    }

    @Override
    public void markCleaned(BedId bedId) {
        tx.executeWithoutResult(status -> core.markCleaned(bedId));
    }

    @Override
    public List<Bed> bedsOf(WardId wardId) {
        return readOnlyTx.execute(status -> core.bedsOf(wardId));
    }

    @Override
    public void handle(String messageId, AssignBedCommand command) {
        tx.executeWithoutResult(status -> admissions.handle(messageId, command));
    }
}
