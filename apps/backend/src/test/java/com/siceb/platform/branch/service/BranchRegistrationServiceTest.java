package com.siceb.platform.branch.service;

import com.siceb.platform.audit.service.AuditEventReceiver;
import com.siceb.platform.branch.command.RegisterBranchCommand;
import com.siceb.platform.branch.command.UpdateBranchCommand;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchRegistrationServiceTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchOnboardingOrchestrator onboardingOrchestrator;
    @Mock private AuditEventReceiver auditEventReceiver;

    private BranchRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new BranchRegistrationService(branchRepository, onboardingOrchestrator, auditEventReceiver);
    }

    // --- registerBranch ---

    @Test
    void registerBranch_validData_createsBranchAndStartsOnboarding() {
        var cmd = new RegisterBranchCommand(
                "Sucursal Norte", "Av. Universidad 1500",
                "+528112345678", "norte@clinica.mx",
                LocalTime.of(8, 0), LocalTime.of(20, 0), "NTE");

        when(branchRepository.existsByName("Sucursal Norte")).thenReturn(false);
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        Branch result = service.registerBranch(cmd);

        assertNotNull(result.getBranchId());
        assertEquals("Sucursal Norte", result.getName());
        assertEquals("+528112345678", result.getPhone());
        assertEquals("NTE", result.getBranchCode());
        assertTrue(result.isActive());
        assertFalse(result.isOnboardingComplete());

        verify(onboardingOrchestrator).startOnboarding(result.getBranchId());
        verify(auditEventReceiver).recordSecurityEvent(any());
    }

    @Test
    void registerBranch_duplicateName_throwsConflict() {
        var cmd = new RegisterBranchCommand(
                "Sucursal Norte", "addr", null, null, null, null, null);

        when(branchRepository.existsByName("Sucursal Norte")).thenReturn(true);

        BranchException ex = assertThrows(BranchException.class, () -> service.registerBranch(cmd));
        assertEquals("SICEB-4001", ex.getErrorCode().code());
    }

    // --- updateBranch ---

    @Test
    void updateBranch_existingBranch_updatesFields() {
        Branch branch = Branch.builder().name("Original").address("Addr").build();
        UUID branchId = branch.getBranchId();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new UpdateBranchCommand("Updated", "New Addr", "+521234567890", "new@test.mx",
                LocalTime.of(9, 0), LocalTime.of(18, 0));

        Branch result = service.updateBranch(branchId, cmd);

        assertEquals("Updated", result.getName());
        assertEquals("New Addr", result.getAddress());
        assertEquals("+521234567890", result.getPhone());
    }

    @Test
    void updateBranch_notFound_throws404() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(BranchException.class, () ->
                service.updateBranch(id, new UpdateBranchCommand("X", null, null, null, null, null)));
    }

    // --- deactivateBranch ---

    @Test
    void deactivateBranch_activeBranch_setsInactive() {
        Branch branch = Branch.builder().name("Test").build();
        UUID branchId = branch.getBranchId();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        Branch result = service.deactivateBranch(branchId);

        assertFalse(result.isActive());
        verify(auditEventReceiver).recordSecurityEvent(any());
    }

    @Test
    void deactivateBranch_notFound_throws404() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(BranchException.class, () -> service.deactivateBranch(id));
    }
}
