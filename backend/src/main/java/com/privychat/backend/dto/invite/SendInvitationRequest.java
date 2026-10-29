package com.privychat.backend.dto.invite;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SendInvitationRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Valid email address is required")
    private String recipientEmail;

    public SendInvitationRequest() {}

    public SendInvitationRequest(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
}

