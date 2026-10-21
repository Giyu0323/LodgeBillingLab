package com.mcs.hotel.controller;

import com.mcs.hotel.model.User;
import com.mcs.hotel.service.AuthService;
import com.mcs.hotel.util.LoginPreferences;
import com.mcs.hotel.view.LoginView;

import java.util.function.Consumer;

/**
 * Connects LoginView to AuthService.
 * Flow: button click -> read the fields -> ask AuthService -> update the view.
 * It also fills in the remembered username when the screen opens.
 */
public class LoginController {

    private final LoginView view;
    private final AuthService authService;
    private final Consumer<User> onSuccess; // what to do after a good login

    /**
     * @param onSuccess supplied by Main. The controller doesn't know which screen
     *                  comes next, which keeps it reusable and easy to change.
     */
    public LoginController(LoginView view, AuthService authService, Consumer<User> onSuccess) {
        this.view = view;
        this.authService = authService;
        this.onSuccess = onSuccess;

        // Pre-fill the username if "Remember me" was used last time
        String saved = LoginPreferences.getSavedUsername();
        if (saved.isEmpty()) {
            view.focusUsername();
        } else {
            view.setUsername(saved);
            view.setRemember(true);
            view.focusPassword(); // only the password is left to type
        }

        view.getSignInButton().setOnAction(e -> handleSignIn());
    }

    private void handleSignIn() {
        User user;
        try {
            user = authService.login(view.getUsername(), view.getPassword());
        } catch (IllegalArgumentException ex) {
            // AuthService's message is already user-friendly, so display it as is
            view.showError(ex.getMessage());
            view.clearPassword();
            return;
        }

        // Only after a successful login: remember or forget the username
        if (view.isRemember()) {
            LoginPreferences.saveUsername(user.getUsername());
        } else {
            LoginPreferences.clear();
        }

        try {
            onSuccess.accept(user); // opens the dashboard
        } catch (RuntimeException ex) {
            // The login was fine, but the next screen failed to build (for example a
            // missing image). Show the reason here instead of failing silently.
            ex.printStackTrace();
            view.showError("Signed in, but the app could not open: " + ex.getMessage());
        }
    }
}