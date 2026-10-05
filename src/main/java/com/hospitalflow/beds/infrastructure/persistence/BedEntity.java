package com.hospitalflow.beds.infrastructure.persistence;

import com.hospitalflow.beds.domain.model.BedStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "beds")
public class BedEntity {

    @Id
    private String id;

    @Column(name = "ward_id", nullable = false)
    private String wardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedStatus status;

    @Column(name = "occupant_id")
    private String occupantId;

    @Version
    private long version;

    protected BedEntity() {}

    public BedEntity(String id, String wardId, BedStatus status, String occupantId, long version) {
        this.id = id;
        this.wardId = wardId;
        this.status = status;
        this.occupantId = occupantId;
        this.version = version;
    }

    public String getId() { return id; }
    public String getWardId() { return wardId; }
    public BedStatus getStatus() { return status; }
    public String getOccupantId() { return occupantId; }
    public long getVersion() { return version; }
}
