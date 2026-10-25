package com.minierp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the HTTP boundary, services and actual SQLite persistence together. */
class ApiIntegrationTest {
    private Connection connection;
    private ApiServer api;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void start() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        api = ApiServer.start(0, connection);
    }

    @AfterEach
    void stop() throws Exception {
        if (api != null) api.close();
        if (connection != null) connection.close();
    }

    @Test
    void customerCanBeCreatedLoadedAndUpdatedWithIsoBirthdate() throws Exception {
        String body = """
                {"name":"  Ada Test  ","address":"Berlin","birthdate":"1990-04-12","email":"ada@example.test","phone":"123"}
                """;
        var created = request("POST", "/customers", body);
        assertEquals(201, created.statusCode(), created.body());
        JsonNode record = json.readTree(created.body());
        int id = record.get("customerID").asInt();
        assertTrue(id > 0);
        assertEquals("1990-04-12", record.get("birthdate").asText());
        assertEquals("/api/customers/" + id, created.headers().firstValue("Location").orElseThrow());
        assertEquals("Ada Test", json.readTree(request("GET", "/customers/" + id, null).body()).get("name").asText());
        assertEquals(409, request("POST", "/customers", body).statusCode());

        var updated = request("PUT", "/customers/" + id, body.replace("Berlin", "Hamburg"));
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals("Hamburg", json.readTree(updated.body()).get("address").asText());
        assertEquals(1, json.readTree(request("GET", "/customers", null).body()).size());
    }

    @Test
    void productWritesAreValidatedAndPersisted() throws Exception {
        String body = """
                {"name":"  Testartikel  ","description":null,"weight":"0.250","price":"19.95","minStockThreshold":3}
                """;
        var created = request("POST", "/products", body);
        assertEquals(201, created.statusCode(), created.body());
        int id = json.readTree(created.body()).get("productID").asInt();
        assertEquals("Testartikel", json.readTree(created.body()).get("name").asText());
        assertEquals(409, request("POST", "/products", body).statusCode());
        assertEquals(400, request("POST", "/products", body.replace("19.95", "-1")).statusCode());
        assertEquals(400, request("POST", "/products", body.replace(":3", ":1.5")).statusCode());
        var updated = request("PUT", "/products/" + id, body.replace("19.95", "24.50"));
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(24.5, json.readTree(request("GET", "/products/" + id, null).body()).get("price").asDouble());
        assertEquals(1, json.readTree(request("GET", "/products", null).body()).size());
    }

    @Test
    void errorsAndCorsArePredictable() throws Exception {
        assertEquals(404, request("GET", "/products/999", null).statusCode());
        assertEquals(404, request("GET", "/unknown", null).statusCode());
        assertEquals(400, request("POST", "/customers", "{broken").statusCode());
        assertEquals(400, request("POST", "/customers", "null").statusCode());
        assertEquals(405, request("DELETE", "/products/1", null).statusCode());
        var options = client.send(HttpRequest.newBuilder(uri("/products"))
                .header("Origin", "http://localhost:5173").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, options.statusCode());
        assertEquals("http://localhost:5173", options.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        var unsupported = client.send(HttpRequest.newBuilder(uri("/products"))
                .header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(415, unsupported.statusCode());
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + api.port() + "/api" + path); }
    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path));
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
