package com.minierp.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Credentials stay in the account store. Only the public account record crosses the HTTP boundary. */
public final class Accounts {
    public record User(String username, Role role, boolean active) {}
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 600_000;
    private final Connection db;
    public Accounts(Connection db) { this.db = db; }

    public void bootstrap(String username, String password) throws SQLException {
        try (var query = db.createStatement(); var rows = query.executeQuery("SELECT 1 FROM users LIMIT 1")) {
            if (rows.next()) return;
        }
        if (password == null || password.isBlank())
            throw new IllegalStateException("Erster Start: MINIERP_ADMIN_PASSWORD setzen (mindestens 12 Zeichen).");
        create(username, password, Role.ADMIN);
    }

    public boolean create(String name, String password, Role role) throws SQLException {
        String username = normalize(name);
        if (!username.matches("[a-z0-9][a-z0-9._-]{2,63}"))
            throw new IllegalArgumentException("Benutzername: 3 bis 64 Zeichen aus a–z, 0–9, Punkt, Unterstrich oder Bindestrich.");
        if (password == null || password.isBlank() || password.length() < 12 || password.length() > 256)
            throw new IllegalArgumentException("Das Passwort muss 12 bis 256 Zeichen lang sein.");
        if (role == null) throw new IllegalArgumentException("Bitte eine Rolle wählen.");
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        try (var q = db.prepareStatement("INSERT INTO users(username,role,passwordHash,salt,iterations) VALUES(?,?,?,?,?) ON CONFLICT(username) DO NOTHING")) {
            q.setString(1, username);
            q.setString(2, role.name());
            q.setBytes(3, derive(password, salt, ITERATIONS));
            q.setBytes(4, salt);
            q.setInt(5, ITERATIONS);
            return q.executeUpdate() == 1;
        }
    }

    public User authenticate(String name, String password) throws SQLException {
        if (password == null || password.length() > 256) return null;
        try (var q = db.prepareStatement("SELECT * FROM users WHERE username=?")) {
            q.setString(1, normalize(name));
            try (var r = q.executeQuery()) {
                boolean found = r.next();
                byte[] salt = found ? r.getBytes("salt") : new byte[16];
                byte[] expected = found ? r.getBytes("passwordHash") : new byte[32];
                byte[] actual = derive(password, salt, found ? r.getInt("iterations") : ITERATIONS);
                if (!MessageDigest.isEqual(actual, expected) || !found || !r.getBoolean("active")) return null;
                return new User(r.getString("username"), Role.valueOf(r.getString("role")), true);
            }
        }
    }

    public List<User> list() throws SQLException {
        try (var q = db.createStatement(); var r = q.executeQuery("SELECT username,role,active FROM users ORDER BY username")) {
            var users = new ArrayList<User>();
            while (r.next()) users.add(new User(r.getString(1), Role.valueOf(r.getString(2)), r.getBoolean(3)));
            return users;
        }
    }

    public boolean setActive(String username, boolean active) throws SQLException {
        try (var q = db.prepareStatement("UPDATE users SET active=? WHERE username=?")) {
            q.setBoolean(1, active);
            q.setString(2, username);
            return q.executeUpdate() == 1;
        }
    }

    public static String normalize(String name) { return name == null ? "" : name.strip().toLowerCase(Locale.ROOT); }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("Passwortprüfung nicht verfügbar.", e); }
        finally { spec.clearPassword(); }
    }
}
