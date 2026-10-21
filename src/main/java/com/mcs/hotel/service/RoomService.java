package com.mcs.hotel.service;

import com.mcs.hotel.model.Room;
import com.mcs.hotel.model.RoomType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RoomService {

    private final List<Room> rooms = new ArrayList<>();

    public RoomService() {
        for (int i = 1; i <= 5; i++) rooms.add(new Room("S-10" + i, RoomType.STANDARD));
        for (int i = 1; i <= 4; i++) rooms.add(new Room("D-20" + i, RoomType.DELUXE));
        for (int i = 1; i <= 3; i++) rooms.add(new Room("F-30" + i, RoomType.FAMILY));
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public int totalCount() {
        return rooms.size();
    }

    public int occupiedCount() {
        return (int) rooms.stream().filter(Room::isOccupied).count();
    }

    public int availableCount() {
        return totalCount() - occupiedCount();
    }

    public int availableCount(RoomType type) {
        return (int) rooms.stream()
                .filter(r -> r.getType() == type && !r.isOccupied())
                .count();
    }

    /** Marks the first free room of this type as occupied and returns it. */
    public Room assignRoom(RoomType type) {
        Optional<Room> free = rooms.stream()
                .filter(r -> r.getType() == type && !r.isOccupied())
                .findFirst();
        Room room = free.orElseThrow(() ->
                new IllegalStateException("No " + type + " rooms are available right now."));
        room.setOccupied(true);
        return room;
    }

    /** Frees a room (used when a bill is deleted). */
    public void releaseRoom(String roomNumber) {
        rooms.stream()
                .filter(r -> r.getNumber().equals(roomNumber))
                .findFirst()
                .ifPresent(r -> r.setOccupied(false));
    }
}