package com.mcs.hotel.view;

import com.mcs.hotel.model.Expense;
import com.mcs.hotel.util.MoneyFormat;
import com.mcs.hotel.util.Theme;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/** The "delete this expense?" question. Kept here so controllers hold no layout code. */
public final class ExpenseDialogs {

    private ExpenseDialogs() {} // utility class: no instances

    /** Returns true only if Delete is clicked. */
    public static boolean confirmDelete(Expense expense) {
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete the expense \"" + expense.description() + "\" ("
                        + MoneyFormat.k(expense.amount()) + ")?\nThis cannot be undone.",
                delete, ButtonType.CANCEL);
        alert.setTitle("Delete Expense");
        alert.setHeaderText(null);
        Theme.apply(alert.getDialogPane());
        return alert.showAndWait().filter(button -> button == delete).isPresent();
    }
}