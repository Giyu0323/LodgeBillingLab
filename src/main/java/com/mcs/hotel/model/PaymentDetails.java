package com.mcs.hotel.model;

/**
 * What staff typed about the guest in the payment window. It exists only while
 * the request is being checked and is then thrown away. There is no PIN: the
 * guest enters that on their own phone. A bank-card payment also uses
 * accountNumber; mobile money leaves it empty.
 */
public record PaymentDetails(String phone, String nrc, String email, String accountNumber) {

    /** Hides everything, so personal details never end up in a log or console by accident. */
    @Override
    public String toString() {
        return "PaymentDetails[hidden]";
    }
}