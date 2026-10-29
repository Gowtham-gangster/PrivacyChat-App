package com.privychat.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    private String deviceLabel;

    public LoginRequest() {}

    public LoginRequest(String identifier, String password, String deviceLabel) {
        this.identifier = identifier;
        this.password = password;
        this.deviceLabel = deviceLabel;
    }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
}

