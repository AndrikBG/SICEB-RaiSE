package com.siceb.platform.consent.repository;

import com.siceb.platform.consent.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {
    List<ConsentRecord> findByPatientId(UUID patientId);
    List<ConsentRecord> findByPatientIdAndRevokedAtIsNull(UUID patientId);
    List<ConsentRecord> findByPatientIdAndConsentType(UUID patientId, String consentType);
}
