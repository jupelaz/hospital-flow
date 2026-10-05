package com.hospitalflow.beds.infrastructure.config;

import com.hospitalflow.beds.domain.exception.DomainException;
import com.hospitalflow.beds.infrastructure.messaging.contract.InvalidMessageException;
import com.hospitalflow.beds.infrastructure.messaging.contract.UnsupportedSchemaVersionException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
class KafkaConfig {

    /**
     * Reintentos con backoff exponencial para errores transitorios (BD caída, conflicto de
     * concurrencia optimista...). Errores de negocio o de contrato van directos a la DLT:
     * reintentarlos no cambia el resultado.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        var backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                DomainException.class,
                UnsupportedSchemaVersionException.class,
                InvalidMessageException.class);
        return handler;
    }

    @Configuration
    @ConditionalOnProperty(name = "app.kafka.create-topics", havingValue = "true")
    static class Topics {

        /** Particionado por unidad/cama: orden garantizado por clave y escalado horizontal por partición. */
        @Bean
        NewTopic admissionsTopic(@Value("${app.kafka.topics.admissions}") String name) {
            return TopicBuilder.name(name).partitions(6).replicas(1).build();
        }

        @Bean
        NewTopic admissionsDlt(@Value("${app.kafka.topics.admissions}") String name) {
            return TopicBuilder.name(name + ".DLT").partitions(1).replicas(1).build();
        }

        @Bean
        NewTopic bedEventsTopic(@Value("${app.kafka.topics.bed-events}") String name) {
            return TopicBuilder.name(name).partitions(6).replicas(1).build();
        }
    }
}
