package com.mcs.hotel.util;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

/**
 * Loads pictures from src/main/resources/images/ in one place.
 * A missing file gives a clear message instead of a vague crash.
 */
public final class images {

    private images() {} // utility class: no instances

    /** A ready-to-use picture scaled to the given width, keeping its proportions. */
    public static ImageView view(String fileName, double width) {
        // Loaded at twice the size so it stays sharp on high-resolution screens
        Image image = new Image(url(fileName).toExternalForm(), width * 2, 0, true, true);
        ImageView view = new ImageView(image);
        view.setFitWidth(width);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    /**
     * Same as view(), but a white icon is turned dark navy. Use it on orange
     * buttons, where a white icon would be hard to read next to the dark text.
     */
    public static ImageView viewDark(String fileName, double width) {
        ImageView view = view(fileName, width);
        view.setEffect(new ColorAdjust(0, 0, -0.92, 0)); // brightness only, so transparency is kept
        return view;
    }

    /** The raw picture, for example for the window icon. */
    public static Image load(String fileName) {
        return new Image(url(fileName).toExternalForm());
    }

    private static URL url(String fileName) {
        URL url = images.class.getResource("/images/" + fileName);
        if (url == null) {
            throw new IllegalStateException("images/" + fileName
                    + " not found. Put it in src/main/resources/images/ and reload Gradle.");
        }
        return url;
    }
}