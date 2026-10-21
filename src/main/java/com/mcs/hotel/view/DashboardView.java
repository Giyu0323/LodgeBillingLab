package com.mcs.hotel.view;

import com.mcs.hotel.util.images;
import com.mcs.hotel.util.Screen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

/**
 * The main window frame: a top bar across the whole window (logo, search,
 * user), a menu sidebar below it on the left, and a content area where the
 * controller swaps panels.
 * VIEW ONLY: it decides nothing about who may see what. The controller tells
 * it, for example, to hide the admin items.
 */
public class DashboardView {

    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final Map<Screen, Button> navButtons = new EnumMap<>(Screen.class);
    private final Map<Screen, ImageView> navIcons = new EnumMap<>(Screen.class);

    // Turns a white icon dark navy. Used on the active (orange) sidebar button,
    // where a white icon would be hard to read. Brightness -1 would be pure
    // black, so this stops just short of it.
    private final ColorAdjust darken = new ColorAdjust(0, 0, -0.92, 0);

    private final TextField searchField = new TextField();
    private final Label userLabel = new Label();
    private final Label roleLabel = new Label();
    private final Button logoutButton = new Button("Log out");

    public DashboardView() {
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(contentArea);
    }

    /** Left column: one icon button per Screen (in enum order). The logo lives in the top bar. */
    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(18, 16, 16, 16));
        sidebar.setPrefWidth(220);
        sidebar.getStyleClass().add("sidebar");

        for (Screen screen : Screen.values()) {
            ImageView icon = images.view(screen.getIconFile(), 20);

            Button button = new Button(screen.getLabel(), icon);
            button.setMaxWidth(Double.MAX_VALUE);   // fill the sidebar width
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().add("nav-button");
            navButtons.put(screen, button);
            navIcons.put(screen, icon);
            sidebar.getChildren().add(button);
        }
        return sidebar;
    }

    /** Top row: logo and name, search box, then bell, avatar, user info and Log out on the right. */
    private HBox buildTopBar() {
        ImageView logo = images.view("logo.png", 44);
        Label name = new Label("MCS Hotel");
        name.getStyleClass().add("brand-title");
        Label tagline = new Label("STAY · RELAX · ENJOY");
        tagline.getStyleClass().add("brand-tagline");

        HBox brand = new HBox(10, logo, new VBox(1, name, tagline));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setMinWidth(200); // keeps the search box lined up with the content below
        brand.setPrefWidth(200);

        searchField.setPromptText("Search by guest name or bill number...");
        searchField.setPrefWidth(380);
        StackPane search = IconInput.wrap(searchField, "search.png"); // magnifier inside the box

        // Pushes everything after it to the far right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView bell = images.view("bell.png", 22);
        ImageView avatar = images.view("avatar.png", 34);

        userLabel.getStyleClass().add("user-name");
        roleLabel.getStyleClass().add("user-role");
        VBox userBox = new VBox(2, userLabel, roleLabel);

        logoutButton.getStyleClass().add("secondary-button");

        HBox bar = new HBox(16, brand, search, spacer, bell, avatar, userBox, logoutButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 10, 20));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    // ---- What the controller is allowed to see and do ----
    public Parent getRoot() { return root; }
    public Button getNavButton(Screen screen) { return navButtons.get(screen); }
    public Button getLogoutButton() { return logoutButton; }
    public TextField getSearchField() { return searchField; }

    /** Replaces whatever panel is showing with a new one. */
    public void setContent(Node panel) {
        contentArea.getChildren().setAll(panel);
    }

    /** Shows the signed-in user at the top right. */
    public void setUserInfo(String username, String roleName) {
        userLabel.setText(username);
        roleLabel.setText(roleName);
    }

    /**
     * Marks one sidebar button as the current section. Its colours come from
     * ".nav-button.active" in style.css. The active icon is darkened to match
     * the dark text on the orange background.
     */
    public void setActive(Screen active) {
        navButtons.forEach((screen, button) -> {
            button.getStyleClass().remove("active");
            navIcons.get(screen).setEffect(null); // back to plain white
            if (screen == active) {
                button.getStyleClass().add("active");
                navIcons.get(screen).setEffect(darken);
            }
        });
    }

    /**
     * Shows or hides the admin-only items (Users, Prices).
     * Setting "managed" to false also removes the gap they would leave.
     */
    public void setAdminItemsVisible(boolean visible) {
        for (Screen screen : Screen.values()) {
            if (screen.isAdminOnly()) {
                Button button = navButtons.get(screen);
                button.setVisible(visible);
                button.setManaged(visible);
            }
        }
    }
}