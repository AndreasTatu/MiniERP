package com.minierp.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Process-local sessions for the API's single request executor. */
public final class Sessions {
    public static final Duration LIFETIME = Duration.ofHours(8);
    private static final long LOGIN_WINDOW = Duration.ofMinutes(5).toMillis();
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, Attempts> failures = new LinkedHashMap<>();
    private record Session(Accounts.User user, long expires) {}
    private record Attempts(int count, long until) {}

    public Sessions(Clock clock) { this.clock = clock; }

    public String open(Accounts.User user) {
        long now = clock.millis();
        sessions.values().removeIf(s -> s.expires <= now);
        byte[] token = new byte[32];
        random.nextBytes(token);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        sessions.put(key, new Session(user, now + LIFETIME.toMillis()));
        return key;
    }

    public Accounts.User current(String key) {
        Session session = sessions.get(key);
        if (session == null) return null;
        if (session.expires > clock.millis()) return session.user;
        sessions.remove(key);
        return null;
    }

    public void close(String key) { sessions.remove(key); }
    public void revoke(String username) { sessions.values().removeIf(s -> s.user.username().equals(username)); }

    public boolean throttled(String username) {
        Attempts entry = failures.get(username);
        return entry != null && entry.until > clock.millis() && entry.count >= 8;
    }
    public void failed(String username) {
        long now = clock.millis();
        failures.values().removeIf(entry -> entry.until <= now);
        if (!failures.containsKey(username) && failures.size() >= 1024) failures.remove(failures.keySet().iterator().next());
        failures.compute(username, (key, entry) -> entry == null ? new Attempts(1, now + LOGIN_WINDOW) : new Attempts(entry.count + 1, entry.until));
    }
    public void clearFailures(String username) { failures.remove(username); }
}
