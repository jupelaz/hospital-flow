package com.hospitalflow.beds.infrastructure.rest;

import com.hospitalflow.beds.application.port.in.AssignBedCommand;
import com.hospitalflow.beds.application.port.in.AssignBedUseCase;
import com.hospitalflow.beds.application.port.in.MarkBedCleanedUseCase;
import com.hospitalflow.beds.application.port.in.QueryWardBedsUseCase;
import com.hospitalflow.beds.application.port.in.ReleaseBedUseCase;
import com.hospitalflow.beds.domain.model.BedId;
import com.hospitalflow.beds.domain.model.PatientId;
import com.hospitalflow.beds.domain.model.WardId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
class BedController {

    private final AssignBedUseCase assignBed;
    private final ReleaseBedUseCase releaseBed;
    private final MarkBedCleanedUseCase markCleaned;
    private final QueryWardBedsUseCase queryWard;

    BedController(AssignBedUseCase assignBed, ReleaseBedUseCase releaseBed,
                  MarkBedCleanedUseCase markCleaned, QueryWardBedsUseCase queryWard) {
        this.assignBed = assignBed;
        this.releaseBed = releaseBed;
        this.markCleaned = markCleaned;
        this.queryWard = queryWard;
    }

    @GetMapping("/wards/{wardId}/beds")
    List<BedResponse> bedsOfWard(@PathVariable String wardId) {
        return queryWard.bedsOf(new WardId(wardId)).stream().map(BedResponse::from).toList();
    }

    /** Asignación síncrona (p. ej. desde la aplicación del supervisor de planta). */
    @PostMapping("/admissions")
    ResponseEntity<AssignmentResponse> admit(@Valid @RequestBody AdmissionRequest request) {
        BedId bedId = assignBed.assign(
                new AssignBedCommand(new PatientId(request.patientId()), new WardId(request.wardId())));
        return ResponseEntity.created(URI.create("/api/beds/" + bedId.value()))
                .body(new AssignmentResponse(bedId.value()));
    }

    @PostMapping("/beds/{bedId}/release")
    ResponseEntity<Void> release(@PathVariable String bedId) {
        releaseBed.release(new BedId(bedId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/beds/{bedId}/cleaned")
    ResponseEntity<Void> cleaned(@PathVariable String bedId) {
        markCleaned.markCleaned(new BedId(bedId));
        return ResponseEntity.noContent().build();
    }
}
