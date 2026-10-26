package com.minierp.dao.order;

import com.minierp.common.exceptions.StockConflictException;
import com.minierp.model.order.OrderAllocation;
import com.minierp.model.stock.Stock;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class OrderAllocationDAOImpl implements OrderAllocationDAO {
    private final Connection connection;
    public OrderAllocationDAOImpl(Connection connection) { this.connection = connection; }

    @Override
    public List<OrderAllocation> findByOrder(int orderID) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT * FROM orderAllocations WHERE orderID = ? ORDER BY productID, location, stockID")) {
            statement.setInt(1, orderID);
            var allocations = new ArrayList<OrderAllocation>();
            try (var results = statement.executeQuery()) {
                while (results.next()) allocations.add(new OrderAllocation(results.getInt("allocationID"),
                        results.getInt("orderID"), results.getInt("stockID"), results.getInt("productID"),
                        results.getString("location"), results.getInt("quantity"), OrderAllocation.State.valueOf(results.getString("state"))));
            }
            return allocations;
        }
    }

    @Override
    public void create(int orderID, Stock stock, int quantity) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO orderAllocations (orderID, stockID, productID, location, quantity, state) VALUES (?, ?, ?, ?, ?, 'RESERVED')")) {
            statement.setInt(1, orderID);
            statement.setInt(2, stock.getStockID());
            statement.setInt(3, stock.getProductID());
            statement.setString(4, stock.getLocation());
            statement.setInt(5, quantity);
            if (statement.executeUpdate() != 1) throw new SQLException("Allocation could not be saved.");
        }
    }

    @Override
    public void setState(int id, OrderAllocation.State expected, OrderAllocation.State next) throws SQLException {
        try (var statement = connection.prepareStatement("UPDATE orderAllocations SET state = ? WHERE allocationID = ? AND state = ?")) {
            statement.setString(1, next.name()); statement.setInt(2, id); statement.setString(3, expected.name());
            if (statement.executeUpdate() != 1) throw new StockConflictException("Die Lagerzuordnung wurde bereits verändert. Bitte den Auftrag neu laden.");
        }
    }
}
