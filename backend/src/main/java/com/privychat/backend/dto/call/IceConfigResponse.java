package com.privychat.backend.dto.call;

import java.util.List;

public class IceConfigResponse {

    private List<IceServerDto> iceServers;
    private long ttl;
    private boolean turnConfigured;

    public IceConfigResponse() {
    }

    public IceConfigResponse(List<IceServerDto> iceServers, long ttl, boolean turnConfigured) {
        this.iceServers = iceServers;
        this.ttl = ttl;
        this.turnConfigured = turnConfigured;
    }

    public List<IceServerDto> getIceServers() {
        return iceServers;
    }

    public void setIceServers(List<IceServerDto> iceServers) {
        this.iceServers = iceServers;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public boolean isTurnConfigured() {
        return turnConfigured;
    }

    public void setTurnConfigured(boolean turnConfigured) {
        this.turnConfigured = turnConfigured;
    }
}

