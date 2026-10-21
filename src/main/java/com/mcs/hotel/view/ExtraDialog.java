package com.mcs.hotel.view;

import com.mcs.hotel.model.PricingBasis;
import com.mcs.hotel.util.Theme;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Function;

/**
 * The pop-up form used to add or edit an extra charge.
 * VIEW ONLY: it does not know any pricing rules. When Save is clicked it hands
 * the typed values to a function supplied by the controller. If that function
 * returns an error message, the dialog stays open and shows it. If it returns
 * null, the save worked and the dialog closes.
 */
public final class ExtraDialog {

    /** What the admin typed. The price is still text, so the controller can check it. */
    public record Input(String name, String price, PricingBasis basis) {}

    private ExtraDialog() {} // utility class: no instances

    /**
     * Opens the form and waits until it is closed.
     *
     * @param onSave runs the real save. Returns an error message, or null on success.
     * @return true if the extra was saved, false if the admin cancelled
     */
    public static boolean show(String title, String name, String price, PricingBasis basis,
                               Function<Input, String> onSave) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField nameField = new TextField(name);
        nameField.setPromptText("e.g. Airport Pickup");
        TextField priceField = new TextField(price);
        priceField.setPromptText("0.00");
        ComboBox<PricingBasis> basisBox = new ComboBox<>();
        basisBox.getItems().addAll(PricingBasis.values());
        basisBox.setValue(basis);
        basisBox.setMaxWidth(Double.MAX_VALUE);

        // The error line starts hidden and takes no space until it is used
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("message-error"); // red text, from style.css
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        VBox form = new VBox(8,
                new Label("Name"), nameField,
                new Label("Price in K"), priceField,
                new Label("Charged"), basisBox,
                error);
        form.setPrefWidth(300);
        dialog.getDialogPane().setContent(form);

        boolean[] saved = {false}; // a one-item array, so the lambda below can change it

        // Runs when Save is clicked, before the dialog would close
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String problem = onSave.apply(
                    new Input(nameField.getText(), priceField.getText(), basisBox.getValue()));
            if (problem == null) {
                saved[0] = true;     // success: let the dialog close
            } else {
                error.setText(problem);
                error.setVisible(true);
                event.consume();     // consuming the click keeps the dialog open
            }
        });

        Theme.apply(dialog.getDialogPane()); // dialogs are separate windows, so style them too
        dialog.showAndWait();
        return saved[0];
    }
}