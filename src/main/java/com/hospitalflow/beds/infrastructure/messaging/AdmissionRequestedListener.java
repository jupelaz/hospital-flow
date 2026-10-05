package com.hospitalflow.beds.infrastructure.messaging;

import com.hospitalflow.beds.application.port.in.AssignBedCommand;
import com.hospitalflow.beds.application.port.in.HandleAdmissionUseCase;
import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;
import com.hospitalflow.beds.infrastructure.messaging.contract.AdmissionContractReader;
import com.hospitalflow.beds.infrastructure.messaging.contract.AdmissionRequested;
import com.hospitalflow.beds.infrastructure.observability.Correlation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
class AdmissionRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(AdmissionRequestedListener.class);

    private final AdmissionContractReader reader;
    private final HandleAdmissionUseCase handleAdmission;

    AdmissionRequestedListener(AdmissionContractReader reader, HandleAdmissionUseCase handleAdmission) {
        this.reader = reader;
        this.handleAdmission = handleAdmission;
    }

    @KafkaListener(topics = "${app.kafka.topics.admissions}")
    void onMessage(ConsumerRecord<String, String> record) {
        String messageId = header(record, "messageId")
                .orElse(record.topic() + "-" + record.partition() + "-" + record.offset());
        header(record, Correlation.KAFKA_HEADER).ifPresent(id -> MDC.put(Correlation.MDC_KEY, id));
        try {
            int version = header(record, "schemaVersion").map(Integer::parseInt).orElse(1);
            AdmissionRequested admission = reader.read(record.value(), version, messageId);
            log.info("Admisión {} recibida (contrato v{}) para la unidad {}",
                    admission.admissionId(), version, admission.wardCode());
            handleAdmission.handle(
                    admission.admissionId(),
                    new AssignBedCommand(new PatientId(admission.patientId()), new WardId(admission.wardCode())));
        } finally {
            MDC.remove(Correlation.MDC_KEY);
        }
    }

    private static Optional<String> header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        return h == null ? Optional.empty() : Optional.of(new String(h.value(), StandardCharsets.UTF_8));
    }
}
