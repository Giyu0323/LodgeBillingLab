package com.mcs.hotel.model;

public enum RoomType {
    STANDARD("Standard"),
    DELUXE("Deluxe"),
    FAMILY("Family");

    private final String label;

    RoomType(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}