package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.BillFilter;
import com.mcs.hotel.view.DeleteBillPanel;
import com.mcs.hotel.view.Dialogs;

import java.util.function.Consumer;

/**
 * Connects DeleteBillPanel to BillingService.
 * Flow: Delete clicked -> ask "are you sure?" -> service deletes and frees
 * the room -> reload the list. View just opens the bill's receipt.
 */
public class DeleteBillController implements Searchable {

    private final DeleteBillPanel view;
    private final AppServices services;
    private String filter = ""; // current search text from the top bar

    /** @param onView supplied by DashboardController, which opens the receipt */
    public DeleteBillController(DeleteBillPanel view, AppServices services,
                                Consumer<Bill> onView) {
        this.view = view;
        this.services = services;
        view.onViewBill(onView);
        view.onDeleteBill(this::deleteBill);
        refresh();
    }

    @Override
    public void setFilter(String text) {
        filter = text == null ? "" : text;
        refresh();
    }

    private void refresh() {
        view.setBills(BillFilter.apply(services.billing().getBills(), filter));
    }

    private void deleteBill(Bill bill) {
        if (!Dialogs.confirmDeleteBill(bill)) {
            return; // the user clicked Cancel
        }
        try {
            services.billing().deleteBill(bill.id()); // also frees the room
        } catch (IllegalArgumentException ex) {
            Dialogs.error(ex.getMessage()); // e.g. the bill was already gone
        }
        refresh();
    }
}