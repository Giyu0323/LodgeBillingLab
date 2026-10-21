package com.mcs.hotel.model;

import java.time.Duration;
import java.time.Instant;

/**
 * A payment request that has been sent to the guest's phone.
 * It holds only MASKED contact details (for example "***456"); the full
 * numbers and email are used once for the checks and then forgotten.
 * What state the request is in is kept by PaymentService, not here.
 */
public record PaymentRequest(PaymentMethod method, double amount, String reference,
                             String maskedPhone, String maskedAccount, String maskedEmail,
                             Instant expiresAt) {

    /** Whole seconds left before the request expires (never below 0). */
    public long secondsLeft() {
        return Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
    }
}