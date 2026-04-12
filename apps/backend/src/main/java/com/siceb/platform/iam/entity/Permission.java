package com.siceb.platform.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(name = "key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "requires_residency_check", nullable = false)
    private boolean requiresResidencyCheck;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Permission() {}

    @PrePersist
    void ensureId() {
        if (this.permissionId == null) this.permissionId = UUID.randomUUID();
    }

    public UUID getPermissionId() { return permissionId; }
    public String getKey() { return key; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isRequiresResidencyCheck() { return requiresResidencyCheck; }
    public Instant getCreatedAt() { return createdAt; }
}
