package com.mcs.hotel.view;

import com.mcs.hotel.util.Theme;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** The "switch this payment method on or off?" question. Kept here so controllers hold no layout code. */
public final class MethodDialog {

    private MethodDialog() {} // utility class: no instances

    /**
     * Asks whether to switch a method off (when it is active) or on (when it is not).
     * Returns true only if the action button is clicked.
     */
    public static boolean confirmToggle(String methodName, boolean currentlyActive) {
        String action = currentlyActive ? "Disable" : "Enable";
        String message = currentlyActive
                ? "Disable " + methodName + "?\nGuests will no longer be able to choose it when paying."
                : "Enable " + methodName + "?\nGuests will be able to choose it when paying.";

        ButtonType actionType = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, actionType, ButtonType.CANCEL);
        alert.setTitle(action + " payment method");
        alert.setHeaderText(null);
        Theme.apply(alert.getDialogPane());
        return alert.showAndWait().filter(button -> button == actionType).isPresent();
    }
}