package com.hospitalflow.beds.infrastructure.messaging.outbox;

import com.hospitalflow.beds.infrastructure.observability.Correlation;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * Envía a Kafka lo pendiente en la outbox. Entrega "al menos una vez": si el proceso cae
 * después de enviar y antes de marcar, el evento se reenvía; por eso los consumidores
 * deduplican por eventId.
 */
@Component
@ConditionalOnProperty(name = "app.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;

    OutboxRelay(OutboxEventJpaRepository outbox, KafkaTemplate<String, String> kafka, Clock clock) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:500}")
    @Transactional
    public void relay() {
        for (OutboxEventEntity event : outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(toRecord(event)).get(10, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
            } catch (Exception e) {
                // Paramos el lote para no desordenar eventos de la misma cama; se reintenta en el siguiente ciclo.
                log.warn("No se pudo publicar el evento {} ({}); se reintentará", event.getId(), e.getMessage());
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static ProducerRecord<String, String> toRecord(OutboxEventEntity event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(event.getTopic(), event.getAggregateId(), event.getPayload());
        addHeader(record, "messageId", event.getId().toString());
        addHeader(record, "eventType", event.getEventType());
        addHeader(record, "schemaVersion", String.valueOf(event.getSchemaVersion()));
        if (event.getCorrelationId() != null) {
            addHeader(record, Correlation.KAFKA_HEADER, event.getCorrelationId());
        }
        return record;
    }

    private static void addHeader(ProducerRecord<String, String> record, String key, String value) {
        record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
