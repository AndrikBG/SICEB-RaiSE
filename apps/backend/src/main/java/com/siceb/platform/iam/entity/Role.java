package com.siceb.platform.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private boolean systemRole;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {}

    public Role(String name, String description) {
        this.roleId = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.systemRole = false;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void ensureId() {
        if (this.roleId == null) this.roleId = UUID.randomUUID();
    }

    public UUID getRoleId() { return roleId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isSystemRole() { return systemRole; }
    public boolean isActive() { return active; }
    public Set<Permission> getPermissions() { return permissions; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; this.updatedAt = Instant.now(); }
    public void setDescription(String description) { this.description = description; this.updatedAt = Instant.now(); }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
        this.updatedAt = Instant.now();
    }

    public boolean hasPermission(String permissionKey) {
        return permissions.stream().anyMatch(p -> p.getKey().equals(permissionKey));
    }
}
