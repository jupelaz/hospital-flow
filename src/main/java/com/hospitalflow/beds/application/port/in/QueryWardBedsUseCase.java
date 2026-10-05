package com.hospitalflow.beds.application.port.in;

import com.hospitalflow.beds.domain.model.Bed;
import com.hospitalflow.beds.domain.model.WardId;

import java.util.List;

public interface QueryWardBedsUseCase {
    List<Bed> bedsOf(WardId wardId);
}
