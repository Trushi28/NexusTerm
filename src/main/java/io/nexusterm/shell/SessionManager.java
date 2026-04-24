package io.nexusterm.shell;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

/**
 * Manages active user sessions for collaborative features.
 */
public class SessionManager {
    private final Map<String, NexusShell> sessions = new ConcurrentHashMap<>();

    public void register(String id, NexusShell shell) {
        sessions.put(id, shell);
    }

    public void unregister(String id) {
        sessions.remove(id);
    }

    public Collection<NexusShell> getActiveSessions() {
        return sessions.values();
    }
    
    public java.util.Optional<NexusShell> getSession(String id) {
        return java.util.Optional.ofNullable(sessions.get(id));
    }
}
