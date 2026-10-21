package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.BillFilter;
import com.mcs.hotel.view.Dialogs;
import com.mcs.hotel.view.StayDialogs;
import com.mcs.hotel.view.StayPanel;

import java.util.List;
import java.util.function.Consumer;

/**
 * Feeds a StayPanel with either the guests staying now or the guests who left,
 * and handles the Check Out button. BillingService holds the rules (for
 * example: no check-out while money is owed).
 */
public class StayController implements Searchable {

    private final StayPanel view;
    private final AppServices services;
    private final boolean checkedIn;
    private String filter = ""; // current search text from the top bar

    /**
     * @param checkedIn true for the Checked-In page, false for the Checked-Out page
     * @param onView    supplied by DashboardController, which opens the receipt
     */
    public StayController(StayPanel view, AppServices services, boolean checkedIn, Consumer<Bill> onView) {
        this.view = view;
        this.services = services;
        this.checkedIn = checkedIn;
        view.onView(onView);
        view.onCheckOut(this::checkOut);
        refresh();
    }

    @Override
    public void setFilter(String text) {
        filter = text == null ? "" : text;
        refresh();
    }

    private void refresh() {
        List<Bill> all = checkedIn ? services.billing().getCheckedIn() : services.billing().getCheckedOut();
        view.setBills(BillFilter.apply(all, filter));
    }

    private void checkOut(Bill bill) {
        if (!StayDialogs.confirmCheckOut(bill)) {
            return; // the user clicked Cancel
        }
        try {
            services.billing().checkOut(bill.id()); // records the time and frees the room
        } catch (IllegalArgumentException ex) {
            Dialogs.error(ex.getMessage()); // e.g. "John still owes K 1,812.50..."
        }
        refresh();
    }
}