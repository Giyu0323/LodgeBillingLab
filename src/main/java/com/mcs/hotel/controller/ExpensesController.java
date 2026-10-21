package com.mcs.hotel.controller;

import com.mcs.hotel.model.Expense;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.Dialogs;
import com.mcs.hotel.view.ExpenseDialogs;
import com.mcs.hotel.view.ExpensesPanel;

import java.util.List;
import java.util.Locale;

/**
 * Connects ExpensesPanel to ExpenseService.
 * ExpenseService holds the rules (date, description, amount). This class turns
 * typed text into a number, calls the service, and shows the result or the
 * service's error message.
 */
public class ExpensesController implements Searchable {

    private final ExpensesPanel view;
    private final AppServices services;
    private String filter = ""; // lower-case search text from the top bar

    public ExpensesController(ExpensesPanel view, AppServices services) {
        this.view = view;
        this.services = services;
        view.getAddButton().setOnAction(e -> addExpense());
        view.onDelete(this::deleteExpense);
        refresh();
    }

    /** Top-bar search: filters the list by description or category. */
    @Override
    public void setFilter(String text) {
        filter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        refresh();
    }

    private void refresh() {
        List<Expense> shown = services.expenses().getAll().stream()
                .filter(e -> filter.isEmpty()
                        || e.description().toLowerCase(Locale.ROOT).contains(filter)
                        || e.category().toString().toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        double total = shown.stream().mapToDouble(Expense::amount).sum();

        view.setExpenses(shown);
        view.setTotal("Total expenses: " + MoneyFormat.k(total) + "  (" + shown.size() + " shown)");
    }

    private void addExpense() {
        try {
            double amount = parseAmount(view.getAmountText());
            Expense saved = services.expenses().add(
                    view.getDate(), view.getCategory(), view.getDescription(), amount);
            view.clearForm();
            refresh();
            view.showMessage("Added " + saved.id() + ": " + saved.description()
                    + " (" + MoneyFormat.k(saved.amount()) + ").", false);
        } catch (IllegalArgumentException ex) {
            // The service's message is already user-friendly, so show it as is.
            // The form is left alone so it can be corrected.
            view.showMessage(ex.getMessage(), true);
        }
    }

    private void deleteExpense(Expense expense) {
        if (!ExpenseDialogs.confirmDelete(expense)) {
            return; // the admin clicked Cancel
        }
        try {
            services.expenses().delete(expense.id());
        } catch (IllegalArgumentException ex) {
            Dialogs.error(ex.getMessage());
        }
        refresh();
    }

    /** Turns typed text into a number. The "above zero" rule itself is checked by the service. */
    private double parseAmount(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter the amount.");
        }
        try {
            double value = Double.parseDouble(text.trim().replace(",", ""));
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("The amount must be a number.");
        }
    }
}