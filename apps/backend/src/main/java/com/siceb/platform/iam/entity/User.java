package com.siceb.platform.iam.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "last_active_branch_id")
    private UUID lastActiveBranchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_branch_assignments",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    private Set<com.siceb.platform.branch.entity.Branch> assignedBranches = new HashSet<>();

    protected User() {}

    public User(String username, String email, String fullName, String passwordHash, Role role, UUID branchId) {
        this.userId = UUID.randomUUID();
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.branchId = branchId;
        this.active = true;
        this.mustChangePassword = true;
        this.failedLoginAttempts = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void ensureId() {
        if (this.userId == null) this.userId = UUID.randomUUID();
    }

    // Getters
    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public UUID getBranchId() { return branchId; }
    public UUID getLastActiveBranchId() { return lastActiveBranchId; }
    public Set<com.siceb.platform.branch.entity.Branch> getAssignedBranches() { return assignedBranches; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters for mutable fields
    public void setFullName(String fullName) { this.fullName = fullName; this.updatedAt = Instant.now(); }
    public void setEmail(String email) { this.email = email; this.updatedAt = Instant.now(); }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; this.updatedAt = Instant.now(); }
    public void setRole(Role role) { this.role = role; this.updatedAt = Instant.now(); }
    public void setActive(boolean active) { this.active = active; this.updatedAt = Instant.now(); }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; this.updatedAt = Instant.now(); }
    public void setBranchId(UUID branchId) { this.branchId = branchId; this.updatedAt = Instant.now(); }
    public void setLastActiveBranchId(UUID lastActiveBranchId) { this.lastActiveBranchId = lastActiveBranchId; this.updatedAt = Instant.now(); }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        this.updatedAt = Instant.now();
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void lockUntil(Instant until) {
        this.lockedUntil = until;
        this.updatedAt = Instant.now();
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public boolean hasPermission(String permissionKey) {
        return role != null && role.hasPermission(permissionKey);
    }
}
