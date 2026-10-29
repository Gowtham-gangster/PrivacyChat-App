package com.privychat.backend.security;

import com.privychat.backend.config.JwtAuthenticationFilter;
import com.privychat.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AccessControlTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                15,
                30,
                "https://privychat.example.com"
        );
        jwtFilter = new JwtAuthenticationFilter(jwtService);
    }

    @Test
    void testJwtFilterValidatesRealSignedTokenAndSetsSecurityContext() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        String validToken = jwtService.generateAccessToken(userId, deviceId);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void testJwtFilterRejectsInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.signature.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testRefreshTokenCannotBeUsedAsAccessToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        String refreshToken = jwtService.generateRefreshToken(userId, deviceId);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Refresh tokens must not be accepted for API access");
    }
}
