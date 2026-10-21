package com.mcs.hotel.model;

public class Room {
    private final String number;
    private final RoomType type;
    private boolean occupied;

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() { return number; }
    public RoomType getType() { return type; }
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }
}