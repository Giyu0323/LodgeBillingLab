package com.mcs.hotel.service;

import com.mcs.hotel.model.Expense;
import com.mcs.hotel.model.ExpenseCategory;
import com.mcs.hotel.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * The rules for recording money spent. Analytics (a later step) reads its
 * expenses from here, so profit is always worked out from real records.
 */
public class ExpenseService {

    private final ExpenseRepository repository;
    private int counter = 1;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    /**
     * Records an expense. Past dates are allowed (so earlier months can be
     * entered); dates in the future are not. Throws a friendly message if
     * anything is wrong.
     */
    public Expense add(LocalDate date, ExpenseCategory category, String description, double amount) {
        if (date == null) {
            throw new IllegalArgumentException("Choose the date of the expense.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The date cannot be in the future.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Choose a category.");
        }
        String text = description == null ? "" : description.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Enter a short description.");
        }
        if (text.length() > 80) {
            throw new IllegalArgumentException("The description can be at most 80 characters.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("The amount must be more than zero.");
        }
        if (BigDecimal.valueOf(amount).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("The amount can have at most 2 decimal places.");
        }
        return repository.save(new Expense(
                String.format("E%03d", counter++), date, category, text, amount));
    }

    public void delete(String id) {
        if (repository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Expense " + id + " not found.");
        }
        repository.deleteById(id);
    }

    /** Every expense, newest date first (and the most recently added first within a day). */
    public List<Expense> getAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Expense::date).thenComparing(Expense::id).reversed())
                .toList();
    }
}