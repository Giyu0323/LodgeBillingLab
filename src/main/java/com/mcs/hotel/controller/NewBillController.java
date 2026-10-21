package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.model.BillSummary;
import com.mcs.hotel.model.ExtraSelection;
import com.mcs.hotel.model.ExtraType;
import com.mcs.hotel.model.Payment;
import com.mcs.hotel.model.PaymentDetails;
import com.mcs.hotel.model.PaymentMethod;
import com.mcs.hotel.model.PaymentRequest;
import com.mcs.hotel.model.PaymentStatus;
import com.mcs.hotel.model.PricingBasis;
import com.mcs.hotel.model.RoomType;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.service.BillingService;
import com.mcs.hotel.service.PaymentService;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.view.NewBillPanel;
import com.mcs.hotel.view.PaymentDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Connects NewBillPanel to BillingService.
 * The service does all the maths and rule checks. This class only reads the
 * form, calls the service, and shows the result or the error message.
 *
 * Pay Later saves the bill with nothing paid. Pay Now first checks the bill and
 * the amount, then opens the payment window; a request goes to the guest's
 * phone, and the bill is saved only after the guest approves it there.
 */
public class NewBillController {

    private final NewBillPanel view;
    private final AppServices services;

    public NewBillController(NewBillPanel view, AppServices services) {
        this.view = view;
        this.services = services;

        // Build the checklist from the CURRENT prices (admins can edit them)
        view.setExtras(services.prices().getExtras());

        view.onInputChanged(this::refreshLive);
        view.getClearButton().setOnAction(e -> {
            view.clear();
            view.clearMessage();
        });
        view.getCalculateButton().setOnAction(e -> calculate());
        view.getPayNowButton().setOnAction(e -> payNow());
        view.getPayLaterButton().setOnAction(e -> payLater());

        refreshLive(); // fill the summary and room hint straight away
    }

    /** Runs on every keystroke or tick. Stays silent if the input is half-typed. */
    private void refreshLive() {
        RoomType type = view.getRoomType();
        int free = services.rooms().availableCount(type);
        view.setRoomHint(free == 0
                ? "No " + type + " rooms are available right now."
                : free + " " + type + " room(s) available");

        try {
            updateSummary();
        } catch (IllegalArgumentException ex) {
            // e.g. "12x" typed in the amount box: keep the last numbers, show no error yet
        }
    }

    /** The Calculate button: same maths as the live summary, but errors are shown. */
    private void calculate() {
        try {
            updateSummary();
            view.clearMessage();
        } catch (IllegalArgumentException ex) {
            view.showMessage(ex.getMessage(), true);
        }
    }

    /** Asks BillingService for the numbers and puts them on screen. Saves nothing. */
    private void updateSummary() {
        BillSummary summary = services.billing()
                .calculate(view.getRoomType(), view.getNights(), collectExtras());
        double paying = parseMoney(view.getPaidText(), "Amount to pay now");

        view.setSummary(
                MoneyFormat.k(summary.roomCharge()),
                "- " + MoneyFormat.k(summary.discount()),
                MoneyFormat.k(summary.extrasTotal()),
                MoneyFormat.k(summary.total()),
                MoneyFormat.k(paying),
                MoneyFormat.k(summary.total() - paying));

        long percent = Math.round(BillingService.MIN_DEPOSIT_RATE * 100);
        view.setDepositHint("Minimum to pay now: "
                + MoneyFormat.k(services.billing().minimumDeposit(summary.total()))
                + " (" + percent + "% of the total). Or choose Pay Later.");
    }

    /** Pay Later: the bill is saved with nothing paid, so the whole total is the balance. */
    private void payLater() {
        try {
            Bill bill = services.billing().createBill(
                    view.getGuestName(), view.getRoomType(), view.getNights(),
                    collectExtras(), null, view.getNotes());
            afterSaved("Saved " + bill.id() + " for " + bill.guestName() + " in room "
                    + bill.roomNumber() + ". No payment taken: " + MoneyFormat.k(bill.balance())
                    + " is due.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            view.showMessage(ex.getMessage(), true);
        }
    }

    /**
     * Pay Now. Everything that can be checked is checked BEFORE the payment
     * window opens, so nobody is asked to pay for a bill that cannot be saved.
     * The window then sends a request to the guest's phone and waits; the bill
     * is created only after the guest approves.
     */
    private void payNow() {
        try {
            // Read the form once, so the window and the save both use the same values
            String guest = view.getGuestName();
            RoomType type = view.getRoomType();
            int nights = view.getNights();
            List<ExtraSelection> extras = collectExtras();
            String notes = view.getNotes();

            BillSummary summary = services.billing().checkNewBill(guest, type, nights, extras);
            double amount = parseMoney(view.getPaidText(), "Amount to pay now");
            services.billing().checkPayNowAmount(amount, summary.total());

            PaymentService payments = services.billing().payments();
            Bill[] saved = new Bill[1]; // filled in when the guest approves

            // The window asks for these five things; each one is a single call to the service
            boolean paid = PaymentDialog.show(payments.enabledMethods(), MoneyFormat.k(amount),
                    new PaymentDialog.Flow() {
                        @Override
                        public PaymentRequest send(PaymentMethod method, PaymentDetails details) {
                            return payments.send(method, amount, details);
                        }

                        @Override
                        public PaymentStatus check(PaymentRequest request) {
                            return payments.check(request);
                        }

                        @Override
                        public void guestResponds(PaymentRequest request, boolean approve) {
                            payments.guestResponds(request, approve); // demo stand-in for the guest's phone
                        }

                        @Override
                        public void cancel(PaymentRequest request) {
                            payments.cancel(request);
                        }

                        @Override
                        public String finish(PaymentRequest request) {
                            try {
                                Payment payment = payments.complete(request);
                                saved[0] = services.billing().createBill(
                                        guest, type, nights, extras, payment, notes);
                                return null; // null = success
                            } catch (IllegalArgumentException | IllegalStateException ex) {
                                return ex.getMessage(); // shown inside the window
                            }
                        }
                    });

            if (paid) {
                Bill bill = saved[0];
                Payment payment = bill.payments().get(0);
                afterSaved("Saved " + bill.id() + " for " + bill.guestName() + " in room "
                        + bill.roomNumber() + ". Paid " + MoneyFormat.k(payment.amount())
                        + " by " + payment.method() + " (ref " + payment.reference()
                        + ", approved by the guest on their phone). Balance "
                        + MoneyFormat.k(bill.balance()) + ".");
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            view.showMessage(ex.getMessage(), true);
        }
    }

    /** After a successful save: empty the form and show the green message. */
    private void afterSaved(String message) {
        view.clear();
        refreshLive(); // the room count has changed, and the form is empty again
        view.showMessage(message, false);
    }

    /** The ticked extras, plus the optional "Other (K)" amount as a one-time extra. */
    private List<ExtraSelection> collectExtras() {
        List<ExtraSelection> list = new ArrayList<>(view.getSelectedExtras());
        double other = parseMoney(view.getOtherText(), "Other charge");
        if (other > 0) {
            // Not in the price list, so it exists only on this bill
            list.add(new ExtraSelection(new ExtraType("Other", other, PricingBasis.ONE_TIME), 1));
        }
        return list;
    }

    /** Turns typed text into a number. A blank box counts as 0. */
    private double parseMoney(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            double value = Double.parseDouble(text.trim().replace(",", ""));
            if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number, 0 or more.");
        }
    }
}