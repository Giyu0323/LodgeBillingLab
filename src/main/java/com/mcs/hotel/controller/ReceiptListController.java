package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.BillFilter;
import com.mcs.hotel.view.ReceiptListPanel;

import java.util.function.Consumer;

/** Feeds the View Receipts list and passes a clicked bill on to be opened. */
public class ReceiptListController implements Searchable {

    private final ReceiptListPanel view;
    private final AppServices services;
    private String filter = ""; // current search text from the top bar

    /** @param onView supplied by DashboardController, which opens the receipt */
    public ReceiptListController(ReceiptListPanel view, AppServices services,
                                 Consumer<Bill> onView) {
        this.view = view;
        this.services = services;
        view.onViewBill(onView);
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
}