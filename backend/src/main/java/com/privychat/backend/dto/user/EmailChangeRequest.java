package com.privychat.backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailChangeRequest {

    @NotBlank(message = "New email address is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String newEmail;

    public EmailChangeRequest() {}

    public EmailChangeRequest(String newEmail) {
        this.newEmail = newEmail;
    }

    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }
}

