package com.privychat.backend.dto.keys;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class PublishKeysRequest {

    @NotNull(message = "Device ID is required")
    private UUID deviceId;

    @NotBlank(message = "Identity key is required")
    private String identityKey;

    private int registrationId;

    @NotNull(message = "Signed prekey is required")
    @Valid
    private SignedPreKeyDto signedPreKey;

    @Valid
    private List<OneTimePreKeyDto> oneTimePreKeys;

    public PublishKeysRequest() {}

    public PublishKeysRequest(UUID deviceId, String identityKey, int registrationId, SignedPreKeyDto signedPreKey, List<OneTimePreKeyDto> oneTimePreKeys) {
        this.deviceId = deviceId;
        this.identityKey = identityKey;
        this.registrationId = registrationId;
        this.signedPreKey = signedPreKey;
        this.oneTimePreKeys = oneTimePreKeys;
    }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getIdentityKey() { return identityKey; }
    public void setIdentityKey(String identityKey) { this.identityKey = identityKey; }

    public int getRegistrationId() { return registrationId; }
    public void setRegistrationId(int registrationId) { this.registrationId = registrationId; }

    public SignedPreKeyDto getSignedPreKey() { return signedPreKey; }
    public void setSignedPreKey(SignedPreKeyDto signedPreKey) { this.signedPreKey = signedPreKey; }

    public List<OneTimePreKeyDto> getOneTimePreKeys() { return oneTimePreKeys; }
    public void setOneTimePreKeys(List<OneTimePreKeyDto> oneTimePreKeys) { this.oneTimePreKeys = oneTimePreKeys; }
}

