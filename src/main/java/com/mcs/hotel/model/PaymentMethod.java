package com.mcs.hotel.model;

import java.util.List;

/**
 * The ways a guest can pay, with the rules for each one's details.
 * The guest's PIN is never part of this: the guest enters it on their own
 * phone, so the system has no PIN rules at all.
 * Every number here (phone prefixes, account length) lives in this one place.
 */
public enum PaymentMethod {

    // label, kind, phone prefixes, description, tile image
    AIRTEL("Airtel Money", Kind.MOBILE_MONEY, List.of("097", "077"),
            "Customers can pay using Airtel Money.", "pay-airtel.png"),
    MTN("MTN Money", Kind.MOBILE_MONEY, List.of("096", "076"),
            "Customers can pay using MTN Money.", "pay-mtn.png"),
    ZAMTEL("Zamtel", Kind.MOBILE_MONEY, List.of("095"),
            "Customers can pay using Zamtel Money.", "pay-zamtel.png"),
    CARD("Card Payment", Kind.BANK, List.of(),
            "Customers can pay using Visa, Mastercard and other cards.", "pay-card.png");

    /** Mobile money must use that network's phone number; a bank card asks for an account number. */
    public enum Kind { MOBILE_MONEY, BANK }

    /** Bank account numbers: digits only, between these lengths (the maximum is 12). */
    public static final int ACCOUNT_MIN_DIGITS = 8;
    public static final int ACCOUNT_MAX_DIGITS = 12;

    private final String label;
    private final Kind kind;
    private final List<String> phonePrefixes;
    private final String description;
    private final String iconFile;

    PaymentMethod(String label, Kind kind, List<String> phonePrefixes,
                  String description, String iconFile) {
        this.label = label;
        this.kind = kind;
        this.phonePrefixes = phonePrefixes;
        this.description = description;
        this.iconFile = iconFile;
    }

    public boolean isBank() { return kind == Kind.BANK; }
    public List<String> phonePrefixes() { return phonePrefixes; }
    public String description() { return description; }
    public String iconFile() { return iconFile; }

    /** One sentence telling staff how the guest will confirm this payment. */
    public String confirmationNote() {
        return isBank()
                ? "The guest's bank will send an approval request to their phone."
                : label + " will send a prompt to the guest's phone. The guest enters their PIN there, so never ask for it.";
    }

    @Override
    public String toString() { return label; }
}