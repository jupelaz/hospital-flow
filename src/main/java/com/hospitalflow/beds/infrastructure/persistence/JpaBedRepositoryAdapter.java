package com.hospitalflow.beds.infrastructure.persistence;

import com.hospitalflow.beds.application.port.out.BedRepository;
import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.BedStatus;
import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaBedRepositoryAdapter implements BedRepository {

    private final BedJpaRepository jpa;

    JpaBedRepositoryAdapter(BedJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Bed> findById(BedId id) {
        return jpa.findById(id.value()).map(JpaBedRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Bed> findFirstAvailableInWard(WardId ward) {
        return jpa.findFirstByWardIdAndStatusOrderByIdAsc(ward.value(), BedStatus.AVAILABLE)
                .map(JpaBedRepositoryAdapter::toDomain);
    }

    @Override
    public List<Bed> findByWard(WardId ward) {
        return jpa.findByWardIdOrderByIdAsc(ward.value()).stream().map(JpaBedRepositoryAdapter::toDomain).toList();
    }

    /**
     * merge() compara la versión del agregado con la de la fila: si otra transacción
     * la cambió entretanto, Hibernate lanza ObjectOptimisticLockingFailureException.
     */
    @Override
    public void save(Bed bed) {
        jpa.save(new BedEntity(
                bed.id().value(),
                bed.ward().value(),
                bed.status(),
                bed.occupant().map(PatientId::value).orElse(null),
                bed.version()));
    }

    private static Bed toDomain(BedEntity e) {
        return Bed.rehydrate(
                new BedId(e.getId()),
                new WardId(e.getWardId()),
                e.getStatus(),
                e.getOccupantId() == null ? null : new PatientId(e.getOccupantId()),
                e.getVersion());
    }
}
