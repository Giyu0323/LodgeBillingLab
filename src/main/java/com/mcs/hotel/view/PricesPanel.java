package com.mcs.hotel.view;

import com.mcs.hotel.model.ExtraType;
import com.mcs.hotel.model.PricingBasis;
import com.mcs.hotel.model.RoomType;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.MoneyFormat;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Prices screen: "Room Rates" on the left, "Extra Charges" on the right.
 * VIEW ONLY: it never validates a price or changes one. It shows values,
 * and tells the controller when a button is clicked.
 */
public class PricesPanel {

    private final VBox root = new VBox(12);

    // Room Rates card
    private final Map<RoomType, TextField> rateFields = new EnumMap<>(RoomType.class);
    private final Label rateMessage = new Label();

    // Extra Charges card
    private final TableView<ExtraType> table = new TableView<>();
    private final Button addExtraButton = new Button("Add Extra", images.viewDark("plus.png", 14));
    private final Label extraMessage = new Label();

    // Set by the controller
    private BiConsumer<RoomType, String> saveRateHandler = (type, text) -> {};
    private Consumer<ExtraType> editHandler = extra -> {};
    private Consumer<ExtraType> removeHandler = extra -> {};

    public PricesPanel() {
        prepareMessage(rateMessage);
        prepareMessage(extraMessage);

        // ---- Room Rates card: for each room, a label above a box with a Save button ----
        VBox ratesBox = new VBox(14);
        for (RoomType type : RoomType.values()) {
            TextField field = new TextField();
            field.setPromptText("0.00");
            field.setMaxWidth(Double.MAX_VALUE);
            rateFields.put(type, field);

            Button save = new Button("Save");
            save.getStyleClass().add("primary-button");
            save.setMinWidth(Region.USE_PREF_SIZE); // never shrink: a squeezed button shows "..." instead of "Save"
            save.setOnAction(e -> saveRateHandler.accept(type, field.getText()));
            field.setOnAction(e -> save.fire()); // pressing Enter in the box also saves

            Region fieldBox = IconInput.wrap(field, "coin.png"); // coin with a K inside the box
            HBox.setHgrow(fieldBox, Priority.ALWAYS);
            HBox inputRow = new HBox(10, fieldBox, save);
            inputRow.setAlignment(Pos.CENTER_LEFT);

            ratesBox.getChildren().add(new VBox(4, new Label(type.toString()), inputRow));
        }
        Label hint = new Label("Price per night. New prices apply to new bills only.");
        hint.getStyleClass().add("page-subtitle");
        hint.setWrapText(true);

        VBox ratesCard = card(header(images.view("bed.png", 22), "Room Rates"),
                hint, ratesBox, rateMessage);
        ratesCard.setPrefWidth(380);
        ratesCard.setMinWidth(380);
        ratesCard.setMaxHeight(Region.USE_PREF_SIZE); // don't stretch to the table's height

        // ---- Extra Charges card: the Add Extra button sits in the header, at the right ----
        addExtraButton.getStyleClass().add("primary-button");
        buildTable();

        VBox extrasCard = card(
                header(images.view("stat-revenue.png", 30), "Extra Charges", addExtraButton),
                table, extraMessage);
        VBox.setVgrow(table, Priority.ALWAYS);       // the table fills its card
        extrasCard.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(extrasCard, Priority.ALWAYS);  // the card takes the remaining width

        HBox row = new HBox(16, ratesCard, extrasCard);
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(row, Priority.ALWAYS);

        root.setPadding(new Insets(24));
        root.getChildren().add(row);
    }

    /** Columns: name, price in K, basis badge (Per Day / One-time), and Edit + Remove buttons. */
    private void buildTable() {
        table.getColumns().add(textColumn("Name", ExtraType::getName));
        table.getColumns().add(textColumn("Price in K", e -> MoneyFormat.k(e.getPrice())));
        table.getColumns().add(basisColumn());
        table.getColumns().add(actionsColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No extra charges yet. Click Add Extra."));
    }

    /** One text column. "text" says how to get the cell's text from an ExtraType. */
    private TableColumn<ExtraType, String> textColumn(String title, Function<ExtraType, String> text) {
        TableColumn<ExtraType, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Basis column: a blue "Per Day" badge or a purple "One-time" badge. */
    private TableColumn<ExtraType, String> basisColumn() {
        TableColumn<ExtraType, String> col = textColumn("Basis", e -> e.getBasis().toString());
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String basis, boolean empty) {
                super.updateItem(basis, empty);
                setText(null); // the badge replaces the plain text
                if (empty || basis == null) {
                    setGraphic(null); // JavaFX reuses cells, so always reset
                    return;
                }
                Label badge = new Label(basis);
                badge.getStyleClass().addAll("badge",
                        basis.equals(PricingBasis.PER_DAY.toString()) ? "badge-perday" : "badge-onetime");
                setGraphic(badge);
            }
        });
        return col;
    }

    /** The Actions column: an orange Edit (pencil) and a red Remove (trash) button in every filled row. */
    private TableColumn<ExtraType, Void> actionsColumn() {
        TableColumn<ExtraType, Void> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setCellFactory(c -> new TableCell<>() {
            // Each button needs its OWN icon object: one icon can only be in one place
            private final Button editButton = new Button("Edit", images.view("pencil.png", 14));
            private final Button removeButton = new Button("Remove", images.view("nav-delete.png", 14));
            private final HBox box = new HBox(6, editButton, removeButton);

            { // runs once when the cell is created
                editButton.getStyleClass().add("view-button");     // orange button (style.css)
                removeButton.getStyleClass().add("danger-button"); // red button (style.css)
                editButton.setOnAction(e -> {
                    ExtraType extra = getTableRow().getItem(); // the extra on THIS row
                    if (extra != null) editHandler.accept(extra);
                });
                removeButton.setOnAction(e -> {
                    ExtraType extra = getTableRow().getItem();
                    if (extra != null) removeHandler.accept(extra);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // JavaFX reuses cells, so the buttons must be reset every time
                setGraphic(empty ? null : box);
            }
        });
        return col;
    }

    // ---------- Small layout helpers ----------

    /** A message label starts hidden and takes no space until it is used. */
    private void prepareMessage(Label label) {
        label.setWrapText(true);
        label.setVisible(false);
        label.managedProperty().bind(label.visibleProperty());
    }

    /** A card header: an icon, a title, and optionally something pushed to the far right. */
    private HBox header(Node icon, String title, Node... atRight) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        HBox header = new HBox(10, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);
        if (atRight.length > 0) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().add(spacer);
            header.getChildren().addAll(atRight);
        }
        return header;
    }

    /** A card with a ready-made header. Its look comes from the "card" class in style.css. */
    private VBox card(HBox header, Node... content) {
        VBox card = new VBox(12, header);
        card.getChildren().addAll(content);
        card.getStyleClass().add("card");
        return card;
    }

    /** Shows a message in red (error) or green (success). */
    private void show(Label label, String text, boolean error) {
        label.setText(text);
        label.getStyleClass().removeAll("message-error", "message-success");
        label.getStyleClass().add(error ? "message-error" : "message-success");
        label.setVisible(true);
    }

    // ---------- What the controller is allowed to see and do ----------

    public Node getRoot() { return root; }
    public Button getAddExtraButton() { return addExtraButton; }

    /** The controller passes the method to run when a room's Save button is clicked. */
    public void onSaveRate(BiConsumer<RoomType, String> handler) { this.saveRateHandler = handler; }

    /** The controller passes the methods to run for a row's Edit and Remove buttons. */
    public void onEditExtra(Consumer<ExtraType> handler) { this.editHandler = handler; }
    public void onRemoveExtra(Consumer<ExtraType> handler) { this.removeHandler = handler; }

    /** Puts a price into one room's text box. */
    public void setRate(RoomType type, String text) { rateFields.get(type).setText(text); }

    /** Replaces the rows of the extras table. */
    public void setExtras(List<ExtraType> extras) { table.getItems().setAll(extras); }

    public void showRateMessage(String text, boolean error) { show(rateMessage, text, error); }
    public void showExtraMessage(String text, boolean error) { show(extraMessage, text, error); }
}