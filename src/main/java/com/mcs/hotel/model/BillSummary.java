package com.mcs.hotel.model;

public record BillSummary(double roomCharge, double discount, double extrasTotal) {

    public double total() {
        return roomCharge - discount + extrasTotal;
    }
}