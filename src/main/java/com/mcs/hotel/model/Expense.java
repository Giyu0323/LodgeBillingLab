package com.mcs.hotel.model;

import java.time.LocalDate;

/** One amount of money spent running the lodge, on a given day. */
public record Expense(String id, LocalDate date, ExpenseCategory category,
                      String description, double amount) {}