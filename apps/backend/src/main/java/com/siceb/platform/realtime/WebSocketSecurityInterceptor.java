package com.siceb.platform.realtime;

import com.siceb.platform.iam.security.JwtClaimsPrincipalMapper;
import com.siceb.platform.iam.security.SicebUserPrincipal;
import com.siceb.platform.iam.service.JwtTokenService;
import com.siceb.platform.iam.service.TokenDenyListService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Three-layer WebSocket security per design §WebSocket Security:
 * <ol>
 *   <li>CONNECT: JWT validated, denied tokens rejected, principal set on session</li>
 *   <li>SUBSCRIBE: destination authorized against user branch + permissions</li>
 *   <li>RLS: PG row-level security as final fallback (not enforced here)</li>
 * </ol>
 */
@Component
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);
    private static final Pattern BRANCH_TOPIC_PATTERN =
            Pattern.compile("^/topic/branch/([0-9a-fA-F\\-]+)/inventory$");

    private final JwtTokenService jwtTokenService;
    private final TokenDenyListService tokenDenyListService;

    public WebSocketSecurityInterceptor(JwtTokenService jwtTokenService,
                                         TokenDenyListService tokenDenyListService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenDenyListService = tokenDenyListService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        return switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor, message);
            default -> message;
        };
    }

    private Message<?> handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT without Authorization header, session={}", accessor.getSessionId());
            return null;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtTokenService.parseAccessToken(token);

            String jti = claims.getId();
            if (jti != null && tokenDenyListService.isDenied(jti)) {
                log.warn("WebSocket CONNECT with denied token, jti={}", jti);
                return null;
            }

            SicebUserPrincipal principal = JwtClaimsPrincipalMapper.fromClaims(claims);

            StompHeaderAccessor outAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            outAccessor.setSessionId(accessor.getSessionId());
            outAccessor.setUser(new StompPrincipal(principal));
            // Copy native headers
            if (accessor.toNativeHeaderMap() != null) {
                accessor.toNativeHeaderMap().forEach((key, values) ->
                        values.forEach(v -> outAccessor.addNativeHeader(key, v)));
            }

            log.debug("WebSocket CONNECT authenticated: user={}, branch={}",
                    principal.userId(), principal.activeBranchId());

            return MessageBuilder.createMessage(new byte[0], outAccessor.getMessageHeaders());

        } catch (JwtException e) {
            log.warn("WebSocket CONNECT with invalid JWT: {}", e.getMessage());
            return null;
        }
    }

    private Message<?> handleSubscribe(StompHeaderAccessor accessor, Message<?> message) {
        if (accessor.getUser() == null) {
            log.warn("WebSocket SUBSCRIBE without authenticated user, session={}", accessor.getSessionId());
            return null;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return null;
        }

        StompPrincipal stompPrincipal = (StompPrincipal) accessor.getUser();
        SicebUserPrincipal principal = stompPrincipal.sicebPrincipal();

        // Admin topic requires inventory:read_all
        if (destination.equals("/topic/admin/inventory")) {
            if (!principal.hasPermission("inventory:read_all")) {
                log.warn("WebSocket SUBSCRIBE to admin topic denied: user={}", principal.userId());
                return null;
            }
            return message;
        }

        // Branch topic: must match active branch or hold inventory:read_all
        Matcher matcher = BRANCH_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            UUID topicBranchId = UUID.fromString(matcher.group(1));
            if (!topicBranchId.equals(principal.activeBranchId())
                    && !principal.hasPermission("inventory:read_all")) {
                log.warn("WebSocket SUBSCRIBE to branch {} denied for user {} (active branch={})",
                        topicBranchId, principal.userId(), principal.activeBranchId());
                return null;
            }
            return message;
        }

        // Non-inventory destinations: allow (other modules can add their own checks)
        return message;
    }

}
