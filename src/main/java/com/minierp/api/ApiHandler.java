package com.minierp.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minierp.common.exceptions.*;
import com.minierp.model.customer.Customer;
import com.minierp.model.product.Product;
import com.minierp.model.order.Order;
import com.minierp.model.order.OrderItem;
import com.minierp.model.order.OrderStatus;
import com.minierp.model.order.OrderAllocation;
import com.minierp.model.order.StockSelection;
import com.minierp.model.stock.Stock;
import com.minierp.service.customer.CustomerService;
import com.minierp.service.product.ProductService;
import com.minierp.service.order.OrderService;
import com.minierp.service.stock.StockService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

final class ApiHandler implements HttpHandler {
    private static final int MAX_BODY_BYTES = 65_536;
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    private final CustomerService customers;
    private final ProductService products;
    private final OrderService orders;
    private final StockService stocks;

    ApiHandler(CustomerService customers, ProductService products, OrderService orders, StockService stocks) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.stocks = stocks;
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
            } catch (CustomerNotFoundException | ProductNotFoundException | OrderNotFoundException | StockNotFoundException exception) {
                response = error(404, "Der Datensatz wurde nicht gefunden.");
            } catch (CustomerAlreadyExistsException | ProductAlreadyExistsException exception) {
                response = error(409, "E-Mail-Adresse oder Produktname wird bereits verwendet.");
            } catch (StockAlreadyExistsException exception) {
                response = error(409, "Für diesen Artikel existiert an diesem Lagerplatz bereits ein Bestand.");
            } catch (StockConflictException exception) {
                response = error(409, exception.getMessage());
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

    private Response route(HttpExchange exchange) throws IOException, SQLException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.equals("/api/health")) {
            requireMethod(exchange, "GET");
            return new Response(200, Map.of("status", "ok", "service", "MiniERP API"), null);
        }
        if (path.matches("/api/orders(/[1-9][0-9]*(/status)?)?")) return orderRoute(exchange);
        if (path.matches("/api/stocks(/[1-9][0-9]*)?")) return stockRoute(exchange);
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

    private Response orderRoute(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length == 3) {
            requireMethod(exchange, "GET", "POST");
            if (exchange.getRequestMethod().equals("GET")) return new Response(200, orders.findAllOrders(), null);
            var request = body(exchange, OrderRequest.class);
            var customer = customers.findCustomerByID(request.customerID);
            if (!customer.isActive()) throw new IllegalArgumentException("Der Kunde ist nicht aktiv.");
            if (request.items == null || request.items.isEmpty()) throw new IllegalArgumentException("Mindestens eine Position ist erforderlich.");
            var items = request.items.stream().map(item -> {
                if (item == null) throw new IllegalArgumentException("Eine Auftragsposition darf nicht leer sein.");
                var product = products.findProductByID(item.productID);
                return new OrderItem(item.productID, item.quantity, product.getPrice());
            }).toList();
            var order = new Order(request.customerID, LocalDateTime.now(ZoneId.of("Europe/Berlin")), request.discount, request.tax, null);
            orders.createOrder(order, items);
            return new Response(201, orderDetail(order.getOrderID()), path + "/" + order.getOrderID());
        }
        int id = Integer.parseInt(parts[3]);
        if (parts.length == 5) {
            requireMethod(exchange, "PATCH");
            var request = body(exchange, StatusRequest.class);
            orders.updateOrderStatus(id, request.status, request.allocations);
            return new Response(200, orders.findOrderByID(id), null);
        }
        requireMethod(exchange, "GET");
        return new Response(200, orderDetail(id), null);
    }

    private OrderDetail orderDetail(int id) {
        return new OrderDetail(orders.findOrderByID(id), orders.findAllOrderItemsByOrder(id), orders.findAllocationsByOrder(id));
    }

    private Response stockRoute(HttpExchange exchange) throws IOException, SQLException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length == 3) {
            requireMethod(exchange, "GET", "POST");
            if (exchange.getRequestMethod().equals("GET")) {
                var views = new java.util.ArrayList<StockView>();
                for (var stock : stocks.findAllActiveStock()) views.add(stockView(stock));
                return new Response(200, views, null);
            }
            var stock = body(exchange, StockRequest.class).toStock(0, true);
            stocks.createStock(stock);
            return new Response(201, stockView(stocks.findStockByStockID(stock.getStockID())), path + "/" + stock.getStockID());
        }
        int id = Integer.parseInt(parts[3]);
        requireMethod(exchange, "GET", "PUT");
        var current = stocks.findStockByStockID(id);
        if (exchange.getRequestMethod().equals("PUT")) {
            stocks.updateFullStock(body(exchange, StockRequest.class).toStock(id, current.isActive()));
            current = stocks.findStockByStockID(id);
        }
        return new Response(200, stockView(current), null);
    }

    private StockView stockView(Stock stock) throws SQLException {
        long orderReserved = stocks.findOrderReservedQuantity(stock.getStockID());
        if (orderReserved > stock.getReserved()) throw new StockConflictException("Die Lagerreservierungen sind inkonsistent. Bitte den Lagerbestand prüfen.");
        return new StockView(stock.getStockID(), stock.getProductID(), stock.getLocation(), stock.getQuantity(),
                stock.getMaxStock(), stock.getReserved(), stock.isActive(), orderReserved, stock.getReserved() - orderReserved);
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, OPTIONS");
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

    record OrderRequest(int customerID, BigDecimal discount, BigDecimal tax, List<OrderLineRequest> items) {}
    record OrderLineRequest(int productID, int quantity) {}
    record StatusRequest(OrderStatus status, List<StockSelection> allocations) {}
    record OrderDetail(Order order, List<OrderItem> items, List<OrderAllocation> allocations) {}
    record StockView(int stockID, int productID, String location, int quantity, int maxStock,
                     int reserved, boolean active, long orderReserved, long manualReserved) {}
    record StockRequest(int productID, String location, Integer quantity, Integer maxStock, Integer reserved) {
        Stock toStock(int id, boolean active) {
            if (quantity == null || maxStock == null || reserved == null)
                throw new IllegalArgumentException("Bestand, Kapazität und Reservierung sind erforderlich.");
            return new Stock(id, productID, location, quantity, maxStock, reserved, active);
        }
    }
}
