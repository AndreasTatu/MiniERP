package com.minierp.api;

import com.minierp.common.database.DatabaseConnection;
import com.minierp.common.database.SchemaInitializer;
import com.minierp.dao.customer.CustomerDAOImpl;
import com.minierp.dao.product.ProductDAOImpl;
import com.minierp.dao.order.OrderDAOImpl;
import com.minierp.dao.stock.StockDAOImpl;
import com.minierp.service.customer.CustomerServiceImpl;
import com.minierp.service.product.ProductServiceImpl;
import com.minierp.service.order.OrderServiceImpl;
import com.minierp.service.stock.StockServiceImpl;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns the local HTTP server; business logic stays in the service layer. */
public final class ApiServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;

    private ApiServer(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
    }

    public static ApiServer start(int port, Connection connection) throws IOException, SQLException {
        SchemaInitializer.initialize(connection);
        var customers = new CustomerServiceImpl(new CustomerDAOImpl(connection));
        var productDAO = new ProductDAOImpl(connection);
        var products = new ProductServiceImpl(productDAO);
        var orders = new OrderServiceImpl(new OrderDAOImpl(connection), productDAO);
        var stocks = new StockServiceImpl(new StockDAOImpl(connection), productDAO);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        // The existing DAOs share one JDBC connection. Serialize access until a pool is introduced.
        var executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext("/api/", new ApiHandler(customers, products, orders, stocks));
        server.start();
        return new ApiServer(server, executor);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    public static void main(String[] args) throws IOException, SQLException {
        int port = Integer.parseInt(System.getenv().getOrDefault("MINIERP_PORT", "8080"));
        var api = start(port, DatabaseConnection.getConnection());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            api.close();
            DatabaseConnection.closeConnection();
        }));
        System.out.println("MiniERP API: http://127.0.0.1:" + api.port() + "/api");
    }
}
