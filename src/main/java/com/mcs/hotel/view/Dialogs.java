package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.util.Theme;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** Pop-up questions and messages, kept here so controllers contain no layout code. */
public final class Dialogs {

    private Dialogs() {} // utility class: no instances

    /** Asks "are you sure?" before a bill is deleted. Returns true only if Delete is clicked. */
    public static boolean confirmDeleteBill(Bill bill) {
        return confirm("Delete Bill", "Delete",
                "Delete bill " + bill.id() + " for " + bill.guestName() + "?\n"
                        + "Room " + bill.roomNumber() + " will be freed. This cannot be undone.");
    }

    /** Asks "are you sure?" before a user account is deleted. */
    public static boolean confirmDeleteUser(String username) {
        return confirm("Delete User", "Delete",
                "Delete the user \"" + username + "\"?\n"
                        + "They will no longer be able to sign in. This cannot be undone.");
    }

    /** Asks "are you sure?" before an extra charge is removed from the price list. */
    public static boolean confirmRemoveExtra(String name) {
        return confirm("Remove Extra", "Remove",
                "Remove \"" + name + "\" from the price list?\n"
                        + "Bills already saved keep their charges and receipts do not change.");
    }

    /** Shows a simple error message. */
    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        Theme.apply(alert.getDialogPane()); // dialogs are separate windows, so style them too
        alert.showAndWait();
    }

    /** Shared by the confirm dialogs above: true only if the action button is clicked. */
    private static boolean confirm(String title, String actionLabel, String message) {
        ButtonType action = new ButtonType(actionLabel, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, action, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Theme.apply(alert.getDialogPane());
        return alert.showAndWait().filter(button -> button == action).isPresent();
    }
}