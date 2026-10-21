package com.mcs.hotel;

import com.mcs.hotel.controller.DashboardController;
import com.mcs.hotel.controller.LoginController;
import com.mcs.hotel.repository.InMemoryBillRepository;
import com.mcs.hotel.repository.InMemoryExpenseRepository;
import com.mcs.hotel.repository.InMemoryUserRepository;
import com.mcs.hotel.repository.UserRepository;
import com.mcs.hotel.service.AnalyticsService;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.service.AuthService;
import com.mcs.hotel.service.BillingService;
import com.mcs.hotel.service.ExpenseService;
import com.mcs.hotel.service.PriceService;
import com.mcs.hotel.service.RoomService;
import com.mcs.hotel.service.UserService;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.SceneManager;
import com.mcs.hotel.util.Session;
import com.mcs.hotel.view.DashboardView;
import com.mcs.hotel.view.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point and "wiring" class.
 * It creates every service exactly ONCE and hands them to the controllers.
 * Because all data is in memory, two copies of a service would mean two
 * separate sets of bills, so no controller may create its own.
 */
public class Main extends Application {

    private PriceService priceService;
    private RoomService roomService;
    private BillingService billingService;
    private UserService userService;
    private AuthService authService;
    private ExpenseService expenseService;
    private AnalyticsService analyticsService;

    private SceneManager scenes;

    @Override
    public void start(Stage stage) {
        // Users: AuthService and UserService share one repository (one user list)
        UserRepository userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);
        authService = new AuthService(userRepository);
        userService.seedDefaultAdmin(); // memory is empty on every launch, so re-create admin

        // Billing: BillingService needs rooms and prices, so those are created first
        priceService = new PriceService();
        roomService = new RoomService();
        billingService = new BillingService(new InMemoryBillRepository(), roomService, priceService);

        // Expenses: money spent, used by Analytics to work out profit
        expenseService = new ExpenseService(new InMemoryExpenseRepository());

        // Analytics only READS the bills, expenses and rooms, so it is created last
        analyticsService = new AnalyticsService(billingService, expenseService, roomService);

        stage.setTitle("MCS Hotel");
        stage.getIcons().add(images.load("logo.png")); // window and taskbar icon
        scenes = new SceneManager(stage, 1200, 750);
        showLogin();
    }

    /** Builds a fresh login screen. Also used by Log out to return here. */
    private void showLogin() {
        Session.clear();
        LoginView view = new LoginView();
        new LoginController(view, authService, user -> {
            Session.setUser(user);
            showDashboard();
        });
        scenes.show(view.getRoot());
    }

    /** Builds the main window for whoever just signed in. */
    private void showDashboard() {
        DashboardView view = new DashboardView();
        // Bundle the shared services so every panel works on the same data
        AppServices services = new AppServices(priceService, roomService, billingService,
                userService, expenseService, analyticsService);
        new DashboardController(view, Session.getUser(), services, this::showLogin);
        scenes.show(view.getRoot());
    }

    public static void main(String[] args) {
        launch(args);
    }
}