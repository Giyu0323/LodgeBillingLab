package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.service.PaymentService;
import com.mcs.hotel.util.BillFilter;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.GuestPaymentsPanel;
import com.mcs.hotel.view.PaymentDialog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Feeds the Guest Payments page and handles its Pay Now button.
 * Pay Now asks the guest, on their phone, to pay the whole balance, using the
 * same payment window as a new bill. The totals shown always match the rows shown.
 */
public class GuestPaymentsController implements Searchable {

    private final GuestPaymentsPanel view;
    private final AppServices services;
    private String filter = ""; // current search text from the top bar

    /** @param onView supplied by DashboardController, which opens the receipt */
    public GuestPaymentsController(GuestPaymentsPanel view, AppServices services, Consumer<Bill> onView) {
        this.view = view;
        this.services = services;
        view.onPayNow(this::payNow);
        view.onViewDetails(onView);
        view.onFiltersChanged(this::refresh);
        refresh();
    }

    @Override
    public void setFilter(String text) {
        filter = text == null ? "" : text;
        refresh();
    }

    /** Re-reads the bills, applies the search and the date filters, and updates everything. */
    private void refresh() {
        List<Bill> shown = BillFilter.apply(services.billing().getBills(), filter).stream()
                .filter(this::inDateRange)
                .toList();

        double paid = shown.stream().mapToDouble(Bill::amountPaid).sum();
        double due = shown.stream().mapToDouble(Bill::balance).sum();

        view.setCounts(services.billing().checkedInCount(), services.billing().checkedOutCount());
        view.setBills(shown);
        view.setTotals(shown.size(), MoneyFormat.k(paid), MoneyFormat.k(due),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm")));
    }

    /** True if the bill fits the two date filters. An empty filter lets everything through. */
    private boolean inDateRange(Bill bill) {
        LocalDate from = view.getFrom();
        LocalDate until = view.getUntil();
        if (from != null && bill.checkInDate().isBefore(from)) {
            return false;
        }
        return until == null || !bill.checkOutTime().toLocalDate().isAfter(until);
    }

    /** Pay Now: ask the guest's phone to approve the whole balance, then add it to the bill. */
    private void payNow(Bill bill) {
        double amount = Math.round(bill.balance() * 100) / 100.0; // exactly what is owed, to the ngwee
        PaymentService payments = services.billing().payments();

        boolean paid = PaymentDialog.show(payments.enabledMethods(), MoneyFormat.k(amount),
                new PaymentFlow(payments, amount, payment -> {
                    try {
                        services.billing().addPayment(bill.id(), payment);
                        return null; // null = success
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        return ex.getMessage(); // shown inside the window
                    }
                }));

        if (paid) {
            refresh(); // the row turns green and the totals update
        }
    }
}