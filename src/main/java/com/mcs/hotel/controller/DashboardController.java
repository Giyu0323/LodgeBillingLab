package com.mcs.hotel.controller;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.model.User;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.util.Screen;
import com.mcs.hotel.view.AnalyticsPanel;
import com.mcs.hotel.view.DashboardView;
import com.mcs.hotel.view.DeleteBillPanel;
import com.mcs.hotel.view.ExpensesPanel;
import com.mcs.hotel.view.GuestPaymentsPanel;
import com.mcs.hotel.view.HomePanel;
import com.mcs.hotel.view.NewBillPanel;
import com.mcs.hotel.view.PaymentMethodsPanel;
import com.mcs.hotel.view.PricesPanel;
import com.mcs.hotel.view.ReceiptListPanel;
import com.mcs.hotel.view.ReceiptPanel;
import com.mcs.hotel.view.StayPanel;
import com.mcs.hotel.view.UsersPanel;
import javafx.scene.Node;

/**
 * Connects DashboardView to the rest of the app: handles sidebar clicks,
 * decides who may open what, and swaps the panel in the content area.
 */
public class DashboardController {

    private final DashboardView view;
    private final User user;
    private final AppServices services;

    // The open screen's controller IF that screen has a searchable list,
    // otherwise null. The top-bar search box passes its text to it.
    private Searchable currentSearchable;

    /**
     * @param services the shared services, passed on to each panel's controller
     * @param onLogout supplied by Main, because only Main knows how to return
     *                 to the login screen.
     */
    public DashboardController(DashboardView view, User user, AppServices services,
                               Runnable onLogout) {
        this.view = view;
        this.user = user;
        this.services = services;

        view.setUserInfo(user.getUsername(), user.getRole().toString());
        view.setAdminItemsVisible(user.isAdmin()); // staff never see the admin-only items

        for (Screen screen : Screen.values()) {
            view.getNavButton(screen).setOnAction(e -> navigateTo(screen));
        }
        view.getLogoutButton().setOnAction(e -> onLogout.run());

        // Typing in the top-bar search filters whichever list is on screen
        view.getSearchField().textProperty().addListener((obs, oldText, newText) -> {
            if (currentSearchable != null) {
                currentSearchable.setFilter(newText);
            }
        });

        navigateTo(Screen.DASHBOARD); // first screen after login
    }

    /** Opens a section. Hidden buttons are not enough on their own, so this checks the role too. */
    public void navigateTo(Screen screen) {
        if (screen.isAdminOnly() && !user.isAdmin()) {
            return;
        }
        currentSearchable = null;         // set again below if the new screen has a list
        view.getSearchField().clear();    // every section starts with an empty search
        view.setActive(screen);
        view.setContent(createPanel(screen));
    }

    /**
     * Opens one bill's receipt. "backTo" is the screen the user came from, so
     * Back to Bills returns them there. The receipt isn't a sidebar section,
     * so View Receipts is highlighted while it is open.
     */
    private void openReceipt(Bill bill, Screen backTo) {
        currentSearchable = null;
        view.getSearchField().clear();
        view.setActive(Screen.VIEW_RECEIPTS);

        ReceiptPanel panel = new ReceiptPanel();
        new ReceiptController(panel, bill, () -> navigateTo(backTo));
        view.setContent(panel.getRoot());
    }

    /**
     * Builds the panel for a section. A fresh panel is built on every visit,
     * so the numbers are always up to date. Every Screen has a case, so there
     * is no default: adding a new Screen later makes this a compile error
     * until its panel is added here.
     */
    private Node createPanel(Screen screen) {
        return switch (screen) {
            case DASHBOARD -> {
                HomePanel panel = new HomePanel();
                HomeController controller = new HomeController(panel, services,
                        bill -> openReceipt(bill, Screen.DASHBOARD));
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case NEW_BILL -> {
                NewBillPanel panel = new NewBillPanel();
                new NewBillController(panel, services);
                yield panel.getRoot();
            }
            case VIEW_RECEIPTS -> {
                ReceiptListPanel panel = new ReceiptListPanel();
                ReceiptListController controller = new ReceiptListController(panel, services,
                        bill -> openReceipt(bill, Screen.VIEW_RECEIPTS));
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case DELETE_BILL -> {
                DeleteBillPanel panel = new DeleteBillPanel();
                DeleteBillController controller = new DeleteBillController(panel, services,
                        bill -> openReceipt(bill, Screen.DELETE_BILL));
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case USERS -> {
                UsersPanel panel = new UsersPanel();
                UsersController controller = new UsersController(panel, services, user);
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case PAYMENT_METHODS -> {
                PaymentMethodsPanel panel = new PaymentMethodsPanel();
                new PaymentMethodsController(panel, services.billing().payments());
                yield panel.getRoot(); // no search list on this screen
            }
            case CHECKED_IN -> {
                StayPanel panel = new StayPanel("Checked-In Guests",
                        "Guests who are staying at the hotel right now.", "nav-checkin.png", true);
                StayController controller = new StayController(panel, services, true,
                        bill -> openReceipt(bill, Screen.CHECKED_IN));
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case CHECKED_OUT -> {
                StayPanel panel = new StayPanel("Checked-Out Guests",
                        "Guests who have completed their stay.", "nav-checkout.png", false);
                StayController controller = new StayController(panel, services, false,
                        bill -> openReceipt(bill, Screen.CHECKED_OUT));
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case GUEST_PAYMENTS -> {
                GuestPaymentsPanel panel = new GuestPaymentsPanel();
                GuestPaymentsController controller = new GuestPaymentsController(panel, services,
                        bill -> openReceipt(bill, Screen.GUEST_PAYMENTS));
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case EXPENSES -> {
                ExpensesPanel panel = new ExpensesPanel();
                ExpensesController controller = new ExpensesController(panel, services);
                currentSearchable = controller;
                yield panel.getRoot();
            }
            case ANALYTICS -> {
                AnalyticsPanel panel = new AnalyticsPanel();
                new AnalyticsController(panel, services);
                yield panel.getRoot(); // no search list on this screen
            }
            case PRICES -> {
                PricesPanel panel = new PricesPanel();
                new PricesController(panel, services);
                yield panel.getRoot(); // no search list on this screen
            }
        };
    }
}