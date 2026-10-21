package com.mcs.hotel.controller;

import com.mcs.hotel.model.Payment;
import com.mcs.hotel.model.PaymentDetails;
import com.mcs.hotel.model.PaymentMethod;
import com.mcs.hotel.model.PaymentRequest;
import com.mcs.hotel.model.PaymentStatus;
import com.mcs.hotel.service.PaymentService;
import com.mcs.hotel.view.PaymentDialog;

import java.util.function.Function;

/**
 * Connects the payment window to PaymentService for ONE amount.
 * The only thing that differs between "pay a balance" and other payments is
 * what happens once the guest approves, so that is passed in as "onApproved".
 */
public class PaymentFlow implements PaymentDialog.Flow {

    private final PaymentService payments;
    private final double amount;
    private final Function<Payment, String> onApproved; // returns an error message, or null on success

    public PaymentFlow(PaymentService payments, double amount, Function<Payment, String> onApproved) {
        this.payments = payments;
        this.amount = amount;
        this.onApproved = onApproved;
    }

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
        payments.guestResponds(request, approve); // DEMO stand-in for the guest's phone
    }

    @Override
    public void cancel(PaymentRequest request) {
        payments.cancel(request);
    }

    @Override
    public String finish(PaymentRequest request) {
        try {
            Payment payment = payments.complete(request); // the guest approved
            return onApproved.apply(payment);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ex.getMessage(); // shown inside the window
        }
    }
}