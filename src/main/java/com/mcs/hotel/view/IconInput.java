package com.mcs.hotel.view;

import com.mcs.hotel.util.images;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Puts a small icon inside the left edge of a text box (a person in the
 * username box, a magnifier in the search box, and so on).
 * The icon floats over the box, so the box itself stays a normal TextField.
 */
public final class IconInput {

    private IconInput() {} // utility class: no instances

    /** Wraps a text box with an icon on its left. Returns the combined control. */
    public static StackPane wrap(Region input, String iconFile) {
        input.getStyleClass().add("icon-input"); // style.css adds left padding so text clears the icon
        return new StackPane(input, overlay(iconFile));
    }

    /**
     * Just the floating icon, for boxes built by hand (such as the password box,
     * which already has its own StackPane).
     */
    public static ImageView overlay(String iconFile) {
        ImageView icon = images.view(iconFile, 18);
        icon.setMouseTransparent(true); // clicks go through to the text box underneath
        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        StackPane.setMargin(icon, new Insets(0, 0, 0, 13));
        return icon;
    }
}