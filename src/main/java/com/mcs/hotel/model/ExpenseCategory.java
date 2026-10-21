package com.mcs.hotel.model;

/** What an expense was spent on. The label is what people see on screen. */
public enum ExpenseCategory {
    STAFF("Staff"),
    UTILITIES("Utilities"),
    SUPPLIES("Supplies"),
    MAINTENANCE("Maintenance"),
    OTHER("Other");

    private final String label;

    ExpenseCategory(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}