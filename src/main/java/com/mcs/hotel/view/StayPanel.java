package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A list of guests with their check-in and check-out times. Used for both the
 * Checked-In Guests page (with a Check Out button) and the Checked-Out Guests page.
 * VIEW ONLY: it shows what it is given and tells the controller what was clicked.
 */
public class StayPanel {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm");

    private final boolean checkedInMode; // true = guests staying now, false = guests who left
    private final VBox root = new VBox(16);
    private final TableView<Bill> table = new TableView<>();
    private final Label footer = new Label();

    private Consumer<Bill> checkOutHandler = bill -> {}; // set by the controller
    private Consumer<Bill> viewHandler = bill -> {};

    public StayPanel(String title, String subtitle, String iconFile, boolean checkedInMode) {
        this.checkedInMode = checkedInMode;

        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("page-subtitle");
        HBox header = new HBox(16, images.view(iconFile, 36), new VBox(2, heading, sub));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("card");

        buildTable();
        footer.getStyleClass().add("page-subtitle");

        VBox tableCard = new VBox(12, table, footer);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(table, Priority.ALWAYS);      // the table fills its card
        VBox.setVgrow(tableCard, Priority.ALWAYS);  // the card fills the page

        root.setPadding(new Insets(24));
        root.getChildren().addAll(header, tableCard);
    }

    /** Columns differ a little: guests staying show what is due, guests who left show what they paid. */
    private void buildTable() {
        table.getColumns().add(textColumn("Bill", Bill::id));
        table.getColumns().add(textColumn("Guest", Bill::guestName));
        table.getColumns().add(textColumn("Room", Bill::roomNumber));
        table.getColumns().add(textColumn("Checked in", b -> WHEN.format(b.checkIn())));
        if (checkedInMode) {
            table.getColumns().add(textColumn("Check-out due", b -> WHEN.format(b.expectedCheckOut())));
            table.getColumns().add(textColumn("Nights", b -> String.valueOf(b.nights())));
            table.getColumns().add(textColumn("Balance due", b -> MoneyFormat.k(b.balance())));
        } else {
            table.getColumns().add(textColumn("Checked out", b -> WHEN.format(b.checkOutTime())));
            table.getColumns().add(textColumn("Nights", b -> String.valueOf(b.nights())));
            table.getColumns().add(textColumn("Total", b -> MoneyFormat.k(b.total())));
            table.getColumns().add(textColumn("Paid", b -> MoneyFormat.k(b.amountPaid())));
        }
        table.getColumns().add(actionsColumn());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label(checkedInMode ? "No guests are checked in." : "No guests have checked out yet."));
    }

    /** One text column. "text" says how to get the cell's text from a Bill. */
    private TableColumn<Bill, String> textColumn(String title, Function<Bill, String> text) {
        TableColumn<Bill, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Actions column: Check Out (only for guests staying) and View. */
    private TableColumn<Bill, Void> actionsColumn() {
        TableColumn<Bill, Void> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setMinWidth(190);
        col.setCellFactory(c -> new TableCell<>() {
            // Each button needs its OWN icon object: one icon can only be in one place
            private final Button checkOutButton = new Button("Check Out", images.view("nav-checkout.png", 14));
            private final Button viewButton = new Button("View", images.view("eye.png", 14));
            private final HBox box = new HBox(6);

            { // runs once when the cell is created
                checkOutButton.getStyleClass().add("view-button"); // orange button (style.css)
                checkOutButton.setOnAction(e -> {
                    Bill bill = getTableRow().getItem(); // the guest on THIS row
                    if (bill != null) checkOutHandler.accept(bill);
                });
                viewButton.setOnAction(e -> {
                    Bill bill = getTableRow().getItem();
                    if (bill != null) viewHandler.accept(bill);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null); // JavaFX reuses cells, so always reset
                    return;
                }
                box.getChildren().clear();
                if (checkedInMode) box.getChildren().add(checkOutButton);
                box.getChildren().add(viewButton);
                setGraphic(box);
            }
        });
        return col;
    }

    // ---- What the controller is allowed to do ----

    public Node getRoot() { return root; }

    /** The controller passes the method to run when Check Out is clicked on a row. */
    public void onCheckOut(Consumer<Bill> handler) { this.checkOutHandler = handler; }

    /** The controller passes the method to run when View is clicked on a row. */
    public void onView(Consumer<Bill> handler) { this.viewHandler = handler; }

    /** Replaces the rows and updates the count under the table. */
    public void setBills(List<Bill> bills) {
        table.getItems().setAll(bills);
        footer.setText("Total guests: " + bills.size());
    }
}