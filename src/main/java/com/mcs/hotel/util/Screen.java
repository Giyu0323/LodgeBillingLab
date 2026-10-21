package com.mcs.hotel.util;

/**
 * The sections reachable from the sidebar.
 * One enum keeps the label, icon and "admin only" rule in a single place,
 * so the view and the controller can never disagree about them.
 */
public enum Screen {
    DASHBOARD("Dashboard", "nav-dashboard.png", false),
    NEW_BILL("New Bill", "nav-new-bill.png", false),
    VIEW_RECEIPTS("View Receipts", "nav-receipts.png", false),
    DELETE_BILL("Delete Bill", "nav-delete.png", false),
    USERS("Users", "nav-users.png", true),
    PAYMENT_METHODS("Payment Methods", "nav-payments.png", true),
    CHECKED_IN("Checked-In Guests", "nav-checkin.png", false),
    CHECKED_OUT("Checked-Out Guests", "nav-checkout.png", false),
    GUEST_PAYMENTS("Guest Payments", "nav-guest-payments.png", false),
    EXPENSES("Expenses", "nav-expenses.png", true),
    ANALYTICS("Analytics", "nav-analytics.png", true),
    PRICES("Prices", "nav-prices.png", true);

    private final String label;
    private final String iconFile; // file name inside src/main/resources/images/
    private final boolean adminOnly;

    Screen(String label, String iconFile, boolean adminOnly) {
        this.label = label;
        this.iconFile = iconFile;
        this.adminOnly = adminOnly;
    }

    public String getLabel() { return label; }
    public String getIconFile() { return iconFile; }
    public boolean isAdminOnly() { return adminOnly; }
}