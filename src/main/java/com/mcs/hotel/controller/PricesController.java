package com.mcs.hotel.controller;

import com.mcs.hotel.model.ExtraType;
import com.mcs.hotel.model.RoomType;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.Dialogs;
import com.mcs.hotel.view.ExtraDialog;
import com.mcs.hotel.view.PricesPanel;

import java.util.Locale;

/**
 * Connects PricesPanel to PriceService.
 * PriceService holds the rules (price above 0, no duplicate extra names).
 * This class turns typed text into numbers, calls the service, and shows
 * the result or the service's error message.
 */
public class PricesController {

    private final PricesPanel view;
    private final AppServices services;

    public PricesController(PricesPanel view, AppServices services) {
        this.view = view;
        this.services = services;

        view.onSaveRate(this::saveRate);
        view.onEditExtra(this::editExtra);
        view.onRemoveExtra(this::removeExtra);
        view.getAddExtraButton().setOnAction(e -> addExtra());

        // Fill the screen with the current prices
        for (RoomType type : RoomType.values()) {
            view.setRate(type, plain(services.prices().getRate(type)));
        }
        refreshExtras();
    }

    // ---------- Room rates ----------

    private void saveRate(RoomType type, String text) {
        try {
            double price = parsePrice(text);
            services.prices().setRate(type, price);
            view.setRate(type, plain(price)); // tidies what was typed, e.g. "400" -> "400.00"
            view.showRateMessage(type + " rate saved: " + MoneyFormat.k(price) + " per night.", false);
        } catch (IllegalArgumentException ex) {
            view.showRateMessage(ex.getMessage(), true);
        }
    }

    // ---------- Extra charges ----------

    private void addExtra() {
        boolean saved = ExtraDialog.show("Add Extra", "", "", com.mcs.hotel.model.PricingBasis.PER_DAY,
                input -> {
                    try {
                        services.prices().addExtra(input.name(), parsePrice(input.price()), input.basis());
                        return null; // null = success, so the dialog closes
                    } catch (IllegalArgumentException ex) {
                        return ex.getMessage(); // shown inside the dialog
                    }
                });
        if (saved) {
            refreshExtras();
            view.showExtraMessage("Extra added.", false);
        }
    }

    private void editExtra(ExtraType extra) {
        boolean saved = ExtraDialog.show("Edit Extra", extra.getName(), plain(extra.getPrice()),
                extra.getBasis(),
                input -> {
                    try {
                        services.prices().updateExtra(extra, input.name(),
                                parsePrice(input.price()), input.basis());
                        return null;
                    } catch (IllegalArgumentException ex) {
                        return ex.getMessage();
                    }
                });
        if (saved) {
            refreshExtras();
            view.showExtraMessage("Extra updated.", false);
        }
    }

    private void removeExtra(ExtraType extra) {
        if (!Dialogs.confirmRemoveExtra(extra.getName())) {
            return; // the admin clicked Cancel
        }
        services.prices().removeExtra(extra);
        refreshExtras();
        view.showExtraMessage("Removed " + extra.getName() + ".", false);
    }

    private void refreshExtras() {
        view.setExtras(services.prices().getExtras());
    }

    // ---------- Helpers ----------

    /** Turns typed text into a price. The "above 0" rule itself is checked by PriceService. */
    private double parsePrice(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Enter a price.");
        }
        try {
            double value = Double.parseDouble(text.trim().replace(",", ""));
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Price must be a number greater than 0.");
        }
    }

    /** A price as plain text for a text box: no "K", no thousands comma. */
    private String plain(double price) {
        return String.format(Locale.US, "%.2f", price);
    }
}