package com.siceb.platform.iam.security;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Residency-level authorization policy per requeriments3.md §5.2:
 * <ul>
 *   <li>R1/R2/R3: blocked from {@code controlled_med:prescribe} (SEC-01, US-051)</li>
 *   <li>R1/R2: require mandatory supervision for residency-checked clinical actions (US-050)</li>
 *   <li>R4: clinical care with minimal supervision (no automatic blocks)</li>
 *   <li>null (non-resident): no residency restrictions apply</li>
 * </ul>
 */
@Component
public class ResidencyLevelPolicy {

    private static final Set<String> CONTROLLED_BLOCKED_LEVELS = Set.of("R1", "R2", "R3");
    private static final Set<String> SUPERVISION_REQUIRED_LEVELS = Set.of("R1", "R2");
    private static final String CONTROLLED_MED_PERMISSION = "controlled_med:prescribe";

    /**
     * Evaluate whether a residency-level restriction blocks this permission.
     *
     * @param residencyLevel       R1–R4 or null if not a resident
     * @param permissionKey        the permission being exercised
     * @param requiresResidencyCheck whether this permission triggers residency evaluation
     */
    public EvaluationResult evaluate(String residencyLevel, String permissionKey,
                                     boolean requiresResidencyCheck) {
        if (residencyLevel == null) {
            return EvaluationResult.allow();
        }

        if (!requiresResidencyCheck) {
            return EvaluationResult.allow();
        }

        if (CONTROLLED_MED_PERMISSION.equals(permissionKey)
                && CONTROLLED_BLOCKED_LEVELS.contains(residencyLevel)) {
            return EvaluationResult.deny(
                    "Residency level %s does not permit prescribing controlled medications"
                            .formatted(residencyLevel));
        }

        if (SUPERVISION_REQUIRED_LEVELS.contains(residencyLevel)) {
            return EvaluationResult.withSupervision(
                    "Residency level %s requires supervisor oversight for this action"
                            .formatted(residencyLevel));
        }

        return EvaluationResult.allow();
    }

    public boolean requiresSupervision(String residencyLevel) {
        return residencyLevel != null && SUPERVISION_REQUIRED_LEVELS.contains(residencyLevel);
    }

    public record EvaluationResult(boolean permitted, boolean supervisionRequired, String reason) {

        public static EvaluationResult allow() {
            return new EvaluationResult(true, false, null);
        }

        public static EvaluationResult deny(String reason) {
            return new EvaluationResult(false, false, reason);
        }

        public static EvaluationResult withSupervision(String reason) {
            return new EvaluationResult(true, true, reason);
        }
    }
}
