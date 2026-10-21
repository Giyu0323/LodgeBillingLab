package com.mcs.hotel.view;

import com.mcs.hotel.util.images;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * The login screen: hotel photo, logo, slogan and feature icons on the left,
 * a tall sign-in card on the right.
 * VIEW ONLY: it builds controls and exposes them. It never checks passwords;
 * that is the controller's and AuthService's job.
 */
public class LoginView {

    private final HBox root = new HBox();
    private final TextField usernameField = new TextField();

    // A PasswordField cannot reveal its text, so a normal TextField sits on top
    // of it and shares the same text. The eye button swaps which one is visible.
    private final PasswordField passwordField = new PasswordField();
    private final TextField passwordVisible = new TextField();
    private final ToggleButton eyeButton = new ToggleButton();

    private final CheckBox rememberBox = new CheckBox("Remember me");
    private final Button signInButton = new Button("Sign In  →");
    private final Label errorLabel = new Label();

    public LoginView() {
        root.getChildren().addAll(buildBrandPanel(), buildFormPanel());
    }

    /**
     * Left side: the photo and a dark shade, then three things on top of it:
     * the logo block near the top, and the slogan with the feature icons at
     * the bottom left.
     */
    private StackPane buildBrandPanel() {
        Region photo = new Region();                 // the picture comes from style.css
        photo.getStyleClass().add("login-photo");
        Region shade = new Region();                 // darkens the photo so text stays readable
        shade.getStyleClass().add("login-shade");

        // ---- Logo block, upper middle ----
        ImageView logo = images.view("logo.png", 150);
        Label name = new Label("MCS Hotel");
        name.getStyleClass().add("brand-title");
        Label tagline = new Label("STAY  ·  RELAX  ·  ENJOY"); // extra spaces stand in for letter spacing
        tagline.getStyleClass().add("brand-tagline");

        VBox logoBlock = new VBox(8, logo, name, tagline);
        logoBlock.setAlignment(Pos.TOP_CENTER);
        logoBlock.getStyleClass().add("login-brand");
        logoBlock.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(logoBlock, Pos.TOP_CENTER);
        StackPane.setMargin(logoBlock, new Insets(60, 0, 0, 0));

        // ---- Slogan and feature icons, bottom left ----
        Label slogan = new Label("Efficient billing for a better guest experience");
        slogan.getStyleClass().add("login-slogan");
        slogan.setWrapText(true);
        slogan.setMaxWidth(340); // wraps onto two lines, as in the design

        HBox features = new HBox(34,
                feature("bed.png", "Manage Rooms"),
                feature("nav-receipts.png", "Track Bills"),
                feature("shield.png", "Keep Records Safe"));
        features.setAlignment(Pos.TOP_LEFT);

        VBox bottomBlock = new VBox(26, slogan, features);
        bottomBlock.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(bottomBlock, Pos.BOTTOM_LEFT);
        StackPane.setMargin(bottomBlock, new Insets(0, 0, 44, 48));

        StackPane brand = new StackPane(photo, shade, logoBlock, bottomBlock);
        // The photo side takes 56% of the window width, even when it is resized
        brand.prefWidthProperty().bind(root.widthProperty().multiply(0.56));
        brand.minWidthProperty().bind(root.widthProperty().multiply(0.56));
        brand.maxWidthProperty().bind(root.widthProperty().multiply(0.56));
        return brand;
    }

    /** One feature highlight: an icon in a circle with a caption (up to two lines) underneath. */
    private VBox feature(String iconFile, String caption) {
        StackPane circle = new StackPane(images.view(iconFile, 26));
        circle.getStyleClass().add("feature-circle");

        Label label = new Label(caption);
        label.getStyleClass().add("feature-label");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setPrefWidth(76); // narrow enough that "Keep Records Safe" wraps to two lines
        label.setMaxWidth(76);

        VBox box = new VBox(8, circle, label);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /** Right side: a tall sign-in card, with the form at the top of it. */
    private StackPane buildFormPanel() {
        Label title = new Label("Admin Login");
        title.getStyleClass().add("form-title");
        Label subtitle = new Label("Sign in to access your MCS Hotel account");
        subtitle.getStyleClass().add("form-subtitle");

        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.getStyleClass().add("login-input"); // roomier padding, see style.css

        signInButton.setDefaultButton(true);          // pressing Enter triggers Sign In
        signInButton.setMaxWidth(Double.MAX_VALUE);   // stretch to the card width
        signInButton.getStyleClass().addAll("primary-button", "signin-button");

        // The error label starts hidden. Binding "managed" to "visible" means a
        // hidden label takes up no space in the layout.
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setGraphic(images.view("warning.png", 18)); // warning triangle before the text
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setVisible(false);
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        VBox card = new VBox(14, title, subtitle,
                new Label("Username"), IconInput.wrap(usernameField, "user.png"),
                new Label("Password"), buildPasswordBox(),
                rememberBox, signInButton, errorLabel);
        card.setPadding(new Insets(30));
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // fill the whole right side, top to bottom
        card.getStyleClass().add("login-card");

        StackPane holder = new StackPane(card);
        holder.setPadding(new Insets(24));
        HBox.setHgrow(holder, Priority.ALWAYS); // fill the space beside the photo
        return holder;
    }

    /** The password box: lock icon, hidden field, visible field and the eye button. */
    private StackPane buildPasswordBox() {
        passwordField.setPromptText("Password");
        passwordVisible.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordVisible.setMaxWidth(Double.MAX_VALUE);
        // Padding for BOTH the lock on the left and the eye on the right (see style.css)
        passwordField.getStyleClass().addAll("icon-input", "password-input", "login-input");
        passwordVisible.getStyleClass().addAll("icon-input", "password-input", "login-input");

        // Both boxes always hold the same text, so switching never loses anything
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());

        // Only one box is shown (and takes up space) at a time
        passwordVisible.setVisible(false);
        passwordVisible.managedProperty().bind(passwordVisible.visibleProperty());
        passwordField.managedProperty().bind(passwordField.visibleProperty());

        ImageView eyeOn = images.view("eye.png", 20);
        ImageView eyeOff = images.view("eye-off.png", 20);
        eyeButton.setGraphic(eyeOn);
        eyeButton.getStyleClass().add("eye-button");
        eyeButton.setTooltip(new Tooltip("Show or hide password"));
        eyeButton.setFocusTraversable(false);        // Tab goes from password to Remember me
        eyeButton.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(eyeButton, Pos.CENTER_RIGHT);

        eyeButton.selectedProperty().addListener((obs, wasShown, show) -> {
            passwordVisible.setVisible(show);
            passwordField.setVisible(!show);
            eyeButton.setGraphic(show ? eyeOff : eyeOn);
            TextField active = show ? passwordVisible : passwordField;
            active.requestFocus();
            active.positionCaret(active.getText().length()); // cursor stays at the end
        });

        Node lock = IconInput.overlay("lock.png");
        return new StackPane(passwordField, passwordVisible, lock, eyeButton);
    }

    // ---- What the controller is allowed to see and do ----
    public Parent getRoot() { return root; }
    public String getUsername() { return usernameField.getText(); }
    public String getPassword() { return passwordField.getText(); } // same text as the visible box
    public Button getSignInButton() { return signInButton; }
    public boolean isRemember() { return rememberBox.isSelected(); }

    public void setUsername(String username) { usernameField.setText(username); }
    public void setRemember(boolean remember) { rememberBox.setSelected(remember); }

    /** Puts the cursor in the username box once the window is showing. */
    public void focusUsername() { Platform.runLater(usernameField::requestFocus); }

    /** Puts the cursor in the password box once the window is showing. */
    public void focusPassword() { Platform.runLater(passwordField::requestFocus); }

    /** Shows a message under the Sign In button. */
    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /** Clears the password after a failed attempt and puts the cursor back in it. */
    public void clearPassword() {
        passwordField.clear(); // the visible box shares this text, so it clears too
        (eyeButton.isSelected() ? passwordVisible : passwordField).requestFocus();
    }
}