package com.mcs.hotel.service;

import com.mcs.hotel.model.ExtraType;
import com.mcs.hotel.model.PricingBasis;
import com.mcs.hotel.model.RoomType;

import java.util.*;

public class PriceService {

    private final Map<RoomType, Double> rates = new EnumMap<>(RoomType.class);
    private final List<ExtraType> extras = new ArrayList<>();

    public PriceService() {
        rates.put(RoomType.STANDARD, 350.0);
        rates.put(RoomType.DELUXE, 550.0);
        rates.put(RoomType.FAMILY, 750.0);

        extras.add(new ExtraType("Breakfast", 50, PricingBasis.PER_DAY));
        extras.add(new ExtraType("Lunch", 80, PricingBasis.PER_DAY));
        extras.add(new ExtraType("Dinner", 90, PricingBasis.PER_DAY));
        extras.add(new ExtraType("Housekeeping", 40, PricingBasis.PER_DAY));
        extras.add(new ExtraType("Laundry", 120, PricingBasis.ONE_TIME));
    }

    // ---- Room rates ----
    public double getRate(RoomType type) {
        return rates.get(type);
    }

    public void setRate(RoomType type, double price) {
        requirePositive(price);
        rates.put(type, price);
    }

    // ---- Extras ----
    public List<ExtraType> getExtras() {
        return Collections.unmodifiableList(extras);
    }

    public Optional<ExtraType> findExtra(String name) {
        return extras.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public void addExtra(String name, double price, PricingBasis basis) {
        String clean = requireName(name);
        requirePositive(price);
        if (findExtra(clean).isPresent()) {
            throw new IllegalArgumentException("An extra named \"" + clean + "\" already exists.");
        }
        extras.add(new ExtraType(clean, price, basis));
    }

    public void updateExtra(ExtraType extra, String newName, double price, PricingBasis basis) {
        String clean = requireName(newName);
        requirePositive(price);
        boolean clash = extras.stream()
                .anyMatch(e -> e != extra && e.getName().equalsIgnoreCase(clean));
        if (clash) {
            throw new IllegalArgumentException("An extra named \"" + clean + "\" already exists.");
        }
        extra.setName(clean);
        extra.setPrice(price);
        extra.setBasis(basis);
    }

    public void removeExtra(ExtraType extra) {
        extras.remove(extra);
    }

    // ---- Validation helpers ----
    private void requirePositive(double price) {
        if (!(price > 0)) {
            throw new IllegalArgumentException("Price must be greater than 0.");
        }
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        return name.trim();
    }
}