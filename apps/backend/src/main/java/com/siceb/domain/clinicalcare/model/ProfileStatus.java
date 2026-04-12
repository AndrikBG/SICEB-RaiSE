package com.siceb.domain.clinicalcare.model;

/**
 * Tracks data completeness (US-019). INCOMPLETE when CURP or other
 * required data is missing but patient is still allowed medical attention.
 */
public enum ProfileStatus {
    COMPLETE, INCOMPLETE
}
