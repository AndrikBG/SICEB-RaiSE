package com.siceb.platform.iam.repository;

import com.siceb.platform.iam.entity.MedicalStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalStaffRepository extends JpaRepository<MedicalStaff, UUID> {

    Optional<MedicalStaff> findByUserUserId(UUID userId);

    boolean existsByUserUserId(UUID userId);
}
