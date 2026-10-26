package com.minierp.service.stock;

import com.minierp.common.database.JdbcTransactions;
import com.minierp.common.exceptions.StockConflictException;
import com.minierp.dao.product.ProductDAO;
import com.minierp.dao.stock.StockDAO;
import com.minierp.model.stock.Stock;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class StockServiceImpl implements StockService {
    private final StockDAO stocks;
    private final ProductDAO products;
    private final JdbcTransactions transactions;

    public StockServiceImpl(StockDAO stocks, ProductDAO products, JdbcTransactions transactions) {
        this.stocks = stocks; this.products = products; this.transactions = transactions;
    }

    @Override
    public void createStock(Stock stock) {
        validate(stock);
        write(() -> { requireActiveProduct(stock.getProductID()); stocks.createStock(stock); return null; });
    }

    @Override
    public Stock findStockByStockID(int id) { positiveID(id); return read(() -> stocks.findStockByStockID(id)); }

    @Override
    public List<Stock> findStockByProductID(int id) { positiveID(id); return read(() -> stocks.findStockByProductID(id)); }

    @Override
    public List<Stock> findAllActiveStock() { return read(stocks::findAllActiveStock); }

    @Override
    public List<Stock> findAllStock() { return read(stocks::findAllStock); }

    @Override
    public long findOrderReservedQuantity(int id) { positiveID(id); return read(() -> stocks.findOrderReservedQuantity(id)); }

    @Override
    public void updateFullStock(Stock stock) {
        validate(stock); positiveID(stock.getStockID());
        write(() -> {
            var current = stocks.findStockByStockID(stock.getStockID());
            long managed = checkReservations(current, stock.getReserved());
            if (managed > 0 && (stock.getProductID() != current.getProductID()
                    || !stock.getLocation().equals(current.getLocation()) || stock.isActive() != current.isActive()))
                throw new StockConflictException("Artikel und Lagerplatz können bei Auftragsreservierungen nicht verändert werden.");
            requireActiveProduct(stock.getProductID());
            stocks.updateFullStock(stock);
            return null;
        });
    }

    @Override
    public void updateStockQuantityAndReserved(int id, int quantity, int reserved) {
        positiveID(id);
        write(() -> {
            var current = stocks.findStockByStockID(id);
            validateQuantities(quantity, current.getMaxStock(), reserved);
            checkReservations(current, reserved);
            stocks.updateStockQuantityAndReserved(id, quantity, reserved);
            return null;
        });
    }

    @Override
    public void deactivateStock(int id) {
        positiveID(id);
        write(() -> {
            stocks.findStockByStockID(id);
            if (stocks.findOrderReservedQuantity(id) > 0)
                throw new StockConflictException("Ein Lagerbestand mit Auftragsreservierungen kann nicht deaktiviert werden.");
            stocks.deactivateStock(id); return null;
        });
    }

    @Override
    public void reactivateStock(int id) {
        positiveID(id);
        write(() -> { requireActiveProduct(stocks.findStockByStockID(id).getProductID()); stocks.reactivateStock(id); return null; });
    }

    private long checkReservations(Stock current, int nextReserved) throws SQLException {
        long managed = stocks.findOrderReservedQuantity(current.getStockID());
        if (current.getReserved() < managed)
            throw new StockConflictException("Der Lagerbestand passt nicht zu seinen Auftragsreservierungen. Bitte prüfen.");
        if (nextReserved < managed)
            throw new StockConflictException("Mindestens " + managed + " Stück sind durch Aufträge reserviert. Diese Reservierungen können hier nicht reduziert werden.");
        return managed;
    }

    private void requireActiveProduct(int id) throws SQLException {
        if (!products.findProductByID(id).isActive()) throw new IllegalArgumentException("Der Artikel ist nicht aktiv.");
    }

    private void validate(Stock stock) {
        if (stock == null) throw new IllegalArgumentException("Ein Lagerbestand ist erforderlich.");
        positiveID(stock.getProductID());
        String location = stock.getLocation() == null ? "" : stock.getLocation().trim().toUpperCase(Locale.ROOT);
        if (!location.matches("[A-Z]-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Der Lagerplatz muss dem Format A-01-02 entsprechen.");
        stock.setLocation(location);
        validateQuantities(stock.getQuantity(), stock.getMaxStock(), stock.getReserved());
    }

    private void validateQuantities(int quantity, int capacity, int reserved) {
        if (quantity < 0 || capacity <= 0 || quantity > capacity || reserved < 0 || reserved > quantity)
            throw new IllegalArgumentException("Bestand und Reservierung müssen nichtnegativ sein; Reservierung ≤ Bestand ≤ Kapazität, Kapazität > 0.");
    }

    private void positiveID(int id) { if (id <= 0) throw new IllegalArgumentException("Die ID muss positiv sein."); }
    private <T> T read(JdbcTransactions.Work<T> work) {
        try { return work.run(); }
        catch (SQLException exception) { throw new RuntimeException("Database access error", exception); }
    }
    private <T> T write(JdbcTransactions.Work<T> work) { return read(() -> transactions.execute(work)); }
}
