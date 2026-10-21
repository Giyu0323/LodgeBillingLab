package com.mcs.hotel.repository;

import com.mcs.hotel.model.User;

import java.util.*;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();

    private String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    @Override
    public User save(User user) {
        users.put(key(user.getUsername()), user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(key(username)));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public void deleteByUsername(String username) {
        users.remove(key(username));
    }
}