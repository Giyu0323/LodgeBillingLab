package com.mcs.hotel.model;

public record BillExtra(String name, double unitPrice, int quantity, PricingBasis basis) {

    public double lineTotal() {
        return unitPrice * quantity;
    }
}