package com.minierp.common.exceptions;

/** The requested change conflicts with current stock or committed reservations. */
public class StockConflictException extends RuntimeException {
    public StockConflictException(String message) { super(message); }
}
