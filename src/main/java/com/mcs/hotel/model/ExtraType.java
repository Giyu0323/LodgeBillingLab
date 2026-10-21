package com.mcs.hotel.model;

public class ExtraType {
    private String name;
    private double price;
    private PricingBasis basis;

    public ExtraType(String name, double price, PricingBasis basis) {
        this.name = name;
        this.price = price;
        this.basis = basis;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public PricingBasis getBasis() { return basis; }

    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setBasis(PricingBasis basis) { this.basis = basis; }
}