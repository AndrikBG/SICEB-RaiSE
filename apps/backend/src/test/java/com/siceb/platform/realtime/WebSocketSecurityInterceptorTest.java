package com.siceb.platform.realtime;

import com.siceb.platform.iam.security.SicebUserPrincipal;
import com.siceb.platform.iam.service.JwtTokenService;
import com.siceb.platform.iam.service.TokenDenyListService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTest {

    @Mock private JwtTokenService jwtTokenService;
    @Mock private TokenDenyListService tokenDenyListService;
    @Mock private MessageChannel channel;

    private WebSocketSecurityInterceptor interceptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID OTHER_BRANCH_ID = UUID.randomUUID();
    private static final UUID STAFF_ID = UUID.randomUUID();
    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketSecurityInterceptor(jwtTokenService, tokenDenyListService);
    }

    @Nested
    class Connect {

        @Test
        void validJwt_setsPrincipalOnSession() {
            Claims claims = buildClaims(BRANCH_ID, Set.of("inventory:read_service"));
            when(jwtTokenService.parseAccessToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenDenyListService.isDenied(any())).thenReturn(false);

            Message<?> message = buildConnectMessage(VALID_TOKEN);
            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertNotNull(accessor.getUser());
            assertEquals(USER_ID.toString(), accessor.getUser().getName());
        }

        @Test
        void expiredJwt_returnsNull() {
            when(jwtTokenService.parseAccessToken(VALID_TOKEN)).thenThrow(new JwtException("Token expired"));

            Message<?> message = buildConnectMessage(VALID_TOKEN);
            Message<?> result = interceptor.preSend(message, channel);

            assertNull(result);
        }

        @Test
        void deniedToken_returnsNull() {
            Claims claims = buildClaims(BRANCH_ID, Set.of());
            when(jwtTokenService.parseAccessToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenDenyListService.isDenied("test-jti")).thenReturn(true);

            Message<?> message = buildConnectMessage(VALID_TOKEN);
            Message<?> result = interceptor.preSend(message, channel);

            assertNull(result);
        }

        @Test
        void noAuthorizationHeader_returnsNull() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            accessor.setSessionId("session-1");
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(message, channel);

            assertNull(result);
        }
    }

    @Nested
    class Subscribe {

        @Test
        void ownBranch_allowed() {
            SicebUserPrincipal principal = buildPrincipal(BRANCH_ID, Set.of("inventory:read_service"));
            Message<?> message = buildSubscribeMessage(
                    "/topic/branch/" + BRANCH_ID + "/inventory", principal);

            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
        }

        @Test
        void otherBranch_withoutReadAll_rejected() {
            SicebUserPrincipal principal = buildPrincipal(BRANCH_ID, Set.of("inventory:read_service"));
            Message<?> message = buildSubscribeMessage(
                    "/topic/branch/" + OTHER_BRANCH_ID + "/inventory", principal);

            Message<?> result = interceptor.preSend(message, channel);

            assertNull(result);
        }

        @Test
        void otherBranch_withReadAll_allowed() {
            SicebUserPrincipal principal = buildPrincipal(BRANCH_ID, Set.of("inventory:read_all"));
            Message<?> message = buildSubscribeMessage(
                    "/topic/branch/" + OTHER_BRANCH_ID + "/inventory", principal);

            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
        }

        @Test
        void adminTopic_withReadAll_allowed() {
            SicebUserPrincipal principal = buildPrincipal(BRANCH_ID, Set.of("inventory:read_all"));
            Message<?> message = buildSubscribeMessage("/topic/admin/inventory", principal);

            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
        }

        @Test
        void adminTopic_withoutReadAll_rejected() {
            SicebUserPrincipal principal = buildPrincipal(BRANCH_ID, Set.of("inventory:read_service"));
            Message<?> message = buildSubscribeMessage("/topic/admin/inventory", principal);

            Message<?> result = interceptor.preSend(message, channel);

            assertNull(result);
        }

        @Test
        void noUser_rejected() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setSessionId("session-1");
            accessor.setDestination("/topic/branch/" + BRANCH_ID + "/inventory");
            Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(message, channel);

            assertNull(result);
        }
    }

    // ---- Helpers ----

    private Message<?> buildConnectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildSubscribeMessage(String destination, SicebUserPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setDestination(destination);
        accessor.setUser(new StompPrincipal(principal));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Claims buildClaims(UUID activeBranchId, Set<String> permissions) {
        Map<String, Object> map = new HashMap<>();
        map.put(Claims.SUBJECT, USER_ID.toString());
        map.put(Claims.ID, "test-jti");
        map.put("username", "testuser");
        map.put("fullName", "Test User");
        map.put("role", "Administrador General");
        map.put("activeBranchId", activeBranchId.toString());
        map.put("permissions", new ArrayList<>(permissions));
        map.put("branchAssignments", List.of(BRANCH_ID.toString()));
        map.put("staffId", STAFF_ID.toString());
        return new DefaultClaims(map);
    }

    private SicebUserPrincipal buildPrincipal(UUID activeBranchId, Set<String> permissions) {
        return new SicebUserPrincipal(
                USER_ID, "testuser", "Test User", "Administrador General", null,
                activeBranchId, permissions, Set.of(BRANCH_ID.toString()), STAFF_ID);
    }
}
