package com.siceb.domain.clinicalcare.readmodel;

import com.siceb.domain.clinicalcare.model.ClinicalEvent;
import com.siceb.domain.clinicalcare.repository.ClinicalEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ClinicalTimelineReadModel — chronologically ordered projection of all
 * clinical events per patient (US-027). Queries the event store directly
 * since the append-only table with its composite index provides efficient
 * chronological access.
 */
@Service
@Transactional(readOnly = true)
public class ClinicalTimelineService {

    private final ClinicalEventRepository eventRepository;

    public ClinicalTimelineService(ClinicalEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<TimelineEntry> getTimeline(UUID recordId) {
        return eventRepository.findByRecordIdOrderByOccurredAtAsc(recordId)
                .stream()
                .map(this::toTimelineEntry)
                .toList();
    }

    public Page<TimelineEntry> getTimelinePaginated(UUID recordId, Pageable pageable) {
        return eventRepository.findByRecordIdOrderByOccurredAtDesc(recordId, pageable)
                .map(this::toTimelineEntry);
    }

    private TimelineEntry toTimelineEntry(ClinicalEvent event) {
        return new TimelineEntry(
                event.getEventId(),
                event.getEventType().name(),
                event.getOccurredAt(),
                event.getPerformedByStaffId(),
                extractSummary(event),
                event.getPayload()  
        );
    }

    private String extractSummary(ClinicalEvent event) {
        Map<String, Object> payload = event.getPayload();
        return switch (event.getEventType()) {
            case RECORD_CREATED -> "Medical record created";
            case CONSULTATION -> "Consultation: " + payload.getOrDefault("diagnosis", "");
            case PRESCRIPTION -> "Prescription: " + countItems(payload) + " medications";
            case LAB_ORDER -> "Lab order: " + payload.getOrDefault("studyType", "");
            case LAB_RESULT -> "Lab result: " + payload.getOrDefault("studyType", "");
            case ATTACHMENT -> "Attachment: " + payload.getOrDefault("fileName", "");
            case CORRECTIVE_ADDENDUM -> "Correction: " + payload.getOrDefault("reason", "");
        };
    }

    private int countItems(Map<String, Object> payload) {
        Object items = payload.get("items");
        if (items instanceof List<?> list) return list.size();
        return 0;
    }

    public record TimelineEntry(
            UUID eventId,
            String eventType,
            java.time.Instant occurredAt,
            UUID performedByStaffId,
            String summary,
            Map<String, Object> payload
    ) {}
}
