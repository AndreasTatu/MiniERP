package model.order;

public enum OrderStatus {
    PENDING("Ausstehend"),
    CONFIRMED("Bestätigt"),
    SHIPPED("Versendet"),
    DELIVERED("Geliefert"),
    CANCELLED("Storniert");


    private final String displayName;

    OrderStatus(String displayName){
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}