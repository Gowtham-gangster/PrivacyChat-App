package com.privychat.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyEmailChangeRequest {

    @NotBlank(message = "Verification token is required")
    @Size(min = 6, max = 128, message = "Invalid token length")
    private String token;

    public VerifyEmailChangeRequest() {}

    public VerifyEmailChangeRequest(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}

