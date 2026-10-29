package com.privychat.backend.dto.auth;

import com.privychat.backend.dto.user.UserProfileResponse;

import java.util.UUID;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserProfileResponse user;
    private UUID deviceId;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserProfileResponse user, UUID deviceId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
        this.deviceId = deviceId;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public UserProfileResponse getUser() { return user; }
    public void setUser(UserProfileResponse user) { this.user = user; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
}

