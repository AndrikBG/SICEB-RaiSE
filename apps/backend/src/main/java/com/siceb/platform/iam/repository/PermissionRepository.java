package com.siceb.platform.iam.repository;

import com.siceb.platform.iam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByKey(String key);

    List<Permission> findByCategory(String category);

    List<Permission> findByPermissionIdIn(Set<UUID> permissionIds);

    List<Permission> findByKeyIn(Set<String> keys);
}
