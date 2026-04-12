package com.siceb.platform.iam.service;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.audit.service.AuditEventReceiver.SecurityAuditEvent;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.iam.entity.MedicalStaff;
import com.siceb.platform.iam.entity.Role;
import com.siceb.platform.iam.entity.User;
import com.siceb.platform.iam.repository.MedicalStaffRepository;
import com.siceb.platform.iam.repository.RefreshTokenRepository;
import com.siceb.platform.iam.repository.RoleRepository;
import com.siceb.platform.iam.repository.UserRepository;
import com.siceb.shared.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * User lifecycle management per requeriments3.md §8.1 and US-001:
 * <ul>
 *   <li>Create user with role, branch assignments, optional medical staff record</li>
 *   <li>Update user profile and role (role change invalidates JWTs)</li>
 *   <li>Deactivate user (revokes all tokens via TokenDenyList)</li>
 *   <li>Assign/remove branch assignments</li>
 * </ul>
 */
@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);
    private static final String DEFAULT_TEMP_PASSWORD = "Temporal1";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final MedicalStaffRepository medicalStaffRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventReceiver auditEventReceiver;

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 BranchRepository branchRepository,
                                 MedicalStaffRepository medicalStaffRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuditEventReceiver auditEventReceiver) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.medicalStaffRepository = medicalStaffRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventReceiver = auditEventReceiver;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return findUserOrThrow(userId);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IamException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserCreationResult createUser(CreateUserRequest request) {
        validateCreateRequest(request);

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new IamException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));

        UUID primaryBranchId = request.primaryBranchId();
        branchRepository.findById(primaryBranchId)
                .orElseThrow(() -> new IamException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Primary branch not found: %s".formatted(primaryBranchId)));

        String tempPassword = request.temporaryPassword() != null
                ? request.temporaryPassword() : DEFAULT_TEMP_PASSWORD;
        validatePasswordPolicy(tempPassword);

        User user = new User(
                request.username().trim(),
                request.email().trim().toLowerCase(),
                request.fullName().trim(),
                passwordEncoder.encode(tempPassword),
                role,
                primaryBranchId
        );

        Set<Branch> branches = resolveBranches(request.branchAssignments());
        Branch primaryBranch = branchRepository.findById(primaryBranchId).orElseThrow();
        branches.add(primaryBranch);
        user.getAssignedBranches().addAll(branches);

        user = userRepository.save(user);

        MedicalStaff medicalStaff = null;
        if (request.medicalStaffInfo() != null) {
            medicalStaff = createMedicalStaff(user, request.medicalStaffInfo());
        }

        log.info("User created: username={}, role={}", user.getUsername(), role.getName());

        return new UserCreationResult(user, medicalStaff);
    }

    @Transactional
    public User updateUser(UUID userId, UpdateUserRequest request) {
        User user = findUserOrThrow(userId);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new IamException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Email already in use");
            }
            user.setEmail(newEmail);
        }

        if (request.roleId() != null && !request.roleId().equals(user.getRole().getRoleId())) {
            Role newRole = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new IamException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
            user.setRole(newRole);
            refreshTokenRepository.revokeAllByUserId(userId);
            log.info("Role changed for user={}, new role={}; tokens revoked", user.getUsername(), newRole.getName());
        }

        if (request.branchAssignments() != null) {
            Set<Branch> branches = resolveBranches(request.branchAssignments());
            user.getAssignedBranches().clear();
            user.getAssignedBranches().addAll(branches);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(UUID userId, UUID performedBy) {
        User user = findUserOrThrow(userId);

        if (!user.isActive()) {
            throw new IamException(ErrorCode.BUSINESS_RULE_VIOLATION, "User is already inactive");
        }

        user.setActive(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("User deactivated: username={}, performedBy={}", user.getUsername(), performedBy);

        auditEventReceiver.recordSecurityEvent(new SecurityAuditEvent(
                "USER_DEACTIVATED", performedBy, null, "User", userId, null, null,
                Map.of("event", "USER_DEACTIVATED", "targetUsername", user.getUsername())
        ));
    }

    @Transactional
    public void activateUser(UUID userId) {
        User user = findUserOrThrow(userId);

        if (user.isActive()) {
            throw new IamException(ErrorCode.BUSINESS_RULE_VIOLATION, "User is already active");
        }

        user.setActive(true);
        user.resetFailedAttempts();
        userRepository.save(user);

        log.info("User reactivated: username={}", user.getUsername());
    }

    @Transactional
    public void resetPassword(UUID userId) {
        User user = findUserOrThrow(userId);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_TEMP_PASSWORD));
        user.setMustChangePassword(true);
        user.resetFailedAttempts();
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Password reset for user={}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public Optional<MedicalStaff> getMedicalStaff(UUID userId) {
        return medicalStaffRepository.findByUserUserId(userId);
    }

    // ---- Internal helpers ----

    private MedicalStaff createMedicalStaff(User user, MedicalStaffInfo info) {
        if (medicalStaffRepository.existsByUserUserId(user.getUserId())) {
            throw new IamException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Medical staff record already exists for this user");
        }

        MedicalStaff staff = new MedicalStaff(
                user,
                info.specialty(),
                info.residencyLevel(),
                info.canPrescribeControlled()
        );
        staff.setProfessionalLicense(info.professionalLicense());

        if (info.supervisorStaffId() != null) {
            MedicalStaff supervisor = medicalStaffRepository.findById(info.supervisorStaffId())
                    .orElseThrow(() -> new IamException(ErrorCode.RESOURCE_NOT_FOUND, "Supervisor not found"));
            staff.setSupervisor(supervisor);
        } else if (staff.requiresSupervision()) {
            throw new IamException(ErrorCode.VALIDATION_FAILED,
                    "Residents R1/R2 require a supervisor assignment (US-050)");
        }

        return medicalStaffRepository.save(staff);
    }

    private Set<Branch> resolveBranches(Set<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) return new HashSet<>();
        List<Branch> found = branchRepository.findAllById(branchIds);
        if (found.size() != branchIds.size()) {
            throw new IamException(ErrorCode.RESOURCE_NOT_FOUND, "One or more branches not found");
        }
        return new HashSet<>(found);
    }

    private void validateCreateRequest(CreateUserRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Username is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Email is required");
        }
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Full name is required");
        }
        if (!request.email().matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Invalid email format");
        }
        if (userRepository.existsByUsername(request.username().trim())) {
            throw new IamException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Username already exists");
        }
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new IamException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Email already in use");
        }
    }

    private void validatePasswordPolicy(String password) {
        if (password.length() < 8) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Password must be at least 8 characters");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IamException(ErrorCode.VALIDATION_FAILED, "Password must contain at least one number");
        }
    }

    // ---- Request / Result records ----

    public record CreateUserRequest(
            String username,
            String email,
            String fullName,
            UUID roleId,
            UUID primaryBranchId,
            Set<UUID> branchAssignments,
            String temporaryPassword,
            MedicalStaffInfo medicalStaffInfo
    ) {}

    public record UpdateUserRequest(
            String fullName,
            String email,
            UUID roleId,
            Set<UUID> branchAssignments
    ) {}

    public record MedicalStaffInfo(
            String specialty,
            String residencyLevel,
            boolean canPrescribeControlled,
            String professionalLicense,
            UUID supervisorStaffId
    ) {}

    public record UserCreationResult(User user, MedicalStaff medicalStaff) {}
}
