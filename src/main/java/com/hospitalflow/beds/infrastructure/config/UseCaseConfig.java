package com.hospitalflow.beds.infrastructure.config;

import com.hospitalflow.beds.application.port.out.BedRepository;
import com.hospitalflow.beds.application.port.out.DomainEventPublisher;
import com.hospitalflow.beds.application.port.out.ProcessedMessageStore;
import com.hospitalflow.beds.application.service.AdmissionHandler;
import com.hospitalflow.beds.application.service.BedManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

/** Composición: aquí se "enchufan" los adaptadores a los casos de uso puros. */
@Configuration
class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionalUseCases useCases(BedRepository beds,
                                   DomainEventPublisher events,
                                   ProcessedMessageStore processed,
                                   Clock clock,
                                   PlatformTransactionManager txManager) {
        var core = new BedManagementService(beds, events, clock);
        var admissions = new AdmissionHandler(processed, core);
        return new TransactionalUseCases(core, admissions, txManager);
    }
}
