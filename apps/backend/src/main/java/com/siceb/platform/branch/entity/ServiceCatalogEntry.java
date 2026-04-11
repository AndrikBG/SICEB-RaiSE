package com.siceb.platform.branch.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branch_service_catalog")
public class ServiceCatalogEntry {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ServiceCatalogEntry() {}

    public ServiceCatalogEntry(UUID branchId, String serviceName, String serviceCode) {
        this.id = UUID.randomUUID();
        this.branchId = branchId;
        this.serviceName = serviceName;
        this.serviceCode = serviceCode;
        this.active = true;
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public String getServiceName() { return serviceName; }
    public String getServiceCode() { return serviceCode; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void deactivate() { this.active = false; }
}
