package com.hospitalflow.beds.infrastructure.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalflow.beds.application.port.out.DomainEventPublisher;
import com.hospitalflow.beds.domain.event.BedAssigned;
import com.hospitalflow.beds.domain.event.BedBecameAvailable;
import com.hospitalflow.beds.domain.event.BedReleased;
import com.hospitalflow.beds.domain.event.DomainEvent;
import com.hospitalflow.beds.infrastructure.messaging.contract.BedEventMessage;
import com.hospitalflow.beds.infrastructure.observability.Correlation;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Escribe los eventos en la tabla outbox, dentro de la transacción del caso de uso. */
@Component
class OutboxDomainEventPublisher implements DomainEventPublisher {

    private final OutboxEventJpaRepository outbox;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final String topic;

    OutboxDomainEventPublisher(OutboxEventJpaRepository outbox, ObjectMapper mapper, Clock clock,
                               @Value("${app.kafka.topics.bed-events}") String topic) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.clock = clock;
        this.topic = topic;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        String correlationId = MDC.get(Correlation.MDC_KEY);
        for (DomainEvent event : events) {
            BedEventMessage message = toMessage(event);
            outbox.save(new OutboxEventEntity(
                    UUID.fromString(message.eventId()),
                    event.bedId().value(),
                    topic,
                    message.eventType(),
                    BedEventMessage.SCHEMA_VERSION,
                    toJson(message),
                    correlationId,
                    clock.instant()));
        }
    }

    private static BedEventMessage toMessage(DomainEvent event) {
        String id = UUID.randomUUID().toString();
        String bed = event.bedId().value();
        String ward = event.wardId().value();
        // Java 17: instanceof con patrón (en Java 21 sería un switch exhaustivo sobre la interfaz sellada)
        if (event instanceof BedAssigned e) {
            return new BedEventMessage(id, "BedAssigned", bed, ward, e.patientId().value(), e.occurredAt());
        }
        if (event instanceof BedReleased e) {
            return new BedEventMessage(id, "BedReleased", bed, ward, e.previousPatientId().value(), e.occurredAt());
        }
        if (event instanceof BedBecameAvailable e) {
            return new BedEventMessage(id, "BedBecameAvailable", bed, ward, null, e.occurredAt());
        }
        throw new IllegalArgumentException("Evento no soportado: " + event.getClass().getSimpleName());
    }

    private String toJson(BedEventMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + message.eventId(), e);
        }
    }
}
