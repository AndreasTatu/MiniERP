package com.minierp.model.order;

/** One manually chosen picking location and quantity. */
public record StockSelection(int stockID, int quantity) {}
