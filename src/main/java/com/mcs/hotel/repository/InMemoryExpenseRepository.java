package com.mcs.hotel.repository;

import com.mcs.hotel.model.Expense;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Keeps expenses in memory, like bills and users: they reset when the app closes. */
public class InMemoryExpenseRepository implements ExpenseRepository {

    private final Map<String, Expense> expenses = new LinkedHashMap<>();

    @Override
    public Expense save(Expense expense) {
        expenses.put(expense.id(), expense);
        return expense;
    }

    @Override
    public List<Expense> findAll() {
        return List.copyOf(expenses.values());
    }

    @Override
    public Optional<Expense> findById(String id) {
        return Optional.ofNullable(expenses.get(id));
    }

    @Override
    public void deleteById(String id) {
        expenses.remove(id);
    }
}