package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.model.BillExtra;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.Dialogs;
import com.mcs.hotel.view.ReceiptPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one saved Bill into receipt text and handles Print and Back.
 * Everything shown comes from the bill's own saved snapshot, so later price
 * edits never change an old receipt.
 */
public class ReceiptController {

    /**
     * @param onBack supplied by DashboardController: it knows which screen
     *               the user came from.
     */
    public ReceiptController(ReceiptPanel view, Bill bill, Runnable onBack) {
        view.setHeader(bill.id(),
                bill.checkInDate().toString(),
                bill.guestName(),
                bill.roomNumber(),
                bill.roomType().toString(),
                String.valueOf(bill.nights()),
                bill.notes());
        view.setLines(buildLines(bill));
        view.setTotals(MoneyFormat.k(bill.total()),
                MoneyFormat.k(bill.amountPaid()),
                MoneyFormat.k(bill.balance()));

        view.getBackButton().setOnAction(e -> onBack.run());
        view.getPrintButton().setOnAction(e -> {
            if (!view.print()) {
                Dialogs.error("No printer was found on this computer.");
            }
        });
    }

    /** Room charge, then the discount (only if one applied), then one line per extra. */
    private List<ReceiptPanel.Line> buildLines(Bill bill) {
        List<ReceiptPanel.Line> lines = new ArrayList<>();

        lines.add(new ReceiptPanel.Line(
                "Room charge (" + bill.roomType() + ")",
                String.valueOf(bill.nights()),
                MoneyFormat.k(bill.roomRate()),
                MoneyFormat.k(bill.roomCharge())));

        if (bill.discount() > 0) {
            // Work the percentage out from the saved amounts, not from today's rule
            long percent = Math.round(bill.discount() / bill.roomCharge() * 100);
            lines.add(new ReceiptPanel.Line(percent + "% discount", "-", "-",
                    "- " + MoneyFormat.k(bill.discount())));
        }

        for (BillExtra extra : bill.extras()) {
            lines.add(new ReceiptPanel.Line(
                    extra.name(),
                    String.valueOf(extra.quantity()),
                    MoneyFormat.k(extra.unitPrice()),
                    MoneyFormat.k(extra.lineTotal())));
        }
        return lines;
    }
}