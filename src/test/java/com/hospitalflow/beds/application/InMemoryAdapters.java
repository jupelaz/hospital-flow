package com.hospitalflow.beds.application;

import com.hospitalflow.beds.application.port.out.BedRepository;
import com.hospitalflow.beds.application.port.out.DomainEventPublisher;
import com.hospitalflow.beds.application.port.out.ProcessedMessageStore;
import com.hospitalflow.beds.domain.event.DomainEvent;
import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.BedStatus;
import com.hospitalflow.beds.domain.model.WardId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Fakes en memoria: los puertos permiten testear la aplicación sin BD ni Kafka. */
final class InMemoryAdapters {

    static final class Beds implements BedRepository {
        final Map<String, Bed> store = new LinkedHashMap<>();

        void add(Bed bed) { store.put(bed.id().value(), bed); }

        @Override public Optional<Bed> findById(BedId id) { return Optional.ofNullable(store.get(id.value())); }

        @Override public Optional<Bed> findFirstAvailableInWard(WardId ward) {
            return store.values().stream()
                    .filter(b -> b.ward().equals(ward) && b.status() == BedStatus.AVAILABLE)
                    .min(Comparator.comparing(b -> b.id().value()));
        }

        @Override public List<Bed> findByWard(WardId ward) {
            return store.values().stream().filter(b -> b.ward().equals(ward)).toList();
        }

        @Override public void save(Bed bed) { store.put(bed.id().value(), bed); }
    }

    static final class Events implements DomainEventPublisher {
        final List<DomainEvent> published = new ArrayList<>();

        @Override public void publish(List<DomainEvent> events) { published.addAll(events); }
    }

    static final class Processed implements ProcessedMessageStore {
        final Set<String> ids = new HashSet<>();

        @Override public boolean markProcessed(String messageId) { return ids.add(messageId); }
    }

    private InMemoryAdapters() {}
}
