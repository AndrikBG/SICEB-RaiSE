package com.siceb.domain.clinicalcare.service;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.model.ClinicalEventType;
import com.siceb.domain.clinicalcare.readmodel.ClinicalEventProjector;
import com.siceb.domain.clinicalcare.repository.ClinicalEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable, append-only persistence layer for clinical events (IC-02, CRN-02).
 * Validates IdempotencyKey to prevent duplicate writes during offline sync (CRN-43).
 * No update or delete operations are exposed.
 */
@Service
public class ClinicalEventStore {

    private static final Logger log = LoggerFactory.getLogger(ClinicalEventStore.class);

    private final ClinicalEventRepository repository;
    private final ClinicalEventProjector projector;

    public ClinicalEventStore(ClinicalEventRepository repository,
                              ClinicalEventProjector projector) {
        this.repository = repository;
        this.projector = projector;
    }

    /**
     * Appends an event to the store. If the idempotency key already exists,
     * returns the existing event without creating a duplicate (safe offline replay).
     *
     * @return the persisted event (new or existing if idempotent replay)
     */
    @Transactional
    public ClinicalEvent append(ClinicalEvent event) {
        Optional<ClinicalEvent> existing = repository.findByIdempotencyKey(event.getIdempotencyKey());
        if (existing.isPresent()) {
            // Keep read models convergent in case an idempotent replay happens after partial failures.
            projector.project(existing.get());
            log.info("Idempotent replay detected for key={}, returning existing event={}",
                    event.getIdempotencyKey(), existing.get().getEventId());
            return existing.get();
        }

        ClinicalEvent saved = repository.save(event);
        projector.project(saved);
        log.debug("Appended clinical event: id={}, type={}, record={}, branch={}",
                saved.getEventId(), saved.getEventType(), saved.getRecordId(), saved.getBranchId());
        return saved;
    }

    /**
     * Appends multiple events atomically within a single transaction.
     */
    @Transactional
    public List<ClinicalEvent> appendAll(List<ClinicalEvent> events) {
        return events.stream().map(this::append).toList();
    }

    public List<ClinicalEvent> findByRecordChronological(UUID recordId) {
        return repository.findByRecordIdOrderByOccurredAtAsc(recordId);
    }

    public List<ClinicalEvent> findByRecordAndType(UUID recordId, ClinicalEventType type) {
        return repository.findByRecordIdAndEventTypeOrderByOccurredAtAsc(recordId, type);
    }

    public List<ClinicalEvent> findByBranchAndType(UUID branchId, ClinicalEventType type) {
        return repository.findByBranchIdAndEventType(branchId, type);
    }

    public Optional<ClinicalEvent> findByIdempotencyKey(String key) {
        return repository.findByIdempotencyKey(key);
    }
}
