package com.minierp.auth;

import java.util.EnumSet;
import java.util.Set;
import static com.minierp.auth.Role.Permission.*;

public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    SALES(EnumSet.of(CUSTOMERS_READ, CUSTOMERS_WRITE, PRODUCTS_READ, ORDERS_READ, ORDERS_CREATE, CANCEL_OPEN, STOCKS_READ)),
    WAREHOUSE(EnumSet.of(PRODUCTS_READ, ORDERS_READ, CONFIRM, SHIP, DELIVER, CANCEL_CONFIRMED, STOCKS_READ, STOCKS_WRITE));

    public enum Permission {
        CUSTOMERS_READ, CUSTOMERS_WRITE, PRODUCTS_READ, PRODUCTS_WRITE, ORDERS_READ, ORDERS_CREATE,
        CONFIRM, SHIP, DELIVER, CANCEL_OPEN, CANCEL_CONFIRMED, STOCKS_READ, STOCKS_WRITE, USERS_MANAGE
    }

    public final Set<Permission> permissions;
    Role(Set<Permission> permissions) { this.permissions = Set.copyOf(permissions); }
}
