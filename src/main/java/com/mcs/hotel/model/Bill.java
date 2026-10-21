package com.mcs.hotel.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One guest's bill and stay. It is a SNAPSHOT: the room rate, discount and
 * extras are copied in when it is created, so later price changes never alter it.
 * What has been paid is the sum of its payments, so the two can never disagree.
 * A guest is "checked in" from the moment the bill is saved until checkedOutAt is set.
 */
public record Bill(
        String id,
        String guestName,
        String roomNumber,
        RoomType roomType,
        int nights,
        LocalDateTime checkIn,
        double roomRate,
        double discount,
        List<BillExtra> extras,
        List<Payment> payments,
        String notes,
        LocalDateTime checkedOutAt) { // null while the guest is still staying

    /** Every guest is expected to leave at this time on their last day. */
    public static final LocalTime CHECK_OUT_TIME = LocalTime.of(11, 0);

    public Bill {
        extras = List.copyOf(extras);
        payments = List.copyOf(payments);
    }

    // ---------- The stay ----------

    /** Just the date part of the check-in (used on receipts and for "today's guests"). */
    public LocalDate checkInDate() {
        return checkIn.toLocalDate();
    }

    /** Check-in day plus the nights, at the standard check-out time. */
    public LocalDateTime expectedCheckOut() {
        return checkIn.toLocalDate().plusDays(nights).atTime(CHECK_OUT_TIME);
    }

    public boolean isCheckedOut() {
        return checkedOutAt != null;
    }

    /** The real check-out time if the guest has left, otherwise the expected one. */
    public LocalDateTime checkOutTime() {
        return checkedOutAt != null ? checkedOutAt : expectedCheckOut();
    }

    // ---------- The money ----------

    public double roomCharge() {
        return roomRate * nights;
    }

    public double extrasTotal() {
        return extras.stream().mapToDouble(BillExtra::lineTotal).sum();
    }

    public double total() {
        return roomCharge() - discount + extrasTotal();
    }

    /** Everything paid so far, across all payments. */
    public double amountPaid() {
        return payments.stream().mapToDouble(Payment::amount).sum();
    }

    public double balance() {
        return total() - amountPaid();
    }

    /** True when nothing is owed. A tiny tolerance avoids decimal rounding surprises. */
    public boolean isFullyPaid() {
        return balance() < 0.005;
    }

    // ---------- Changed copies (a Bill itself never changes) ----------

    /** A copy of this bill with one more payment added. */
    public Bill withPayment(Payment payment) {
        List<Payment> all = new ArrayList<>(payments);
        all.add(payment);
        return new Bill(id, guestName, roomNumber, roomType, nights, checkIn,
                roomRate, discount, extras, all, notes, checkedOutAt);
    }

    /** A copy of this bill with the guest checked out at the given time. */
    public Bill withCheckOut(LocalDateTime when) {
        return new Bill(id, guestName, roomNumber, roomType, nights, checkIn,
                roomRate, discount, extras, payments, notes, when);
    }
}