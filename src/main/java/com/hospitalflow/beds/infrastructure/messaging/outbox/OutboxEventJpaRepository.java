package com.hospitalflow.beds.infrastructure.messaging.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED (timeout -2 en Hibernate): con varias réplicas,
     * cada una coge un lote distinto y no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

    long countByPublishedAtIsNull();
}
