package com.minierp.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minierp.dao.stock.StockDAOImpl;
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
                {"name":"  Testartikel  ","description":null,"weight":"250","price":"19.95","minStockThreshold":3}
                """;
        var created = request("POST", "/products", body);
        assertEquals(201, created.statusCode(), created.body());
        int id = json.readTree(created.body()).get("productID").asInt();
        assertEquals("Testartikel", json.readTree(created.body()).get("name").asText());
        assertEquals(250, json.readTree(created.body()).get("weight").asInt());
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

    @Test
    void stocksCanBeCreatedEditedAndValidated() throws Exception {
        int productID = createProduct();
        String body = "{\"productID\":" + productID + ",\"location\":\"a-01-02\",\"quantity\":10,\"maxStock\":50,\"reserved\":2}";
        var created = request("POST", "/stocks", body);
        assertEquals(201, created.statusCode(), created.body());
        int id = json.readTree(created.body()).get("stockID").asInt();
        assertTrue(id > 0);
        assertEquals("A-01-02", json.readTree(created.body()).get("location").asText());
        assertEquals(50, json.readTree(created.body()).get("maxStock").asInt());
        assertEquals(2, json.readTree(created.body()).get("reserved").asInt());
        assertEquals(409, request("POST", "/stocks", body).statusCode());
        assertEquals(400, request("POST", "/stocks", body.replace(":10", ":60")).statusCode());
        assertEquals(400, request("POST", "/stocks", body.replace("\"reserved\":2", "\"reserved\":11")).statusCode());
        var updated = request("PUT", "/stocks/" + id, body.replace(":10", ":15").replace("a-01-02", "B-02-03"));
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(15, json.readTree(updated.body()).get("quantity").asInt());
        assertEquals("B-02-03", json.readTree(request("GET", "/stocks/" + id, null).body()).get("location").asText());
        var second = request("POST", "/stocks", body);
        assertEquals(201, second.statusCode());
        assertEquals(409, request("PUT", "/stocks/" + id, body).statusCode());
        assertEquals(2, json.readTree(request("GET", "/stocks", null).body()).size());
        var stockDAO = new StockDAOImpl(connection);
        stockDAO.deactivateStock(id);
        assertEquals(1, json.readTree(request("GET", "/stocks", null).body()).size());
        stockDAO.reactivateStock(id);
        assertEquals(2, json.readTree(request("GET", "/stocks", null).body()).size());
    }

    @Test
    void ordersUseCataloguePricesAndAllowOnlyValidStatusTransitions() throws Exception {
        int productID = createProduct();
        var customer = request("POST", "/customers", """
                {"name":"Auftragskunde","address":"Berlin","birthdate":null,"email":"order@example.test","phone":"123"}
                """);
        int customerID = json.readTree(customer.body()).get("customerID").asInt();
        String body = "{\"customerID\":" + customerID + ",\"discount\":\"0.10\",\"tax\":\"0.19\",\"items\":[{\"productID\":" + productID + ",\"quantity\":2}]}";
        var created = request("POST", "/orders", body);
        assertEquals(201, created.statusCode(), created.body());
        var detail = json.readTree(created.body());
        int id = detail.get("order").get("orderID").asInt();
        assertTrue(id > 0);
        assertEquals(107.10, detail.get("order").get("totalPrice").asDouble());
        assertEquals(50, detail.get("items").get(0).get("unitPrice").asInt());
        assertEquals(2, detail.get("items").get(0).get("quantity").asInt());
        assertEquals(200, request("GET", "/orders/" + id, null).statusCode());
        assertEquals(400, request("PATCH", "/orders/" + id + "/status", "{\"status\":\"SHIPPED\"}").statusCode());
        var stock = request("POST", "/stocks", "{\"productID\":" + productID + ",\"location\":\"A-01-01\",\"quantity\":10,\"maxStock\":20,\"reserved\":1}");
        assertEquals(201, stock.statusCode(), stock.body());
        int stockID = json.readTree(stock.body()).get("stockID").asInt();
        assertEquals(400, request("PATCH", "/orders/" + id + "/status", "{\"status\":\"CONFIRMED\"}").statusCode());
        var confirmed = request("PATCH", "/orders/" + id + "/status", "{\"status\":\"CONFIRMED\",\"allocations\":[{\"stockID\":" + stockID + ",\"quantity\":2}]}");
        assertEquals(200, confirmed.statusCode(), confirmed.body());
        assertEquals("CONFIRMED", json.readTree(confirmed.body()).get("orderStatus").asText());
        var allocated = json.readTree(request("GET", "/orders/" + id, null).body()).get("allocations");
        assertEquals("RESERVED", allocated.get(0).get("state").asText());
        var reservedStock = json.readTree(request("GET", "/stocks/" + stockID, null).body());
        assertEquals(2, reservedStock.get("orderReserved").asInt());
        assertEquals(1, reservedStock.get("manualReserved").asInt());
        var illegalEdit = request("PUT", "/stocks/" + stockID, "{\"productID\":" + productID + ",\"location\":\"A-01-01\",\"quantity\":10,\"maxStock\":20,\"reserved\":1}");
        assertEquals(409, illegalEdit.statusCode(), illegalEdit.body());
        assertEquals(400, request("POST", "/orders", body.replace("\"quantity\":2", "\"quantity\":0")).statusCode());
        assertEquals(404, request("POST", "/orders", body.replace("\"customerID\":" + customerID, "\"customerID\":999")).statusCode());
        assertEquals(400, request("POST", "/orders", body.replace("\"quantity\":2", "\"quantity\":2,\"unitPrice\":1")).statusCode());
        assertEquals(200, request("PUT", "/products/" + productID, """
                {"name":"Artikel","description":null,"weight":"250","price":"60.00","minStockThreshold":3}
                """).statusCode());
        var historical = json.readTree(request("GET", "/orders/" + id, null).body());
        assertEquals(50, historical.get("items").get(0).get("unitPrice").asInt());
        assertEquals(107.10, historical.get("order").get("totalPrice").asDouble());
        assertEquals(1, json.readTree(request("GET", "/orders", null).body()).size());
    }

    private int createProduct() throws Exception {
        var created = request("POST", "/products", """
                {"name":"Artikel","description":null,"weight":"250","price":"50.00","minStockThreshold":3}
                """);
        assertEquals(201, created.statusCode(), created.body());
        return json.readTree(created.body()).get("productID").asInt();
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + api.port() + "/api" + path); }
    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path));
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
