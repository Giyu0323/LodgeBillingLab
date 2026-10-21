package com.mcs.hotel.service;

import com.mcs.hotel.model.Payment;
import com.mcs.hotel.model.PaymentDetails;
import com.mcs.hotel.model.PaymentMethod;
import com.mcs.hotel.model.PaymentRequest;
import com.mcs.hotel.model.PaymentStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends payment requests, tracks their status, and remembers which payment
 * methods are switched on.
 *
 * HOW IT WORKS: send() checks the guest's details and creates a PENDING request.
 * In real life the provider (Airtel, MTN, Zamtel or the bank) now sends a prompt
 * to the guest's phone, the guest enters their PIN THERE, and the provider
 * reports back. Here that last step is guestResponds(), a DEMO stand-in.
 * The system never sees a PIN.
 */
public class PaymentService {

    /** How long the guest has to confirm on their phone. */
    public static final int REQUEST_SECONDS = 60;

    private int counter = 1; // for reference numbers: TXN-0001, TXN-0002, ...

    // Every method starts switched on (everything is in memory, so this resets on launch)
    private final Set<PaymentMethod> enabled = EnumSet.allOf(PaymentMethod.class);

    // The current status of every request, by reference number
    private final Map<String, PaymentStatus> statuses = new HashMap<>();

    // ---------- Which methods are available ----------

    public boolean isEnabled(PaymentMethod method) {
        return enabled.contains(method);
    }

    /** The methods guests can choose right now, in the usual order. */
    public List<PaymentMethod> enabledMethods() {
        return Arrays.stream(PaymentMethod.values()).filter(enabled::contains).toList();
    }

    /** Switches a method on or off. The last active method cannot be switched off. */
    public void setEnabled(PaymentMethod method, boolean on) {
        if (!on && enabled.contains(method) && enabled.size() == 1) {
            throw new IllegalArgumentException("At least one payment method must stay active.");
        }
        if (on) {
            enabled.add(method);
        } else {
            enabled.remove(method);
        }
    }

    // ---------- The request / approve cycle ----------

    /**
     * Checks the guest's details and sends the request to their phone.
     * Throws a friendly message if anything is wrong, and sends nothing.
     */
    public PaymentRequest send(PaymentMethod method, double amount, PaymentDetails details) {
        validate(method, details);

        String reference = String.format("TXN-%04d", counter++);
        String maskedAccount = method.isBank() ? mask(details.accountNumber().trim()) : null;

        PaymentRequest request = new PaymentRequest(method, amount, reference,
                mask(normalisePhone(details.phone())), maskedAccount, maskEmail(details.email()),
                Instant.now().plusSeconds(REQUEST_SECONDS));
        statuses.put(reference, PaymentStatus.PENDING);
        return request;
    }

    /** Where the request is now. A pending request that ran out of time becomes EXPIRED. */
    public PaymentStatus check(PaymentRequest request) {
        PaymentStatus status = statuses.getOrDefault(request.reference(), PaymentStatus.CANCELLED);
        if (status == PaymentStatus.PENDING && Instant.now().isAfter(request.expiresAt())) {
            statuses.put(request.reference(), PaymentStatus.EXPIRED);
            return PaymentStatus.EXPIRED;
        }
        return status;
    }

    /**
     * DEMO ONLY: stands in for the guest tapping approve or decline on their phone.
     * A real gateway would call this when the provider reports the guest's answer.
     */
    public void guestResponds(PaymentRequest request, boolean approve) {
        if (check(request) == PaymentStatus.PENDING) {
            statuses.put(request.reference(), approve ? PaymentStatus.APPROVED : PaymentStatus.DECLINED);
        }
    }

    /** Staff cancel a request that is still waiting. */
    public void cancel(PaymentRequest request) {
        if (check(request) == PaymentStatus.PENDING) {
            statuses.put(request.reference(), PaymentStatus.CANCELLED);
        }
    }

    /** Turns an APPROVED request into a Payment, exactly once. */
    public Payment complete(PaymentRequest request) {
        if (check(request) != PaymentStatus.APPROVED) {
            throw new IllegalStateException("This payment has not been approved by the guest.");
        }
        statuses.put(request.reference(), PaymentStatus.COMPLETED);
        String account = request.method().isBank() ? request.maskedAccount() : request.maskedPhone();
        return new Payment(request.method(), request.amount(), request.reference(),
                account, request.maskedEmail(), LocalDateTime.now());
    }

    // ---------- Checking the details ----------

    /** Checks every detail the chosen method needs. */
    public void validate(PaymentMethod method, PaymentDetails details) {
        if (method == null) {
            throw new IllegalArgumentException("Choose a payment method.");
        }
        if (!isEnabled(method)) {
            throw new IllegalArgumentException(method + " is not available right now.");
        }
        validatePhone(method, details.phone());
        validateNrc(details.nrc());
        validateEmail(details.email());
        if (method.isBank()) {
            validateAccount(details.accountNumber());
        }
    }

    private void validatePhone(PaymentMethod method, String raw) {
        String phone = normalisePhone(raw);
        if (!phone.matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone number must be 10 digits, for example 0977123456.");
        }
        if (method.isBank()) {
            // A card needs any Zambian mobile number, because the bank's request goes to the phone
            if (!phone.startsWith("09") && !phone.startsWith("07")) {
                throw new IllegalArgumentException("Enter the guest's mobile number, for example 0977123456.");
            }
        } else if (method.phonePrefixes().stream().noneMatch(phone::startsWith)) {
            throw new IllegalArgumentException(method + " numbers start with "
                    + String.join(" or ", method.phonePrefixes()) + ".");
        }
    }

    /** Zambian NRC: 6 digits, 2 digits, 1 digit, e.g. 123456/78/1 (slashes are optional when typing). */
    private void validateNrc(String raw) {
        String nrc = raw == null ? "" : raw.replaceAll("[\\s/]", "");
        if (!nrc.matches("\\d{9}")) {
            throw new IllegalArgumentException("NRC number must look like 123456/78/1.");
        }
    }

    private void validateEmail(String raw) {
        String email = raw == null ? "" : raw.trim();
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || email.length() > 100) {
            throw new IllegalArgumentException("Enter a valid email address, for example guest@example.com.");
        }
    }

    private void validateAccount(String raw) {
        String account = raw == null ? "" : raw.trim();
        if (!account.matches("\\d+")) {
            throw new IllegalArgumentException("Enter the bank account number (digits only).");
        }
        if (account.length() < PaymentMethod.ACCOUNT_MIN_DIGITS
                || account.length() > PaymentMethod.ACCOUNT_MAX_DIGITS) {
            throw new IllegalArgumentException("Account number must be " + PaymentMethod.ACCOUNT_MIN_DIGITS
                    + " to " + PaymentMethod.ACCOUNT_MAX_DIGITS + " digits.");
        }
    }

    // ---------- Helpers ----------

    /** Turns +260977123456 or 260977123456 into 0977123456, and removes spaces and dashes. */
    private String normalisePhone(String raw) {
        String phone = raw == null ? "" : raw.replaceAll("[\\s-]", "");
        if (phone.startsWith("+260")) {
            phone = "0" + phone.substring(4);
        } else if (phone.startsWith("260")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }

    /** "0977123456" becomes "***456": enough to recognise, not enough to misuse. */
    private String mask(String digits) {
        return "***" + digits.substring(Math.max(0, digits.length() - 3));
    }

    /** "john@mail.com" becomes "j***@mail.com". The address was checked first, so the "@" is there. */
    private String maskEmail(String email) {
        String clean = email.trim();
        return clean.charAt(0) + "***" + clean.substring(clean.indexOf('@'));
    }
}