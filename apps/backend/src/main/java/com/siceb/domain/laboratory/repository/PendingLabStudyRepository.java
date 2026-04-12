package com.siceb.domain.laboratory.repository;

import com.siceb.domain.laboratory.model.PendingLabStudy;
import com.siceb.domain.laboratory.model.StudyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PendingLabStudyRepository extends JpaRepository<PendingLabStudy, UUID> {

    List<PendingLabStudy> findByBranchIdAndStatusOrderByRequestedAtAsc(UUID branchId, StudyStatus status);

    List<PendingLabStudy> findByBranchIdAndStatusInOrderByRequestedAtAsc(UUID branchId, List<StudyStatus> statuses);

    List<PendingLabStudy> findByPatientIdOrderByRequestedAtDesc(UUID patientId);
}
