package com.siceb.platform.realtime;

import com.siceb.platform.iam.security.SicebUserPrincipal;

import java.security.Principal;

/**
 * Wraps {@link SicebUserPrincipal} as a {@link Principal} for STOMP sessions.
 * Allows the user identity to travel with WebSocket messages.
 */
public record StompPrincipal(SicebUserPrincipal sicebPrincipal) implements Principal {

    @Override
    public String getName() {
        return sicebPrincipal.userId().toString();
    }
}
