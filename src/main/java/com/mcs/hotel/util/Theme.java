package com.mcs.hotel.util;

import javafx.scene.control.DialogPane;

import java.util.Objects;

/**
 * Finds style.css once and applies it. The main window gets it through
 * SceneManager. Pop-up dialogs are separate windows that don't inherit it,
 * so they call apply() themselves.
 */
public final class Theme {

    /** The stylesheet's address. A clear error appears if the file is missing. */
    public static final String STYLESHEET = Objects.requireNonNull(
            Theme.class.getResource("/css/style.css"),
            "css/style.css not found. It must be in src/main/resources/css/").toExternalForm();

    private Theme() {} // utility class: no instances

    /** Gives a dialog the same look as the main window. */
    public static void apply(DialogPane pane) {
        pane.getStylesheets().add(STYLESHEET);
    }
}