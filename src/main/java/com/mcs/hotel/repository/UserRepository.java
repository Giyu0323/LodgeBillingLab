package com.mcs.hotel.repository;

import com.mcs.hotel.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void deleteByUsername(String username);
}