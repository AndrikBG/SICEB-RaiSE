package com.siceb.domain.clinicalcare.repository;

import com.siceb.domain.clinicalcare.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    @Query("""
        SELECT p FROM Patient p
        WHERE LOWER(CONCAT(p.firstName, ' ', p.paternalSurname, ' ', COALESCE(p.maternalSurname, '')))
            LIKE LOWER(CONCAT('%', :name, '%'))
        AND p.dateOfBirth = :dob
        """)
    List<Patient> findDuplicateCandidates(String name, LocalDate dob);

    boolean existsByPatientId(UUID patientId);
}
