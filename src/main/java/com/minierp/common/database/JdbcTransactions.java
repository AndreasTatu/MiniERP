package com.minierp.common.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;

/** Executes one unit of work, preserving a caller's enclosing transaction. */
public final class JdbcTransactions {
    private final Connection connection;

    public JdbcTransactions(Connection connection) { this.connection = Objects.requireNonNull(connection); }

    public <T> T execute(Work<T> work) throws SQLException {
        boolean owner = connection.getAutoCommit();
        Savepoint savepoint = null;
        if (owner) connection.setAutoCommit(false);
        else savepoint = connection.setSavepoint();
        Throwable failure = null;
        try {
            T result = work.run();
            if (owner) connection.commit();
            else connection.releaseSavepoint(savepoint);
            return result;
        } catch (SQLException | RuntimeException | Error exception) {
            failure = exception;
            try {
                if (owner) connection.rollback();
                else { connection.rollback(savepoint); connection.releaseSavepoint(savepoint); }
            } catch (SQLException rollbackFailure) { exception.addSuppressed(rollbackFailure); }
            throw exception;
        } finally {
            if (owner) {
                try { connection.setAutoCommit(true); }
                catch (SQLException restoreFailure) {
                    if (failure != null) failure.addSuppressed(restoreFailure);
                    else throw restoreFailure;
                }
            }
        }
    }

    @FunctionalInterface
    public interface Work<T> { T run() throws SQLException; }
}
