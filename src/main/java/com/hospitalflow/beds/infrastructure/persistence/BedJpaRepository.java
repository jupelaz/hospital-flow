package com.hospitalflow.beds.infrastructure.persistence;

import com.hospitalflow.beds.domain.model.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface BedJpaRepository extends JpaRepository<BedEntity, String> {

    Optional<BedEntity> findFirstByWardIdAndStatusOrderByIdAsc(String wardId, BedStatus status);

    List<BedEntity> findByWardIdOrderByIdAsc(String wardId);
}
