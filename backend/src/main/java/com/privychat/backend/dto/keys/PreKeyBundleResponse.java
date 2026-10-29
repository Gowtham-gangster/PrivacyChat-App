package com.privychat.backend.dto.keys;

import java.util.UUID;

public class PreKeyBundleResponse {

    private UUID userId;
    private UUID deviceId;
    private int registrationId;
    private String identityKey;
    private SignedPreKeyDto signedPreKey;
    private OneTimePreKeyDto oneTimePreKey; // Null if exhausted

    public PreKeyBundleResponse() {}

    public PreKeyBundleResponse(UUID userId, UUID deviceId, int registrationId, String identityKey, SignedPreKeyDto signedPreKey, OneTimePreKeyDto oneTimePreKey) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.identityKey = identityKey;
        this.signedPreKey = signedPreKey;
        this.oneTimePreKey = oneTimePreKey;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public int getRegistrationId() { return registrationId; }
    public void setRegistrationId(int registrationId) { this.registrationId = registrationId; }

    public String getIdentityKey() { return identityKey; }
    public void setIdentityKey(String identityKey) { this.identityKey = identityKey; }

    public SignedPreKeyDto getSignedPreKey() { return signedPreKey; }
    public void setSignedPreKey(SignedPreKeyDto signedPreKey) { this.signedPreKey = signedPreKey; }

    public OneTimePreKeyDto getOneTimePreKey() { return oneTimePreKey; }
    public void setOneTimePreKey(OneTimePreKeyDto oneTimePreKey) { this.oneTimePreKey = oneTimePreKey; }
}

