package com.mcs.hotel.model;

/** Where a payment request is in its life, from "sent" to "done". */
public enum PaymentStatus {
    PENDING,    // sent to the guest's phone, waiting for them to confirm
    APPROVED,   // the guest confirmed on their phone
    DECLINED,   // the guest refused
    EXPIRED,    // the guest did not answer in time
    CANCELLED,  // staff cancelled the request
    COMPLETED   // approved and already turned into a payment on a bill
}