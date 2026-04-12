package com.siceb.api;

import com.siceb.platform.branch.command.RegisterBranchCommand;
import com.siceb.platform.branch.entity.Branch;
import com.siceb.platform.branch.entity.BranchOnboardingStep;
import com.siceb.platform.branch.exception.BranchException;
import com.siceb.platform.branch.repository.BranchOnboardingStepRepository;
import com.siceb.platform.branch.repository.BranchRepository;
import com.siceb.platform.branch.service.BranchContextService;
import com.siceb.platform.branch.service.BranchOnboardingOrchestrator;
import com.siceb.platform.branch.service.BranchRegistrationService;
import com.siceb.platform.iam.security.AuthorizationService;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import com.siceb.shared.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchControllerTest {

    @Mock private BranchRegistrationService registrationService;
    @Mock private BranchContextService contextService;
    @Mock private BranchOnboardingOrchestrator onboardingOrchestrator;
    @Mock private BranchRepository branchRepository;
    @Mock private BranchOnboardingStepRepository onboardingStepRepository;
    @Mock private AuthorizationService authorizationService;

    private BranchController controller;

    @BeforeEach
    void setUp() {
        controller = new BranchController(
                registrationService, contextService, onboardingOrchestrator,
                branchRepository, onboardingStepRepository, authorizationService);
    }

    @Test
    void registerBranch_valid_returns201() {
        Branch branch = Branch.builder().name("Norte").build();
        when(registrationService.registerBranch(any())).thenReturn(branch);

        var request = new BranchController.RegisterBranchRequest(
                "Norte", "Addr", null, null, null, null, null);
        var response = controller.registerBranch(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Norte", response.getBody().name());
    }

    @Test
    void listBranches_returnsAll() {
        Branch b1 = Branch.builder().name("Norte").build();
        Branch b2 = Branch.builder().name("Sur").build();
        when(branchRepository.findAll()).thenReturn(List.of(b1, b2));

        var response = controller.listBranches(true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void listBranches_excludeInactive() {
        Branch active = Branch.builder().name("Active").build();
        when(branchRepository.findByActiveTrue()).thenReturn(List.of(active));

        var response = controller.listBranches(false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getBranch_found_returns200() {
        Branch branch = Branch.builder().name("Norte").build();
        UUID branchId = branch.getBranchId();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        var response = controller.getBranch(branchId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Norte", response.getBody().name());
    }

    @Test
    void getBranch_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(branchRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(BranchException.class, () -> controller.getBranch(id));
    }

    @Test
    void deactivateBranch_returns200() {
        Branch branch = Branch.builder().name("Norte").build();
        branch.deactivate();
        UUID branchId = branch.getBranchId();
        when(registrationService.deactivateBranch(branchId)).thenReturn(branch);

        var response = controller.deactivateBranch(branchId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isActive());
    }

    @Test
    void getOnboardingStatus_returnsSteps() {
        UUID branchId = UUID.randomUUID();
        BranchOnboardingStep step1 = new BranchOnboardingStep(branchId, "PARTITION_SETUP");
        step1.markCompleted();
        BranchOnboardingStep step2 = new BranchOnboardingStep(branchId, "SERVICE_CATALOG");

        when(onboardingStepRepository.findByBranchIdOrderByCreatedAtAsc(branchId))
                .thenReturn(List.of(step1, step2));

        var response = controller.getOnboardingStatus(branchId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(branchId, response.getBody().branchId());
        assertEquals(2, response.getBody().steps().size());
        assertEquals(1, response.getBody().completedSteps());
    }

    @Test
    void switchBranch_valid_returns200() {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        var principal = new SicebUserPrincipal(
                userId, "admin", "Admin", "Administrador General", null,
                branchId, Set.of(), Set.of(branchId.toString()), null);
        when(authorizationService.currentPrincipal()).thenReturn(principal);
        var switchResult = new BranchContextService.BranchSwitchResult("jwt", branchId, "Norte");
        when(contextService.switchBranch(userId, branchId)).thenReturn(switchResult);

        var request = new BranchController.SwitchBranchRequest(branchId);
        var response = controller.switchBranch(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt", response.getBody().accessToken());
    }
}
