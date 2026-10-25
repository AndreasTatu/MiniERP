package com.minierp.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minierp.common.exceptions.*;
import com.minierp.model.customer.Customer;
import com.minierp.model.product.Product;
import com.minierp.service.customer.CustomerService;
import com.minierp.service.product.ProductService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

final class ApiHandler implements HttpHandler {
    private static final int MAX_BODY_BYTES = 65_536;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    private final CustomerService customers;
    private final ProductService products;

    ApiHandler(CustomerService customers, ProductService products) {
        this.customers = customers;
        this.products = products;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            cors(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            Response response;
            try {
                response = route(exchange);
            } catch (CustomerNotFoundException | ProductNotFoundException exception) {
                response = error(404, "Der Datensatz wurde nicht gefunden.");
            } catch (CustomerAlreadyExistsException | ProductAlreadyExistsException exception) {
                response = error(409, "E-Mail-Adresse oder Produktname wird bereits verwendet.");
            } catch (JsonProcessingException exception) {
                response = error(400, "Ungültige JSON-Daten oder ungültiges Datums-/Zahlenformat.");
            } catch (IllegalArgumentException exception) {
                response = error(400, exception.getMessage());
            } catch (HttpProblem exception) {
                response = error(exception.status, exception.getMessage());
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
                response = error(500, "Die Anfrage konnte nicht verarbeitet werden.");
            }
            if (response.location != null) exchange.getResponseHeaders().set("Location", response.location);
            byte[] bytes = json.writeValueAsBytes(response.body);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(response.status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private Response route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.equals("/api/health")) {
            requireMethod(exchange, "GET");
            return new Response(200, Map.of("status", "ok", "service", "MiniERP API"), null);
        }
        if (!path.matches("/api/(customers|products)(/[1-9][0-9]*)?")) {
            throw new HttpProblem(404, "API-Endpunkt nicht gefunden.");
        }
        String[] parts = path.split("/");
        boolean customerRoute = parts[2].equals("customers");
        if (parts.length == 3) {
            requireMethod(exchange, "GET", "POST");
            if (method.equals("GET")) return new Response(200,
                    customerRoute ? customers.findAllActiveCustomers() : products.findAllActiveProducts(), null);
            Object saved;
            int id;
            if (customerRoute) {
                var request = body(exchange, CustomerRequest.class);
                var customer = request.toCustomer(0, true);
                customers.createCustomer(customer);
                var created = customers.findCustomerByEmail(customer.getEmail());
                saved = created;
                id = created.getCustomerID();
            } else {
                var request = body(exchange, ProductRequest.class);
                var product = request.toProduct(0, true);
                products.createProduct(product);
                var created = products.findProductByName(product.getName());
                saved = created;
                id = created.getProductID();
            }
            return new Response(201, saved, path + "/" + id);
        }
        int id = Integer.parseInt(parts[3]);
        requireMethod(exchange, "GET", "PUT");
        if (customerRoute) {
            var current = customers.findCustomerByID(id);
            if (method.equals("PUT")) {
                customers.updateCustomer(body(exchange, CustomerRequest.class).toCustomer(id, current.isActive()));
                current = customers.findCustomerByID(id);
            }
            return new Response(200, current, null);
        }
        var current = products.findProductByID(id);
        if (method.equals("PUT")) {
            products.updateProduct(body(exchange, ProductRequest.class).toProduct(id, current.isActive()));
            current = products.findProductByID(id);
        }
        return new Response(200, current, null);
    }

    private <T> T body(HttpExchange exchange, Class<T> type) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new HttpProblem(415, "Content-Type muss application/json sein.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) throw new HttpProblem(413, "Die Anfrage ist zu groß.");
        T request = json.readValue(bytes, type);
        if (request == null) throw new IllegalArgumentException("Die Anfrage darf nicht leer sein.");
        return request;
    }

    private void requireMethod(HttpExchange exchange, String... allowed) {
        for (String method : allowed) if (exchange.getRequestMethod().equals(method)) return;
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowed) + ", OPTIONS");
        throw new HttpProblem(405, "HTTP-Methode wird nicht unterstützt.");
    }

    private void cors(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null) {
            try {
                URI uri = URI.create(origin);
                if ("http".equals(uri.getScheme()) && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))) {
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
                    exchange.getResponseHeaders().set("Vary", "Origin");
                }
            } catch (IllegalArgumentException ignored) { /* An invalid origin receives no CORS permission. */ }
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private Response error(int status, String message) {
        return new Response(status, Map.of("error", message), null);
    }

    private record Response(int status, Object body, String location) {}
    private static final class HttpProblem extends RuntimeException {
        final int status;
        HttpProblem(int status, String message) { super(message); this.status = status; }
    }

    record CustomerRequest(String name, String address, LocalDate birthdate, String email, String phone) {
        Customer toCustomer(int id, boolean active) {
            return new Customer(id, name, address, birthdate, email, phone, active);
        }
    }

    record ProductRequest(String name, String description, BigDecimal weight, BigDecimal price, Integer minStockThreshold) {
        Product toProduct(int id, boolean active) {
            if (minStockThreshold == null) throw new IllegalArgumentException("Mindestbestand ist erforderlich.");
            return new Product(id, name, description, weight, price, minStockThreshold, active);
        }
    }
}
