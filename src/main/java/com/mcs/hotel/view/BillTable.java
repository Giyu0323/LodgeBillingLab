package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.MoneyFormat;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The bills table, reused by the Dashboard, Delete Bill and View Receipts screens.
 * A row gets a View button only if someone registered with onView(), and a
 * Delete button only if someone registered with onDelete(). That lets each
 * screen show just the buttons it needs.
 * VIEW ONLY: a click just hands the bill to the registered handler.
 */
public class BillTable {

    private final TableView<Bill> table = new TableView<>();
    private Consumer<Bill> viewHandler;   // null = no View button
    private Consumer<Bill> deleteHandler; // null = no Delete button

    public BillTable() {
        table.getColumns().add(textColumn("Bill", Bill::id));
        table.getColumns().add(textColumn("Guest", Bill::guestName));
        table.getColumns().add(textColumn("Room", Bill::roomNumber));
        table.getColumns().add(textColumn("Nights", b -> String.valueOf(b.nights())));
        table.getColumns().add(textColumn("Total", b -> MoneyFormat.k(b.total())));
        table.getColumns().add(textColumn("Paid", b -> MoneyFormat.k(b.amountPaid())));
        table.getColumns().add(textColumn("Balance", b -> MoneyFormat.k(b.balance())));
        table.getColumns().add(actionsColumn());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No bills found."));
    }

    /** One text column. "text" says how to get the cell's text from a Bill. */
    private TableColumn<Bill, String> textColumn(String title, Function<Bill, String> text) {
        TableColumn<Bill, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Actions column: View (eye) and/or Delete (trash) buttons in every filled row. */
    private TableColumn<Bill, Void> actionsColumn() {
        TableColumn<Bill, Void> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setCellFactory(c -> new TableCell<>() {
            // Each button needs its OWN icon object: one icon can only be in one place
            private final Button viewButton = new Button("View", images.view("eye.png", 14));
            private final Button deleteButton = new Button("Delete", images.view("nav-delete.png", 14));
            private final HBox box = new HBox(6);

            { // runs once when the cell is created
                deleteButton.getStyleClass().add("danger-button"); // red button (style.css)
                viewButton.getStyleClass().add("view-button");     // orange button (style.css)
                viewButton.setOnAction(e -> {
                    Bill bill = getTableRow().getItem(); // the bill on THIS row
                    if (bill != null && viewHandler != null) {
                        viewHandler.accept(bill);
                    }
                });
                deleteButton.setOnAction(e -> {
                    Bill bill = getTableRow().getItem();
                    if (bill != null && deleteHandler != null) {
                        deleteHandler.accept(bill);
                    }
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
                if (viewHandler != null) box.getChildren().add(viewButton);
                if (deleteHandler != null) box.getChildren().add(deleteButton);
                setGraphic(box);
            }
        });
        return col;
    }

    // ---- What the panels and controllers are allowed to do ----
    public TableView<Bill> getNode() { return table; }

    public void setBills(List<Bill> bills) { table.getItems().setAll(bills); }

    /** Adds a View button to every row; the handler receives that row's bill. */
    public void onView(Consumer<Bill> handler) { this.viewHandler = handler; }

    /** Adds a Delete button to every row; the handler receives that row's bill. */
    public void onDelete(Consumer<Bill> handler) { this.deleteHandler = handler; }

    /** Used by the Dashboard to keep the table a fixed height. */
    public void setFixedHeight(double height) {
        table.setPrefHeight(height);
        table.setMinHeight(height);
    }
}