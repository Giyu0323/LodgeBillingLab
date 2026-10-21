package com.mcs.hotel.model;

public enum PricingBasis {
    PER_DAY("Per Day"),
    ONE_TIME("One-time");

    private final String label;

    PricingBasis(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}