package com.privychat.backend.dto.keys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OneTimePreKeyDto {

    @NotNull(message = "Key ID is required")
    private Integer keyId;

    @NotBlank(message = "Public key is required")
    private String publicKey;

    public OneTimePreKeyDto() {}

    public OneTimePreKeyDto(Integer keyId, String publicKey) {
        this.keyId = keyId;
        this.publicKey = publicKey;
    }

    public Integer getKeyId() { return keyId; }
    public void setKeyId(Integer keyId) { this.keyId = keyId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}

