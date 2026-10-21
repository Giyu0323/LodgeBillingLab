package com.mcs.hotel.service;

import com.mcs.hotel.model.Role;
import com.mcs.hotel.model.User;
import com.mcs.hotel.repository.UserRepository;
import com.mcs.hotel.util.PasswordHasher;

import java.util.List;

public class UserService {

    public static final String DEFAULT_ADMIN = "admin";
    public static final String DEFAULT_PASSWORD = "admin123";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    /** Called on every launch, because everything is in memory. Safe to call twice. */
    public void seedDefaultAdmin() {
        if (repository.findByUsername(DEFAULT_ADMIN).isEmpty()) {
            addUser(DEFAULT_ADMIN, DEFAULT_PASSWORD, DEFAULT_PASSWORD, Role.ADMIN);
        }
    }

    public User addUser(String username, String password, String confirm, Role role) {
        String name = username == null ? "" : username.trim();
        if (!name.matches("\\S{3,}")) {
            throw new IllegalArgumentException(
                    "Username must be at least 3 characters with no spaces.");
        }
        if (repository.findByUsername(name).isPresent()) {
            throw new IllegalArgumentException("Username \"" + name + "\" is already taken.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Please choose a role.");
        }
        return repository.save(new User(name, PasswordHasher.hash(password), role));
    }

    public void deleteUser(String username, String currentUsername) {
        User target = repository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (target.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        long admins = repository.findAll().stream().filter(User::isAdmin).count();
        if (target.isAdmin() && admins <= 1) {
            throw new IllegalArgumentException("The last admin cannot be deleted.");
        }
        repository.deleteByUsername(target.getUsername());
    }

    public List<User> getUsers() {
        return repository.findAll();
    }
}