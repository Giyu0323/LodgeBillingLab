package com.mcs.hotel.repository;

import com.mcs.hotel.model.Expense;

import java.util.List;
import java.util.Optional;

/** Where expenses are kept. A database could replace the in-memory version later. */
public interface ExpenseRepository {
    Expense save(Expense expense);
    List<Expense> findAll();
    Optional<Expense> findById(String id);
    void deleteById(String id);
}