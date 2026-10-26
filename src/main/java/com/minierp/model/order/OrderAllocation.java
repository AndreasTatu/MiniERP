package com.minierp.model.order;

public record OrderAllocation(int allocationID, int orderID, int stockID, int productID,
                              String location, int quantity, State state) {
    public enum State { RESERVED, SHIPPED, RELEASED }
}
