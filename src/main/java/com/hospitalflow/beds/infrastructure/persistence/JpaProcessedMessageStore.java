package com.hospitalflow.beds.infrastructure.persistence;

import com.hospitalflow.beds.application.port.out.ProcessedMessageStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Clock;

/**
 * Tabla de "inbox" para deduplicar. Si dos réplicas procesan el mismo mensaje a la vez,
 * la clave primaria hace fallar a una; su transacción se deshace y en el reintento
 * ya ve el mensaje como procesado.
 */
@Repository
class JpaProcessedMessageStore implements ProcessedMessageStore {

    private final EntityManager em;
    private final Clock clock;

    JpaProcessedMessageStore(EntityManager em, Clock clock) {
        this.em = em;
        this.clock = clock;
    }

    @Override
    public boolean markProcessed(String messageId) {
        if (em.find(ProcessedMessageEntity.class, messageId) != null) {
            return false;
        }
        em.persist(new ProcessedMessageEntity(messageId, clock.instant()));
        return true;
    }
}
