package com.privychat.backend.dto.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ContactLookupRequest {

    @NotEmpty(message = "Phone numbers list cannot be empty")
    private List<String> phoneNumbers;

    public ContactLookupRequest() {}

    public ContactLookupRequest(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }
}

