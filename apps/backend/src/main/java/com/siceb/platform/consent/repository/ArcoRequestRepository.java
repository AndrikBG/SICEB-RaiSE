package com.siceb.platform.consent.repository;

import com.siceb.platform.consent.entity.ArcoRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArcoRequestRepository extends JpaRepository<ArcoRequest, UUID> {
    List<ArcoRequest> findByPatientId(UUID patientId);
    Page<ArcoRequest> findByStatusIn(List<ArcoRequest.ArcoStatus> statuses, Pageable pageable);
}
