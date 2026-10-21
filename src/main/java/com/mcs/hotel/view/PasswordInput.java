package com.mcs.hotel.view;

import com.mcs.hotel.util.images;
import javafx.geometry.Pos;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * A password box with a lock icon on the left and a show/hide eye on the right.
 * A PasswordField cannot reveal its text, so a normal TextField sits on top of
 * it and shares the same text. The eye swaps which one is visible.
 * VIEW ONLY: it holds no rules, just the text the user typed.
 */
public class PasswordInput {

    private final PasswordField hidden = new PasswordField();
    private final TextField shown = new TextField();
    private final ToggleButton eye = new ToggleButton();
    private final StackPane root;

    public PasswordInput(String prompt) {
        setPrompt(prompt);
        hidden.setMaxWidth(Double.MAX_VALUE);
        shown.setMaxWidth(Double.MAX_VALUE);
        // Padding for BOTH the lock on the left and the eye on the right (see style.css)
        hidden.getStyleClass().addAll("icon-input", "password-input");
        shown.getStyleClass().addAll("icon-input", "password-input");

        // Both boxes always hold the same text, so switching never loses anything
        shown.textProperty().bindBidirectional(hidden.textProperty());

        // Only one box is shown (and takes up space) at a time
        shown.setVisible(false);
        shown.managedProperty().bind(shown.visibleProperty());
        hidden.managedProperty().bind(hidden.visibleProperty());

        ImageView eyeOn = images.view("eye.png", 20);
        ImageView eyeOff = images.view("eye-off.png", 20);
        eye.setGraphic(eyeOn);
        eye.getStyleClass().add("eye-button");
        eye.setTooltip(new Tooltip("Show or hide"));
        eye.setFocusTraversable(false);
        eye.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);

        eye.selectedProperty().addListener((obs, was, show) -> {
            shown.setVisible(show);
            hidden.setVisible(!show);
            eye.setGraphic(show ? eyeOff : eyeOn);
            TextField active = show ? shown : hidden;
            active.requestFocus();
            active.positionCaret(active.getText().length()); // cursor stays at the end
        });

        root = new StackPane(hidden, shown, IconInput.overlay("lock.png"), eye);
    }

    /** The control to place in a layout. */
    public Region getNode() { return root; }

    public String getText() { return hidden.getText(); }

    /** Changes the grey hint text inside the box. */
    public void setPrompt(String prompt) {
        hidden.setPromptText(prompt);
        shown.setPromptText(prompt);
    }

    /** From now on the box accepts only digits, and no more than maxDigits of them. */
    public void limitDigits(int maxDigits) {
        // Each box needs its own formatter object
        hidden.setTextFormatter(digitsOnly(maxDigits));
        shown.setTextFormatter(digitsOnly(maxDigits));
    }

    /** Empties the box and hides the text again. */
    public void clear() {
        hidden.clear();
        eye.setSelected(false);
    }

    private static TextFormatter<String> digitsOnly(int max) {
        return new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("[0-9]*") && text.length() <= max ? change : null; // null = reject the keystroke
        });
    }
}