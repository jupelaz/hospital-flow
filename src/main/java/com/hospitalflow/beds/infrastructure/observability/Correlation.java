package com.hospitalflow.beds.infrastructure.observability;

/** Nombres compartidos para propagar el id de correlación HTTP -> BD (outbox) -> Kafka -> logs. */
public final class Correlation {
    public static final String HTTP_HEADER = "X-Correlation-Id";
    public static final String KAFKA_HEADER = "correlationId";
    public static final String MDC_KEY = "correlationId";

    private Correlation() {}
}
