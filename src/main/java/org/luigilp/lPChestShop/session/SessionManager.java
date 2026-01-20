package org.luigilp.lPChestShop.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SessionManager {

    private final Map<UUID, CreateSession> sessions = new HashMap<>();

    public void set(UUID playerId, CreateSession session) {
        sessions.put(playerId, session);
    }

    public CreateSession get(UUID playerId) {
        return sessions.get(playerId);
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }
}
