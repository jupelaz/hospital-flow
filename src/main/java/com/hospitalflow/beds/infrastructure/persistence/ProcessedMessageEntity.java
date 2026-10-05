package com.hospitalflow.beds.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessageEntity {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedMessageEntity() {}

    public ProcessedMessageEntity(String messageId, Instant processedAt) {
        this.messageId = messageId;
        this.processedAt = processedAt;
    }
}
