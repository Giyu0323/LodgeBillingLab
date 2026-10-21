package com.mcs.hotel.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Switches between screens (login, dashboard) inside ONE window.
 * Controllers ask it to show a screen, so they never touch the Stage directly.
 */
public class SceneManager {

    private final Stage stage;
    private final double width;
    private final double height;
    private Scene scene; // created on first use, then reused

    public SceneManager(Stage stage, double width, double height) {
        this.stage = stage;
        this.width = width;
        this.height = height;
    }

    /** Shows the given screen. Swapping the root keeps the window size and position. */
    public void show(Parent root) {
        if (scene == null) {
            scene = new Scene(root, width, height);
            scene.getStylesheets().add(Theme.STYLESHEET); // the dark navy + orange theme
            stage.setScene(scene);
            stage.show();
        } else {
            scene.setRoot(root);
        }
    }
}