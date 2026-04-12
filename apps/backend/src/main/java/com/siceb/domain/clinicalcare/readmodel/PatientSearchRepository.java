package com.siceb.domain.clinicalcare.readmodel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface PatientSearchRepository extends JpaRepository<PatientSearchEntry, UUID> {

    @Query("""
        SELECT p FROM PatientSearchEntry p
        WHERE p.branchId = :branchId
        AND (
            :query IS NULL
            OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(p.curp, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(p.credentialNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(p.phone, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            OR str(p.patientId) = :query
        )
        AND (:dateOfBirth IS NULL OR p.dateOfBirth = :dateOfBirth)
        AND (:patientType IS NULL OR p.patientType = :patientType)
        ORDER BY p.fullName
        """)
    Page<PatientSearchEntry> search(UUID branchId, String query,
                                     LocalDate dateOfBirth, String patientType,
                                     Pageable pageable);
}
