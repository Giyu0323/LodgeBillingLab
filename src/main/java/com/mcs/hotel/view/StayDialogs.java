package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.util.Theme;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** The "check this guest out?" question. Kept here so controllers hold no layout code. */
public final class StayDialogs {

    private StayDialogs() {} // utility class: no instances

    /** Returns true only if Check Out is clicked. */
    public static boolean confirmCheckOut(Bill bill) {
        ButtonType checkOut = new ButtonType("Check Out", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Check out " + bill.guestName() + " from room " + bill.roomNumber() + "?\n"
                        + "The room will become available again.",
                checkOut, ButtonType.CANCEL);
        alert.setTitle("Check Out");
        alert.setHeaderText(null);
        Theme.apply(alert.getDialogPane());
        return alert.showAndWait().filter(button -> button == checkOut).isPresent();
    }
}