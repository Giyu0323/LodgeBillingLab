package com.mcs.hotel.model;

public enum Role {
    ADMIN("Admin"),
    STAFF("Staff");

    private final String label;

    Role(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}