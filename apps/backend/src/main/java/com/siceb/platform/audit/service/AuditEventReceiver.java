package com.siceb.platform.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siceb.platform.audit.entity.AuditLogEntry;
import com.siceb.platform.audit.repository.AuditLogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AuditEventReceiver per requeriments3.md §5.3:
 * - Synchronous ingestion for security-critical events (login, refresh, permission denied)
 * - Asynchronous ingestion for high-volume access events (AuditInterceptor)
 * - Hash chain computed in PostgreSQL trigger (IC-03), not in application
 */
@Service
public class AuditEventReceiver {

    private static final Logger log = LoggerFactory.getLogger(AuditEventReceiver.class);
    private static final String PAYLOAD_EVENT_KEY = "event";

    private final AuditLogEntryRepository auditLogEntryRepository;
    private final ObjectMapper objectMapper;

    public AuditEventReceiver(AuditLogEntryRepository auditLogEntryRepository, ObjectMapper objectMapper) {
        this.auditLogEntryRepository = auditLogEntryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSecurityEvent(SecurityAuditEvent event) {
        persistAuditEvent(event);
    }

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccessEventAsync(SecurityAuditEvent event) {
        persistAuditEvent(event);
    }

    private void persistAuditEvent(SecurityAuditEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event.payload());
            AuditLogEntry entry = new AuditLogEntry(
                    UUID.randomUUID(),
                    event.branchId(),
                    event.userId(),
                    event.action(),
                    event.targetEntity(),
                    event.targetId(),
                    event.ipAddress(),
                    event.userAgent(),
                    payload
            );
            auditLogEntryRepository.save(entry);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize audit payload for action={}", event.action());
        }
    }

    public record SecurityAuditEvent(
            String action,
            UUID userId,
            UUID branchId,
            String targetEntity,
            UUID targetId,
            String ipAddress,
            String userAgent,
            Map<String, Object> payload
    ) {
        public static SecurityAuditEvent loginSuccess(UUID userId, UUID branchId, String username, String ip, String ua) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put(PAYLOAD_EVENT_KEY, "LOGIN_SUCCESS");
            p.put("username", username);
            return new SecurityAuditEvent("AUTH_LOGIN_SUCCESS", userId, branchId, "User", userId, ip, ua, p);
        }

        public static SecurityAuditEvent loginFailure(String username, String ip, String ua, String reasonCode) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put(PAYLOAD_EVENT_KEY, "LOGIN_FAILURE");
            p.put("username", username);
            p.put("reason", reasonCode);
            return new SecurityAuditEvent("AUTH_LOGIN_FAILURE", null, null, "User", null, ip, ua, p);
        }

        public static SecurityAuditEvent refresh(UUID userId, UUID branchId, String ip, String ua) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put(PAYLOAD_EVENT_KEY, "TOKEN_REFRESH");
            return new SecurityAuditEvent("AUTH_REFRESH", userId, branchId, "User", userId, ip, ua, p);
        }

        public static SecurityAuditEvent logout(UUID userId, UUID branchId, String ip, String ua) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put(PAYLOAD_EVENT_KEY, "LOGOUT");
            return new SecurityAuditEvent("AUTH_LOGOUT", userId, branchId, "User", userId, ip, ua, p);
        }

        public static SecurityAuditEvent permissionDenied(UUID userId, UUID branchId,
                String permission, String reason, String ip, String ua) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put(PAYLOAD_EVENT_KEY, "PERMISSION_DENIED");
            p.put("permission", permission);
            p.put("reason", reason);
            return new SecurityAuditEvent("AUTH_PERMISSION_DENIED", userId, branchId, "User", userId, ip, ua, p);
        }

        public static SecurityAuditEvent accessEvent(UUID userId, UUID branchId,
                String method, String path, String targetEntity, UUID targetId, String ip, String ua) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put(PAYLOAD_EVENT_KEY, "ACCESS");
            p.put("method", method);
            p.put("path", path);
            return new SecurityAuditEvent("ACCESS", userId, branchId, targetEntity, targetId, ip, ua, p);
        }
    }
}

