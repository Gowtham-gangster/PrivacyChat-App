package com.privychat.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PhoneChangeRequest {

    @NotBlank(message = "New phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone number must be in valid E.164 format (e.g. +14155552671)")
    private String newPhoneNumber;

    public PhoneChangeRequest() {}

    public PhoneChangeRequest(String newPhoneNumber) {
        this.newPhoneNumber = newPhoneNumber;
    }

    public String getNewPhoneNumber() { return newPhoneNumber; }
    public void setNewPhoneNumber(String newPhoneNumber) { this.newPhoneNumber = newPhoneNumber; }
}

