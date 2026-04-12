package com.siceb.domain.clinicalcare.repository;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalEventRepository extends JpaRepository<ClinicalEvent, UUID> {

    Optional<ClinicalEvent> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<ClinicalEvent> findByRecordIdOrderByOccurredAtAsc(UUID recordId);

    Page<ClinicalEvent> findByRecordIdOrderByOccurredAtDesc(UUID recordId, Pageable pageable);

    List<ClinicalEvent> findByRecordIdAndEventTypeOrderByOccurredAtAsc(UUID recordId, ClinicalEventType eventType);

    List<ClinicalEvent> findByBranchIdAndEventType(UUID branchId, ClinicalEventType eventType);
}
