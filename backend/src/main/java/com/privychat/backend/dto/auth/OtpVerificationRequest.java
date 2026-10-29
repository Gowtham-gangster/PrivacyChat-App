package com.privychat.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OtpVerificationRequest {

    @NotBlank(message = "Identifier is required")
    private String identifier; // Email or phone number

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
    private String otpCode;

    public OtpVerificationRequest() {}

    public OtpVerificationRequest(String identifier, String otpCode) {
        this.identifier = identifier;
        this.otpCode = otpCode;
    }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}

