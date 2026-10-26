package com.minierp.service.order;

import com.minierp.common.database.JdbcTransactions;
import com.minierp.common.exceptions.StockConflictException;
import com.minierp.dao.order.OrderAllocationDAO;
import com.minierp.dao.order.OrderDAO;
import com.minierp.dao.product.ProductDAO;
import com.minierp.dao.stock.StockDAO;
import com.minierp.model.order.*;
import com.minierp.model.stock.Stock;
import java.sql.SQLException;
import java.util.*;

/** Changes order status, allocations and stock together, or rolls everything back. */
public final class OrderInventoryWorkflow {
    private final OrderDAO orders;
    private final ProductDAO products;
    private final StockDAO stocks;
    private final OrderAllocationDAO allocations;
    private final JdbcTransactions transactions;

    public OrderInventoryWorkflow(OrderDAO orders, ProductDAO products, StockDAO stocks,
                                  OrderAllocationDAO allocations, JdbcTransactions transactions) {
        this.orders = orders; this.products = products; this.stocks = stocks;
        this.allocations = allocations; this.transactions = transactions;
    }

    public void transition(int orderID, OrderStatus next, List<StockSelection> selection) throws SQLException {
        if (next == null) throw new IllegalArgumentException("Ein Auftragsstatus ist erforderlich.");
        if (selection == null) selection = List.of();
        final List<StockSelection> requested = selection;
        transactions.execute(() -> {
            var order = orders.findOrderByID(orderID);
            var current = order.getOrderStatus();
            boolean allowed = current == next || switch (current) {
                case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
                case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
                case SHIPPED -> next == OrderStatus.DELIVERED;
                case DELIVERED, CANCELLED -> false;
            };
            if (!allowed) throw new IllegalArgumentException("Dieser Statuswechsel ist nicht zulässig.");
            if (next != OrderStatus.CONFIRMED && !requested.isEmpty())
                throw new IllegalArgumentException("Eine Lagerauswahl ist nur beim Bestätigen erlaubt.");
            var existing = allocations.findByOrder(orderID);
            if (next == OrderStatus.CONFIRMED) {
                var demand = demand(orderID);
                if (existing.isEmpty()) reserve(orderID, demand, requested);
                else {
                    verifyCoverage(demand, existing);
                    if (!requested.isEmpty() && !sameSelection(existing, requested))
                        throw new StockConflictException("Der Auftrag ist bereits mit anderen Lagerplätzen reserviert. Bitte neu laden.");
                }
            } else if (current != next && next == OrderStatus.SHIPPED) {
                if (existing.isEmpty()) throw new StockConflictException("Für diesen Auftrag fehlen Lagerreservierungen. Bitte zuerst die Lagerzuordnung nachholen.");
                verifyCoverage(demand(orderID), existing);
                finish(existing, OrderAllocation.State.SHIPPED);
            } else if (current != next && next == OrderStatus.CANCELLED) {
                // Older confirmed orders can have no tracked allocations. Their manual stock remains untouched.
                finish(existing, OrderAllocation.State.RELEASED);
            }
            if (current != next) orders.updateOrderStatus(orderID, next);
            return null;
        });
    }

    public List<OrderAllocation> findAllocations(int orderID) throws SQLException {
        orders.findOrderByID(orderID);
        return allocations.findByOrder(orderID);
    }

    private Map<Integer, Long> demand(int orderID) throws SQLException {
        var result = new TreeMap<Integer, Long>();
        for (var item : orders.findAllOrderItemsByOrder(orderID)) {
            if (item.getQuantity() <= 0) throw new StockConflictException("Der Auftrag enthält ungültige Mengen.");
            result.merge(item.getProductID(), (long) item.getQuantity(), Math::addExact);
        }
        if (result.isEmpty()) throw new StockConflictException("Der Auftrag enthält keine Positionen.");
        return result;
    }

    private void reserve(int orderID, Map<Integer, Long> demand, List<StockSelection> selection) throws SQLException {
        if (selection.isEmpty()) throw new IllegalArgumentException("Bitte Lagerplätze und Mengen für die Reservierung auswählen.");
        var selectedByStock = new LinkedHashMap<Integer, StockSelection>();
        var selectedStocks = new HashMap<Integer, Stock>();
        var selectedByProduct = new TreeMap<Integer, Long>();
        for (var entry : selection) {
            if (entry == null || entry.stockID() <= 0 || entry.quantity() <= 0)
                throw new IllegalArgumentException("Jede Lagerauswahl benötigt eine gültige ID und eine positive Menge.");
            if (selectedByStock.putIfAbsent(entry.stockID(), entry) != null)
                throw new IllegalArgumentException("Ein Lagerbestand darf nur einmal ausgewählt werden.");
            var stock = stocks.findStockByStockID(entry.stockID());
            if (!demand.containsKey(stock.getProductID())) throw new IllegalArgumentException("Der gewählte Lagerbestand gehört nicht zu diesem Auftrag.");
            if (!stock.isActive() || !products.findProductByID(stock.getProductID()).isActive())
                throw new StockConflictException("Der gewählte Artikel oder Lagerbestand ist nicht aktiv.");
            if (stock.getReserved() < 0 || stock.getQuantity() < stock.getReserved()
                    || stock.getReserved() < stocks.findOrderReservedQuantity(stock.getStockID()))
                throw new StockConflictException("Der Lagerbestand passt nicht zu seinen Reservierungen. Bitte prüfen.");
            if ((long) stock.getQuantity() - stock.getReserved() < entry.quantity())
                throw new StockConflictException("Am Lagerplatz " + stock.getLocation() + " reicht die verfügbare Menge nicht aus. Bitte Auswahl neu laden.");
            selectedStocks.put(entry.stockID(), stock);
            selectedByProduct.merge(stock.getProductID(), (long) entry.quantity(), Math::addExact);
        }
        if (!demand.equals(selectedByProduct)) throw new IllegalArgumentException("Die gewählten Mengen müssen für jeden Artikel genau der Auftragsmenge entsprechen.");
        for (var entry : selectedByStock.values()) {
            stocks.reserveUnits(entry.stockID(), entry.quantity());
            allocations.create(orderID, selectedStocks.get(entry.stockID()), entry.quantity());
        }
    }

    private void verifyCoverage(Map<Integer, Long> demand, List<OrderAllocation> existing) {
        var reserved = new TreeMap<Integer, Long>();
        for (var allocation : existing) {
            if (allocation.state() != OrderAllocation.State.RESERVED)
                throw new StockConflictException("Die gespeicherte Lagerzuordnung passt nicht zum Auftragsstatus.");
            reserved.merge(allocation.productID(), (long) allocation.quantity(), Math::addExact);
        }
        if (!demand.equals(reserved)) throw new StockConflictException("Die gespeicherten Reservierungen decken den Auftrag nicht vollständig ab.");
    }

    private boolean sameSelection(List<OrderAllocation> existing, List<StockSelection> selection) {
        var expected = new TreeMap<Integer, Integer>();
        existing.forEach(row -> expected.put(row.stockID(), row.quantity()));
        var requested = new TreeMap<Integer, Integer>();
        for (var row : selection) {
            if (row == null || row.quantity() <= 0 || requested.putIfAbsent(row.stockID(), row.quantity()) != null) return false;
        }
        return expected.equals(requested);
    }

    private void finish(List<OrderAllocation> existing, OrderAllocation.State next) throws SQLException {
        for (var allocation : existing) {
            if (allocation.state() != OrderAllocation.State.RESERVED)
                throw new StockConflictException("Die Reservierung wurde bereits abgeschlossen. Bitte Auftrag neu laden.");
            var stock = stocks.findStockByStockID(allocation.stockID());
            long managed = stocks.findOrderReservedQuantity(stock.getStockID());
            if (!stock.isActive() || stock.getProductID() != allocation.productID() || !stock.getLocation().equals(allocation.location())
                    || stock.getReserved() < managed || stock.getQuantity() < stock.getReserved())
                throw new StockConflictException("Der Lagerbestand passt nicht mehr zu seinen Reservierungen. Bitte Lagerbestand prüfen.");
            if (next == OrderAllocation.State.SHIPPED) stocks.shipReservedUnits(stock.getStockID(), allocation.quantity());
            else stocks.releaseUnits(stock.getStockID(), allocation.quantity());
            allocations.setState(allocation.allocationID(), OrderAllocation.State.RESERVED, next);
        }
    }
}
