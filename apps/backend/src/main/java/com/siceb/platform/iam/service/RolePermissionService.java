package com.siceb.platform.iam.service;

import com.siceb.platform.iam.entity.Permission;
import com.siceb.platform.iam.entity.Role;
import com.siceb.platform.iam.repository.PermissionRepository;
import com.siceb.platform.iam.repository.RoleRepository;
import com.siceb.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RolePermissionService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional
    public Role createRole(String name, String description, Set<String> permissionKeys) {
        if (name == null || name.isBlank()) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Role name is required");
        }
        if (roleRepository.existsByName(name)) {
            throw new IamException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Role name already exists");
        }

        Role role = new Role(name.trim(), description);
        role.setPermissions(resolvePermissions(permissionKeys));
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(UUID roleId, String name, String description, Set<String> permissionKeys) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IamException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));

        if (role.isSystemRole()) {
            throw new IamException(ErrorCode.FORBIDDEN, "System roles cannot be modified");
        }

        if (name != null && !name.isBlank() && !name.equals(role.getName())) {
            if (roleRepository.existsByName(name)) {
                throw new IamException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Role name already exists");
            }
            role.setName(name.trim());
        }

        if (description != null) {
            role.setDescription(description);
        }

        if (permissionKeys != null) {
            role.setPermissions(resolvePermissions(permissionKeys));
        }

        return roleRepository.save(role);
    }

    private Set<Permission> resolvePermissions(Set<String> permissionKeys) {
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalized = new HashSet<>();
        for (String k : permissionKeys) {
            if (k == null || k.isBlank()) continue;
            normalized.add(k.trim());
        }

        List<Permission> permissions = permissionRepository.findByKeyIn(normalized);
        if (permissions.size() != normalized.size()) {
            Set<String> found = new HashSet<>();
            for (Permission p : permissions) found.add(p.getKey());
            normalized.removeAll(found);
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Unknown permission keys: " + String.join(", ", normalized));
        }

        return new HashSet<>(permissions);
    }
}

