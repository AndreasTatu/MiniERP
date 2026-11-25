package com.minierp.api;

import com.minierp.auth.Accounts;
import com.minierp.auth.Accounts.User;
import com.minierp.auth.Role;
import com.minierp.auth.Sessions;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import static com.minierp.api.HttpApi.*;

final class AccountRoutes {
    private final Accounts accounts;
    private final Sessions sessions = new Sessions(Clock.systemUTC());
    private final String configuredOrigin = System.getenv("MINIERP_FRONTEND_ORIGIN");
    private static final Set<String> DEV_ORIGINS = Set.of("http://localhost:5173", "http://127.0.0.1:5173");
    private static final String COOKIE = "minierp_session";

    AccountRoutes(Accounts accounts) { this.accounts = accounts; }

    void checkOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().set("Vary", "Origin");
        if (origin == null) return;
        if (!(configuredOrigin == null ? DEV_ORIGINS.contains(origin) : configuredOrigin.equals(origin)))
            throw new HttpProblem(403, "Dieser Ursprung ist nicht für die API freigegeben.");
        var headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", origin);
        headers.set("Access-Control-Allow-Credentials", "true");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    Response login(HttpExchange e) throws IOException, SQLException {
        requireMethod(e, "POST");
        var input = body(e, Login.class);
        String name = Accounts.normalize(input.username);
        if (sessions.throttled(name)) throw new HttpProblem(429, "Zu viele Anmeldeversuche. Bitte in fünf Minuten erneut versuchen.");
        User user = accounts.authenticate(name, input.password);
        if (user == null) {
            sessions.failed(name);
            throw new HttpProblem(401, "Benutzername oder Passwort ist falsch.");
        }
        sessions.clearFailures(name);
        sessions.close(token(e));
        cookie(e, sessions.open(user), Sessions.LIFETIME.toSeconds());
        return session(user);
    }

    User requireUser(HttpExchange e) {
        var user = sessions.current(token(e));
        if (user == null) throw new HttpProblem(401, "Bitte erneut anmelden.");
        return user;
    }

    static void require(User user, Role.Permission permission) {
        if (!user.role().permissions.contains(permission)) throw new HttpProblem(403, "Für diese Aktion fehlt die Berechtigung.");
    }

    Response route(HttpExchange e, User user) throws IOException, SQLException {
        String path = e.getRequestURI().getPath();
        if (path.equals("/api/session")) {
            requireMethod(e, "GET");
            return session(user);
        }
        if (path.equals("/api/logout")) {
            requireMethod(e, "POST");
            body(e, Empty.class);
            sessions.close(token(e));
            cookie(e, "", 0);
            return new Response(200, Map.of("ok", true), null);
        }
        require(user, Role.Permission.USERS_MANAGE);
        if (path.equals("/api/users")) {
            requireMethod(e, "GET", "POST");
            if ("GET".equals(e.getRequestMethod())) return new Response(200, accounts.list(), null);
            var input = body(e, CreateUser.class);
            if (!accounts.create(input.username, input.password, input.role)) throw new HttpProblem(409, "Der Benutzername ist bereits vergeben.");
            return new Response(201, new User(Accounts.normalize(input.username), input.role, true), null);
        }
        if (!path.matches("/api/users/[a-z0-9][a-z0-9._-]{2,63}")) throw new HttpProblem(404, "Benutzer nicht gefunden.");
        requireMethod(e, "PATCH");
        String name = path.substring("/api/users/".length());
        var active = body(e, Activation.class).active;
        if (active == null) throw new IllegalArgumentException("Der Aktivstatus ist erforderlich.");
        if (name.equals(user.username()) && !active) throw new HttpProblem(409, "Das eigene Konto kann nicht gesperrt werden.");
        if (!accounts.setActive(name, active)) throw new HttpProblem(404, "Benutzer nicht gefunden.");
        if (!active) sessions.revoke(name);
        return new Response(200, Map.of("ok", true), null);
    }

    private static Response session(User user) {
        return new Response(200, Map.of("username", user.username(), "role", user.role(), "permissions", user.role().permissions), null);
    }

    private static String token(HttpExchange e) {
        for (String header : e.getRequestHeaders().getOrDefault("Cookie", java.util.List.of())) {
            for (String part : header.split(";")) {
                String entry = part.trim();
                if (entry.startsWith(COOKIE + "=")) return entry.substring(COOKIE.length() + 1);
            }
        }
        return "";
    }

    private void cookie(HttpExchange e, String token, long age) {
        String secure = configuredOrigin != null && configuredOrigin.startsWith("https://") ? "; Secure" : "";
        e.getResponseHeaders().set("Set-Cookie", COOKIE + "=" + token + "; Path=/api; HttpOnly; SameSite=Strict; Max-Age=" + age + secure);
    }

    private record Login(String username, String password) {}
    private record CreateUser(String username, String password, Role role) {}
    private record Activation(Boolean active) {}
    private record Empty() {}
}
