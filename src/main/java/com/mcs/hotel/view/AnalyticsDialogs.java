package com.mcs.hotel.view;

import com.mcs.hotel.util.Theme;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/** Small questions asked on the Analytics page. Kept here so controllers hold no layout code. */
public final class AnalyticsDialogs {

    private AnalyticsDialogs() {} // utility class: no instances

    /**
     * Asks for the money invested in the lodge. Returns what was typed, or empty
     * if the window was cancelled. The text is checked by the controller and service.
     */
    public static Optional<String> askInvestedCapital(String currentValue) {
        TextInputDialog dialog = new TextInputDialog(currentValue);
        dialog.setTitle("Invested Capital");
        dialog.setHeaderText("The total money put into the lodge. ROI is worked out from this.\nEnter 0 to clear it.");
        dialog.setContentText("Invested capital (K):");
        Theme.apply(dialog.getDialogPane());
        return dialog.showAndWait();
    }
}