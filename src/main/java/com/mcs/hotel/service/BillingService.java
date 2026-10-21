package com.mcs.hotel.service;

import com.mcs.hotel.model.*;
import com.mcs.hotel.repository.BillRepository;
import com.mcs.hotel.util.MoneyFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The billing rules: totals, the 5% discount, the minimum deposit, room
 * assignment, paying a balance later, checking guests out, and saving or
 * deleting bills.
 */
public class BillingService {

    public static final int DISCOUNT_MIN_NIGHTS = 5;
    public static final double DISCOUNT_RATE = 0.05;

    /**
     * The smallest amount that can be paid NOW, as a share of the total.
     * 0.30 means 30%. Set it to 0 to switch the rule off.
     */
    public static final double MIN_DEPOSIT_RATE = 0.30;

    private final BillRepository repository;
    private final RoomService roomService;
    private final PriceService priceService;
    private final PaymentService paymentService = new PaymentService();
    private int counter = 1;

    public BillingService(BillRepository repository, RoomService roomService,
                          PriceService priceService) {
        this.repository = repository;
        this.roomService = roomService;
        this.priceService = priceService;
    }

    /** The payment service, shared so reference numbers (TXN-0001, ...) never repeat. */
    public PaymentService payments() {
        return paymentService;
    }

    // ---------- Working out a bill ----------

    /** Used by the Calculate button and the live summary. Saves nothing. */
    public BillSummary calculate(RoomType type, int nights, List<ExtraSelection> selections) {
        if (nights < 1) {
            throw new IllegalArgumentException("Nights must be at least 1.");
        }
        double roomCharge = priceService.getRate(type) * nights;
        double discount = nights >= DISCOUNT_MIN_NIGHTS ? roomCharge * DISCOUNT_RATE : 0;

        double extrasTotal = 0;
        for (ExtraSelection s : selections) {
            validateSelection(s, nights);
            extrasTotal += s.extra().getPrice() * s.quantity();
        }
        return new BillSummary(roomCharge, discount, extrasTotal);
    }

    /**
     * Checks that a new bill could be saved, WITHOUT saving anything.
     * The screen calls this before opening the payment window, so nobody is
     * asked to pay for a bill that cannot be created.
     */
    public BillSummary checkNewBill(String guestName, RoomType type, int nights,
                                    List<ExtraSelection> selections) {
        if (guestName == null || guestName.isBlank()) {
            throw new IllegalArgumentException("Guest name cannot be empty.");
        }
        BillSummary summary = calculate(type, nights, selections);
        if (roomService.availableCount(type) == 0) {
            throw new IllegalStateException("No " + type + " rooms are available right now.");
        }
        return summary;
    }

    // ---------- Paying ----------

    /** The smallest amount allowed for Pay Now, rounded UP to the next ngwee. */
    public double minimumDeposit(double total) {
        return Math.ceil(total * MIN_DEPOSIT_RATE * 100) / 100.0;
    }

    /** The rules for paying at the time of booking. Throws a friendly message if broken. */
    public void checkPayNowAmount(double amount, double total) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Enter the amount to pay now, or choose Pay Later.");
        }
        requireTwoDecimals(amount);
        if (amount > total + 0.005) {
            throw new IllegalArgumentException("The amount (" + MoneyFormat.k(amount)
                    + ") cannot be more than the total (" + MoneyFormat.k(total) + ").");
        }
        double minimum = minimumDeposit(total);
        if (amount + 0.005 < minimum) {
            throw new IllegalArgumentException("To pay now, the amount must be at least "
                    + MoneyFormat.k(minimum) + " (" + Math.round(MIN_DEPOSIT_RATE * 100)
                    + "% of the total). Otherwise choose Pay Later.");
        }
    }

    /**
     * Adds a payment to a bill that already exists (paying a balance later).
     * There is no minimum here: the guest may pay any part of what is left,
     * but never more than the balance.
     */
    public Bill addPayment(String billId, Payment payment) {
        Bill bill = find(billId);
        if (bill.isFullyPaid()) {
            throw new IllegalArgumentException("Bill " + billId + " is already fully paid.");
        }
        if (payment.amount() <= 0) {
            throw new IllegalArgumentException("The payment must be more than zero.");
        }
        requireTwoDecimals(payment.amount());
        if (payment.amount() > bill.balance() + 0.005) {
            throw new IllegalArgumentException("The payment (" + MoneyFormat.k(payment.amount())
                    + ") cannot be more than the balance (" + MoneyFormat.k(bill.balance()) + ").");
        }
        return repository.save(bill.withPayment(payment));
    }

    // ---------- Creating, checking out and deleting ----------

    /**
     * Saves a bill and checks the guest in now. "payment" is the completed
     * payment, or null for Pay Later. Everything is checked again here, and the
     * room is assigned last, so a failed check never leaves a room occupied.
     */
    public Bill createBill(String guestName, RoomType type, int nights,
                           List<ExtraSelection> selections, Payment payment, String notes) {
        BillSummary summary = checkNewBill(guestName, type, nights, selections);

        List<Payment> payments = new ArrayList<>();
        if (payment != null) {
            checkPayNowAmount(payment.amount(), summary.total());
            payments.add(payment);
        }

        Room room = roomService.assignRoom(type);

        List<BillExtra> lines = selections.stream()
                .map(s -> new BillExtra(s.extra().getName(), s.extra().getPrice(),
                        s.quantity(), s.extra().getBasis()))
                .toList();

        Bill bill = new Bill(
                String.format("B%03d", counter++),
                guestName.trim(),
                room.getNumber(),
                type,
                nights,
                LocalDateTime.now(),      // the check-in date AND time
                priceService.getRate(type),
                summary.discount(),
                lines,
                payments,
                notes == null ? "" : notes.trim(),
                null);                    // not checked out yet

        return repository.save(bill);
    }

    /**
     * Checks a guest out: records the time and frees the room.
     * A guest who still owes money cannot check out.
     */
    public Bill checkOut(String billId) {
        Bill bill = find(billId);
        if (bill.isCheckedOut()) {
            throw new IllegalArgumentException(bill.guestName() + " has already checked out.");
        }
        if (!bill.isFullyPaid()) {
            throw new IllegalArgumentException(bill.guestName() + " still owes "
                    + MoneyFormat.k(bill.balance())
                    + ". Take the payment in Guest Payments first.");
        }
        roomService.releaseRoom(bill.roomNumber());
        return repository.save(bill.withCheckOut(LocalDateTime.now()));
    }

    public void deleteBill(String id) {
        Bill bill = find(id);
        // A guest who already checked out gave their room back at check-out. Freeing it
        // again could free the room of whoever is staying in it now.
        if (!bill.isCheckedOut()) {
            roomService.releaseRoom(bill.roomNumber());
        }
        repository.deleteById(id);
    }

    // ---------- Reading bills ----------

    public List<Bill> getBills() {
        return repository.findAll();
    }

    /** Guests who are staying right now. */
    public List<Bill> getCheckedIn() {
        return repository.findAll().stream().filter(b -> !b.isCheckedOut()).toList();
    }

    /** Guests who have left. */
    public List<Bill> getCheckedOut() {
        return repository.findAll().stream().filter(Bill::isCheckedOut).toList();
    }

    // ---------- Dashboard stats (computed, never stored) ----------

    public int billCount() {
        return repository.findAll().size();
    }

    public int checkedInCount() {
        return getCheckedIn().size();
    }

    public int checkedOutCount() {
        return getCheckedOut().size();
    }

    /** Money actually received: the sum of everything paid. */
    public double totalRevenue() {
        return repository.findAll().stream().mapToDouble(Bill::amountPaid).sum();
    }

    /** Guests who checked in today (whether or not they have left yet). */
    public int todaysGuests() {
        LocalDate today = LocalDate.now();
        return (int) repository.findAll().stream()
                .filter(b -> b.checkInDate().equals(today))
                .count();
    }

    // ---------- Helpers ----------

    private Bill find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill " + id + " not found."));
    }

    private void requireTwoDecimals(double amount) {
        if (BigDecimal.valueOf(amount).stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("The amount can have at most 2 decimal places.");
        }
    }

    private void validateSelection(ExtraSelection s, int nights) {
        int q = s.quantity();
        String name = s.extra().getName();
        if (s.extra().getBasis() == PricingBasis.PER_DAY) {
            if (q < 1 || q > nights) {
                throw new IllegalArgumentException(
                        name + " days must be between 1 and " + nights + ".");
            }
        } else if (q != 1) {
            throw new IllegalArgumentException(name + " is a one-time charge (quantity 1).");
        }
    }
}