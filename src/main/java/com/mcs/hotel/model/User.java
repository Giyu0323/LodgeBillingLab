package com.mcs.hotel.model;

import java.time.LocalDate;

public class User {
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final LocalDate createdOn;

    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdOn = LocalDate.now();
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public LocalDate getCreatedOn() { return createdOn; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}