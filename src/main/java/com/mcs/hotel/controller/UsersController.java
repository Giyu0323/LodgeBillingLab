package com.mcs.hotel.controller;

import com.mcs.hotel.model.User;
import com.mcs.hotel.service.AppServices;
import com.mcs.hotel.view.Dialogs;
import com.mcs.hotel.view.UsersPanel;

import java.util.List;
import java.util.Locale;

/**
 * Connects UsersPanel to UserService.
 * UserService holds every rule (name length, duplicates, password length, no
 * self-delete, last admin). This class only passes input along and shows
 * whatever message comes back.
 */
public class UsersController implements Searchable {

    private final UsersPanel view;
    private final AppServices services;
    private final User currentUser;   // the admin using this screen
    private String filter = "";       // lower-case search text from the top bar

    public UsersController(UsersPanel view, AppServices services, User currentUser) {
        this.view = view;
        this.services = services;
        this.currentUser = currentUser;

        view.getAddButton().setOnAction(e -> addUser());
        view.onDeleteUser(this::deleteUser);
        refresh();
    }

    /** Top-bar search: filters the list by username. */
    @Override
    public void setFilter(String text) {
        filter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        refresh();
    }

    private void refresh() {
        List<User> shown = services.users().getUsers().stream()
                .filter(u -> filter.isEmpty()
                        || u.getUsername().toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        view.setUsers(shown);
    }

    private void addUser() {
        try {
            User created = services.users().addUser(
                    view.getUsername(), view.getPassword(), view.getConfirm(), view.getRole());
            view.clearForm();
            refresh();
            view.showMessage("Added " + created.getUsername()
                    + " as " + created.getRole() + ".", false);
        } catch (IllegalArgumentException ex) {
            // The service's message is already user-friendly, so show it as is.
            // The form is left alone so the admin can correct it.
            view.showMessage(ex.getMessage(), true);
        }
    }

    private void deleteUser(User user) {
        if (!Dialogs.confirmDeleteUser(user.getUsername())) {
            return; // the admin clicked Cancel
        }
        try {
            // The current username is passed so the service can block self-deletion
            services.users().deleteUser(user.getUsername(), currentUser.getUsername());
        } catch (IllegalArgumentException ex) {
            Dialogs.error(ex.getMessage()); // e.g. "You cannot delete your own account."
        }
        refresh();
    }
}