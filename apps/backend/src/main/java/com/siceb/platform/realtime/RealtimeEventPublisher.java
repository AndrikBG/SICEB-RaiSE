package com.siceb.platform.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Bridge between PostgreSQL pg_notify and STOMP messaging.
 * Holds a dedicated (non-pooled) JDBC connection with LISTEN on the
 * {@code inventory_changes} channel. Polls at 100ms intervals and
 * publishes each notification to tenant-scoped STOMP topics.
 */
@Component
public class RealtimeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);
    private static final String CHANNEL = "inventory_changes";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DataSource dataSource;
    private final SimpMessagingTemplate messagingTemplate;
    private volatile Connection listenConnection;

    public RealtimeEventPublisher(DataSource dataSource, SimpMessagingTemplate messagingTemplate) {
        this.dataSource = dataSource;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Establish the LISTEN connection. Called lazily on first poll
     * to avoid startup failures when the database isn't ready yet.
     */
    void ensureListening() {
        if (listenConnection != null) {
            return;
        }
        try {
            listenConnection = dataSource.getConnection();
            // Disable auto-commit so the connection stays open for LISTEN
            listenConnection.setAutoCommit(true);
            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("LISTEN " + CHANNEL);
            }
            log.info("pg_notify LISTEN established on channel '{}'", CHANNEL);
        } catch (SQLException e) {
            log.error("Failed to establish LISTEN connection on channel '{}': {}", CHANNEL, e.getMessage());
            closeConnection();
        }
    }

    @Scheduled(fixedDelay = 100)
    void pollNotifications() {
        ensureListening();

        if (listenConnection == null) {
            return;
        }

        try {
            PGConnection pgConn = listenConnection.unwrap(PGConnection.class);

            // Issue empty query to fetch pending notifications
            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("");
            }

            PGNotification[] notifications = pgConn.getNotifications();
            if (notifications == null) {
                return;
            }

            for (PGNotification notification : notifications) {
                processNotification(notification.getParameter());
            }
        } catch (SQLException e) {
            log.error("Error polling pg_notify: {}", e.getMessage());
            closeConnection();
        }
    }

    void processNotification(String payload) {
        try {
            JsonNode node = MAPPER.readTree(payload);
            String branchId = node.get("branchId").asText();

            // Publish to branch-scoped topic
            String branchDest = "/topic/branch/" + branchId + "/inventory";
            messagingTemplate.convertAndSend(branchDest, payload);

            // Publish to admin topic (filtered client-side)
            messagingTemplate.convertAndSend("/topic/admin/inventory", payload);

            log.debug("Published inventory event to {} — deltaType={}, itemId={}",
                    branchDest, node.path("deltaType").asText(), node.path("itemId").asText());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse pg_notify payload: {}", e.getMessage());
        }
    }

    @PreDestroy
    void closeConnection() {
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException e) {
                log.debug("Error closing LISTEN connection: {}", e.getMessage());
            }
            listenConnection = null;
        }
    }
}
