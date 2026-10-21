package com.mcs.hotel.model;

import java.time.LocalDateTime;

/**
 * A completed payment, as kept on a bill.
 * It stores only safe details: the method, the amount, a reference number and
 * MASKED contact details ("***456", "j***@mail.com"). No PIN, NRC or full
 * number is stored anywhere.
 * "account" is the masked phone for mobile money, or the masked account number for a card.
 */
public record Payment(PaymentMethod method, double amount, String reference,
                      String account, String email, LocalDateTime paidAt) {}