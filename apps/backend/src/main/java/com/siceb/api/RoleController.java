package com.siceb.api;

import com.siceb.platform.iam.entity.Permission;
import com.siceb.platform.iam.entity.Role;
import com.siceb.platform.iam.service.RolePermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "IAM", description = "Roles and permissions administration (MNT-03)")
public class RoleController {

    private final RolePermissionService rolePermissionService;

    public RoleController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/roles")
    @PreAuthorize("@auth.check('role:read')")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        List<RoleResponse> roles = rolePermissionService.listRoles().stream().map(RoleResponse::from).toList();
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/roles")
    @PreAuthorize("@auth.check('role:manage')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Role created = rolePermissionService.createRole(request.name(), request.description(), request.permissionKeys());
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(created));
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize("@auth.check('role:manage')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        Role updated = rolePermissionService.updateRole(roleId, request.name(), request.description(), request.permissionKeys());
        return ResponseEntity.ok(RoleResponse.from(updated));
    }

    @GetMapping("/permissions")
    @PreAuthorize("@auth.check('role:read')")
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        List<PermissionResponse> permissions = rolePermissionService.listPermissions()
                .stream()
                .map(PermissionResponse::from)
                .toList();
        return ResponseEntity.ok(permissions);
    }

    public record CreateRoleRequest(
            @NotBlank String name,
            String description,
            @NotNull Set<String> permissionKeys
    ) {}

    public record UpdateRoleRequest(
            String name,
            String description,
            Set<String> permissionKeys
    ) {}

    public record RoleResponse(
            String id,
            String name,
            String description,
            boolean systemRole,
            boolean active,
            Set<String> permissions
    ) {
        public static RoleResponse from(Role role) {
            return new RoleResponse(
                    role.getRoleId().toString(),
                    role.getName(),
                    role.getDescription(),
                    role.isSystemRole(),
                    role.isActive(),
                    role.getPermissions().stream().map(Permission::getKey).collect(java.util.stream.Collectors.toSet())
            );
        }
    }

    public record PermissionResponse(
            String id,
            String key,
            String description,
            String category,
            boolean requiresResidencyCheck
    ) {
        public static PermissionResponse from(Permission p) {
            return new PermissionResponse(
                    p.getPermissionId().toString(),
                    p.getKey(),
                    p.getDescription(),
                    p.getCategory(),
                    p.isRequiresResidencyCheck()
            );
        }
    }
}

