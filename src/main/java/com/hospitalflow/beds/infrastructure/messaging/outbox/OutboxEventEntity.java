package com.hospitalflow.beds.infrastructure.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;

    /** Clave de partición en Kafka: garantiza orden por cama. */
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(nullable = false)
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEventEntity() {}

    public OutboxEventEntity(UUID id, String aggregateId, String topic, String eventType, int schemaVersion,
                             String payload, String correlationId, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.payload = payload;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant at) { this.publishedAt = at; }

    public UUID getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getTopic() { return topic; }
    public String getEventType() { return eventType; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
