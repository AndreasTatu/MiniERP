package com.minierp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.database.SchemaInitializer;
import com.minierp.dao.customer.CustomerDAOImpl;
import com.minierp.dao.product.ProductDAOImpl;
import com.minierp.service.customer.CustomerService;
import com.minierp.service.customer.CustomerServiceImpl;
import com.minierp.service.product.ProductService;
import com.minierp.service.product.ProductServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/** HTTP entry point for the React client, backed by the existing service and DAO layers. */
public final class ApiServer {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    private ApiServer() {}

    public static void main(String[] args) throws IOException, SQLException {
        var connection = DatabaseConnection.getConnection();
        SchemaInitializer.initialize(connection);
        CustomerService customers = new CustomerServiceImpl(new CustomerDAOImpl(connection));
        ProductService products = new ProductServiceImpl(new ProductDAOImpl(connection));
        int port = Integer.parseInt(System.getenv().getOrDefault("MINIERP_PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", exchange -> handle(exchange, customers, products));
        // Existing DAOs share a singleton JDBC connection, so process API calls sequentially.
        server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
        server.start();
        System.out.println("MiniERP API listening on http://localhost:" + port + "/api");
    }

    private static void handle(HttpExchange exchange, CustomerService customers, ProductService products) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            switch (exchange.getRequestURI().getPath()) {
                case "/api/health" -> sendJson(exchange, 200, JSON.writeValueAsString(java.util.Map.of("status", "ok", "service", "MiniERP API")));
                case "/api/customers" -> sendJson(exchange, 200, JSON.writeValueAsString(customers.findAllActiveCustomers()));
                case "/api/products" -> sendJson(exchange, 200, JSON.writeValueAsString(products.findAllActiveProducts()));
                default -> sendJson(exchange, 404, "{\"error\":\"Route not found\"}");
            }
        } catch (Exception exception) {
            System.err.println("API request failed: " + exception.getMessage());
            sendJson(exchange, 500, "{\"error\":\"The request could not be completed\"}");
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && (origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:"))) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
