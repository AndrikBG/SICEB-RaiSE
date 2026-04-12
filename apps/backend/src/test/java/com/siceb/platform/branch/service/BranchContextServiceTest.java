package com.siceb.platform.branch.service;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.iam.entity.Role;
import com.siceb.platform.iam.entity.User;
import com.siceb.platform.iam.repository.MedicalStaffRepository;
import com.siceb.platform.iam.repository.UserRepository;
import com.siceb.platform.iam.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchContextServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private MedicalStaffRepository medicalStaffRepository;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private AuditEventReceiver auditEventReceiver;

    private BranchContextService service;

    @BeforeEach
    void setUp() {
        service = new BranchContextService(
                userRepository, branchRepository, medicalStaffRepository,
                jwtTokenService, auditEventReceiver);
    }

    // --- switchBranch ---

    @Test
    void switchBranch_assignedActiveBranch_returnsNewToken() {
        UUID userId = UUID.randomUUID();
        Role role = new Role("Test", "test");
        User user = new User("testuser", "test@test.mx", "Test", "hash", role, UUID.randomUUID());

        Branch targetBranch = Branch.builder().name("Sucursal Norte").build();
        UUID targetBranchId = targetBranch.getBranchId();

        when(branchRepository.findById(targetBranchId)).thenReturn(Optional.of(targetBranch));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(medicalStaffRepository.findByUserUserId(userId)).thenReturn(Optional.empty());
        when(jwtTokenService.generateAccessToken(eq(user), eq(targetBranchId), any()))
                .thenReturn("new-jwt-token");

        user.getAssignedBranches().add(targetBranch);

        BranchContextService.BranchSwitchResult result = service.switchBranch(userId, targetBranchId);

        assertEquals("new-jwt-token", result.accessToken());
        assertEquals(targetBranchId, result.branchId());
        assertEquals("Sucursal Norte", result.branchName());
        verify(userRepository).save(user);
        verify(auditEventReceiver).recordSecurityEvent(any());
    }

    @Test
    void switchBranch_unassignedBranch_throws403() {
        UUID userId = UUID.randomUUID();
        Role role = new Role("Test", "test");
        User user = new User("testuser", "test@test.mx", "Test", "hash", role, UUID.randomUUID());

        Branch targetBranch = Branch.builder().name("Unassigned").build();
        UUID targetBranchId = targetBranch.getBranchId();

        when(branchRepository.findById(targetBranchId)).thenReturn(Optional.of(targetBranch));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // User NOT assigned

        BranchException ex = assertThrows(BranchException.class,
                () -> service.switchBranch(userId, targetBranchId));
        assertEquals("SICEB-5002", ex.getErrorCode().code());
    }

    @Test
    void switchBranch_deactivatedBranch_throws403() {
        UUID userId = UUID.randomUUID();

        Branch targetBranch = Branch.builder().name("Deactivated").build();
        targetBranch.deactivate();
        UUID targetBranchId = targetBranch.getBranchId();

        when(branchRepository.findById(targetBranchId)).thenReturn(Optional.of(targetBranch));
        // Exception thrown before userRepository is called

        BranchException ex = assertThrows(BranchException.class,
                () -> service.switchBranch(userId, targetBranchId));
        assertEquals("SICEB-5002", ex.getErrorCode().code());
    }

    @Test
    void switchBranch_nonExistentBranch_throws404() {
        UUID userId = UUID.randomUUID();
        UUID targetBranchId = UUID.randomUUID();

        when(branchRepository.findById(targetBranchId)).thenReturn(Optional.empty());

        BranchException ex = assertThrows(BranchException.class,
                () -> service.switchBranch(userId, targetBranchId));
        assertEquals("SICEB-3001", ex.getErrorCode().code());
    }

    // --- resolveInitialBranch ---

    @Test
    void resolveInitialBranch_lastActiveStillAssigned_returnsLastActive() {
        Role role = new Role("Test", "test");
        User user = new User("testuser", "test@test.mx", "Test", "hash", role, UUID.randomUUID());

        Branch lastBranch = Branch.builder().name("Last").build();
        UUID lastBranchId = lastBranch.getBranchId();
        user.setLastActiveBranchId(lastBranchId);
        user.getAssignedBranches().add(lastBranch);

        when(branchRepository.findById(lastBranchId)).thenReturn(Optional.of(lastBranch));

        UUID result = service.resolveInitialBranch(user);
        assertEquals(lastBranchId, result);
    }

    @Test
    void resolveInitialBranch_noLastActive_returnsFirstAssigned() {
        Role role = new Role("Test", "test");
        User user = new User("testuser", "test@test.mx", "Test", "hash", role, UUID.randomUUID());

        Branch assigned = Branch.builder().name("First").build();
        user.getAssignedBranches().add(assigned);

        UUID result = service.resolveInitialBranch(user);
        assertEquals(assigned.getBranchId(), result);
    }

    @Test
    void resolveInitialBranch_noBranches_throwsException() {
        Role role = new Role("Test", "test");
        User user = new User("testuser", "test@test.mx", "Test", "hash", role, UUID.randomUUID());

        assertThrows(BranchException.class, () -> service.resolveInitialBranch(user));
    }
}
