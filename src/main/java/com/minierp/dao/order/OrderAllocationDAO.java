package com.minierp.dao.order;

import com.minierp.model.order.OrderAllocation;
import com.minierp.model.stock.Stock;
import java.sql.SQLException;
import java.util.List;

public interface OrderAllocationDAO {
    List<OrderAllocation> findByOrder(int orderID) throws SQLException;
    void create(int orderID, Stock stock, int quantity) throws SQLException;
    void setState(int allocationID, OrderAllocation.State expected, OrderAllocation.State next) throws SQLException;
}
