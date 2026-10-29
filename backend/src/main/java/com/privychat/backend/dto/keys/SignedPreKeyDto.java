package com.privychat.backend.dto.keys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SignedPreKeyDto {

    @NotNull(message = "Key ID is required")
    private Integer keyId;

    @NotBlank(message = "Public key is required")
    private String publicKey;

    @NotBlank(message = "Signature is required")
    private String signature;

    public SignedPreKeyDto() {}

    public SignedPreKeyDto(Integer keyId, String publicKey, String signature) {
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public Integer getKeyId() { return keyId; }
    public void setKeyId(Integer keyId) { this.keyId = keyId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}

