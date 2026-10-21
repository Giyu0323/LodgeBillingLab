package com.mcs.hotel.service;

/**
 * Bundles the ONE shared instance of each service.
 * Main creates it once and passes it to the controllers, so they all work on
 * the same in-memory data instead of each making their own copy.
 */
public record AppServices(
        PriceService prices,
        RoomService rooms,
        BillingService billing,
        UserService users,
        ExpenseService expenses,
        AnalyticsService analytics) {}