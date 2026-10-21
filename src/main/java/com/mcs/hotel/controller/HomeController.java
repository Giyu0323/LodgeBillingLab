package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.model.RoomType;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.service.BillingService;
import com.mcs.hotel.util.BillFilter;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.Dialogs;
import com.mcs.hotel.view.HomePanel;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Feeds HomePanel with live numbers and the Saved Bills list, and handles
 * the View and Delete buttons on each row.
 * The stats are calculated on demand, never stored, so refresh() is the only
 * thing that needs calling after a bill is saved or deleted.
 */
public class HomeController implements Searchable {

    private final HomePanel view;
    private final AppServices services;
    private String filter = ""; // search text from the top bar

    /** @param onView supplied by DashboardController, which opens the receipt */
    public HomeController(HomePanel view, AppServices services, Consumer<Bill> onView) {
        this.view = view;
        this.services = services;
        view.onViewBill(onView);
        view.onDeleteBill(this::deleteBill);
        refresh(); // show current numbers as soon as the panel opens
    }

    /** Re-reads everything from the services and updates the screen. */
    public void refresh() {
        // Stat cards
        view.setRooms(services.rooms().totalCount(),
                services.rooms().occupiedCount(),
                services.rooms().availableCount());
        view.setTodaysGuests(services.billing().todaysGuests());
        view.setRevenue(MoneyFormat.k(services.billing().totalRevenue()));
        view.setBillCount(services.billing().billCount(),
                LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)));

        // Saved Bills table
        refreshBills();

        // Room Prices: current rates, in the order Standard, Deluxe, Family
        Map<String, String> prices = new LinkedHashMap<>();
        for (RoomType type : RoomType.values()) {
            prices.put(type.toString(),
                    MoneyFormat.k(services.prices().getRate(type)));
        }
        view.setRoomPrices(prices);

        // Quick Info: built from the real constants, so the text can't go out of date
        int percent = (int) Math.round(BillingService.DISCOUNT_RATE * 100);
        view.setQuickInfo(List.of(
                BillingService.DISCOUNT_MIN_NIGHTS + " nights or more: " + percent
                        + "% off the room charge.",
                "Extras get no discount.",
                "Saved bills last only until you close the app."));
    }

    /** Called by DashboardController whenever the top-bar search text changes. */
    @Override
    public void setFilter(String text) {
        filter = text == null ? "" : text;
        refreshBills();
    }

    private void refreshBills() {
        view.setBills(BillFilter.apply(services.billing().getBills(), filter));
    }

    /** Delete button on a row: confirm, delete (frees the room), then refresh everything. */
    private void deleteBill(Bill bill) {
        if (!Dialogs.confirmDeleteBill(bill)) {
            return; // the user clicked Cancel
        }
        try {
            services.billing().deleteBill(bill.id());
        } catch (IllegalArgumentException ex) {
            Dialogs.error(ex.getMessage());
        }
        refresh(); // full refresh, so the stat cards change too
    }
}