package com.siceb.domain.clinicalcare.model;

import java.math.BigDecimal;

/**
 * Patient classification determining automatic discount (US-020).
 * Discounts apply to consultations and medical services.
 */
public enum PatientType {
    STUDENT(new BigDecimal("30.00")),
    WORKER(new BigDecimal("20.00")),
    EXTERNAL(BigDecimal.ZERO);

    private final BigDecimal discountPercentage;

    PatientType(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public BigDecimal discountPercentage() {
        return discountPercentage;
    }
}
