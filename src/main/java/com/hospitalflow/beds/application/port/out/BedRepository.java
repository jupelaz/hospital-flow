package com.hospitalflow.beds.application.port.out;

import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.WardId;

import java.util.List;
import java.util.Optional;

public interface BedRepository {
    Optional<Bed> findById(BedId id);
    Optional<Bed> findFirstAvailableInWard(WardId ward);
    List<Bed> findByWard(WardId ward);
    /** Debe fallar si la versión no coincide (bloqueo optimista). */
    void save(Bed bed);
}
