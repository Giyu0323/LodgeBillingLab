package com.mcs.hotel.service;

import com.mcs.hotel.model.User;
import com.mcs.hotel.repository.UserRepository;
import com.mcs.hotel.util.PasswordHasher;

public class AuthService {

    private final UserRepository repository;

    public AuthService(UserRepository repository) {
        this.repository = repository;
    }

    /** Returns the user, or throws a message the login screen can display. */
    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Enter your username and password.");
        }
        // Same message for "no such user" and "wrong password", so nothing is leaked.
        return repository.findByUsername(username.trim())
                .filter(u -> PasswordHasher.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
    }
}