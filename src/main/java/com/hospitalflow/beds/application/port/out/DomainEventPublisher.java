package com.hospitalflow.beds.application.port.out;

import com.hospitalflow.beds.domain.event.DomainEvent;

import java.util.List;

/**
 * Se implementa con el patrón outbox: los eventos se escriben en la misma transacción
 * que el cambio de estado y un proceso aparte los envía a Kafka.
 */
public interface DomainEventPublisher {
    void publish(List<DomainEvent> events);
}
