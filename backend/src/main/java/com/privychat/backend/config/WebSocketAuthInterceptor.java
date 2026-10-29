package com.privychat.backend.config;

import com.privychat.backend.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                List<String> authHeaders = accessor.getNativeHeader("Authorization");
                if (authHeaders == null || authHeaders.isEmpty()) {
                    authHeaders = accessor.getNativeHeader("token");
                }

                if (authHeaders != null && !authHeaders.isEmpty()) {
                    String token = authHeaders.get(0).replace("Bearer ", "").trim();
                    Claims claims = jwtService.validateToken(token);
                    if (claims != null && "ACCESS".equals(claims.get("type"))) {
                        String userId = claims.getSubject();
                        Principal principal = () -> userId;
                        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
                        return message;
                    }
                }
                throw new AccessDeniedException("Unauthorized WebSocket connection attempt: Invalid or missing token");
            } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                Principal user = accessor.getUser();

                if (destination != null && destination.startsWith("/topic/user.") && user != null) {
                    // Destination format: /topic/user.{userId}.messages
                    String[] parts = destination.split("\\.");
                    if (parts.length >= 2) {
                        String targetUserId = parts[1];
                        if (!user.getName().equals(targetUserId)) {
                            throw new AccessDeniedException("Forbidden: Cannot subscribe to another user's private topic");
                        }
                    }
                }
            }
        }

        return message;
    }
}

