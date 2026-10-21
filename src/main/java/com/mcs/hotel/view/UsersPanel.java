package com.mcs.hotel.view;

import com.mcs.hotel.model.Role;
import com.mcs.hotel.model.User;
import com.mcs.hotel.util.images;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Users screen: an "Add User" form on the left, the list of users on the right.
 * VIEW ONLY: it never checks a password or decides who may be deleted.
 * It exposes its inputs and tells the controller when a button is clicked.
 */
public class UsersPanel {

    private final VBox root = new VBox(12);

    // Add User form
    private final TextField usernameField = new TextField();
    private final PasswordInput passwordInput = new PasswordInput("Enter password");
    private final PasswordInput confirmInput = new PasswordInput("Confirm password");
    private final ComboBox<Role> roleBox = new ComboBox<>();
    private final Button addButton = new Button("Add User", images.viewDark("user-plus.png", 18));
    private final Label messageLabel = new Label();

    // Users list
    private final TableView<User> table = new TableView<>();
    private Consumer<User> deleteHandler = user -> {}; // set by the controller

    public UsersPanel() {
        usernameField.setPromptText("Enter username");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        roleBox.getItems().addAll(Role.values());
        roleBox.setValue(Role.STAFF); // safest default: new users are not admins
        roleBox.setMaxWidth(Double.MAX_VALUE);

        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setDefaultButton(true); // Enter in the form clicks Add User
        addButton.getStyleClass().addAll("primary-button", "save-big");

        // The message line starts hidden and takes no space until it is used
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        VBox formCard = iconCard("Add User", images.view("user-plus.png", 24),
                field("Username", IconInput.wrap(usernameField, "user.png")),
                field("Password", passwordInput.getNode()),
                field("Confirm password", confirmInput.getNode()),
                field("Role", roleBox),
                addButton, messageLabel);
        formCard.setPrefWidth(340);
        formCard.setMinWidth(340);
        formCard.setMaxHeight(Region.USE_PREF_SIZE); // don't stretch to the table's height

        buildTable();
        VBox listCard = iconCard("Users", images.view("header-users.png", 28), table);
        VBox.setVgrow(table, Priority.ALWAYS);       // the table fills its card
        listCard.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(listCard, Priority.ALWAYS);    // the list takes the remaining width

        HBox row = new HBox(16, formCard, listCard);
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(row, Priority.ALWAYS);

        root.setPadding(new Insets(24));
        root.getChildren().add(row);
    }

    /** Sets up the columns: username, role badge, created date, and a Delete button. */
    private void buildTable() {
        table.getColumns().add(textColumn("Username", User::getUsername));
        table.getColumns().add(roleColumn());
        table.getColumns().add(textColumn("Created", u -> u.getCreatedOn().toString()));
        table.getColumns().add(actionsColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No users found."));
    }

    /** One text column. "text" says how to get the cell's text from a User. */
    private TableColumn<User, String> textColumn(String title, Function<User, String> text) {
        TableColumn<User, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Role column: a gold-outline "Admin" badge or a blue "Staff" badge. */
    private TableColumn<User, String> roleColumn() {
        TableColumn<User, String> col = textColumn("Role", u -> u.getRole().toString());
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                setText(null); // the badge replaces the plain text
                if (empty || role == null) {
                    setGraphic(null); // JavaFX reuses cells, so always reset
                    return;
                }
                Label badge = new Label(role);
                badge.getStyleClass().addAll("badge",
                        role.equals(Role.ADMIN.toString()) ? "badge-admin" : "badge-staff");
                setGraphic(badge);
            }
        });
        return col;
    }

    /** The Actions column: a Delete button (with a trash icon) in every filled row. */
    private TableColumn<User, Void> actionsColumn() {
        TableColumn<User, Void> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete", images.view("nav-delete.png", 14));

            { // runs once when the cell is created
                deleteButton.getStyleClass().add("danger-button"); // red button (style.css)
                deleteButton.setOnAction(e -> {
                    User user = getTableRow().getItem(); // the user on THIS row
                    if (user != null) {
                        deleteHandler.accept(user);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // JavaFX reuses cells, so the button must be reset every time
                setGraphic(empty ? null : deleteButton);
            }
        });
        return col;
    }

    // ---------- Small layout helpers ----------

    /** A label above an input, with the input stretched to full width. */
    private VBox field(String label, Region input) {
        input.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, new Label(label), input);
    }

    /** A card with an icon and a title in its header. Its look comes from "card" in style.css. */
    private VBox iconCard(String title, Node icon, Node... content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        HBox header = new HBox(10, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, header);
        card.getChildren().addAll(content);
        card.getStyleClass().add("card");
        return card;
    }

    // ---------- What the controller is allowed to see and do ----------

    public Node getRoot() { return root; }
    public Button getAddButton() { return addButton; }

    public String getUsername() { return usernameField.getText(); }
    public String getPassword() { return passwordInput.getText(); }
    public String getConfirm() { return confirmInput.getText(); }
    public Role getRole() { return roleBox.getValue(); }

    /** The controller passes the method to run when a row's Delete button is clicked. */
    public void onDeleteUser(Consumer<User> handler) { this.deleteHandler = handler; }

    /** Replaces the rows of the users table. */
    public void setUsers(List<User> users) { table.getItems().setAll(users); }

    /** Shows a red error or a green success line under the Add User button. */
    public void showMessage(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.getStyleClass().add(error ? "message-error" : "message-success");
        messageLabel.setVisible(true);
    }

    /** Empties the form after a successful add. Passwords are never left on screen. */
    public void clearForm() {
        passwordInput.clear();
        confirmInput.clear();
        usernameField.clear();
        roleBox.setValue(Role.STAFF);
        usernameField.requestFocus();
    }
}