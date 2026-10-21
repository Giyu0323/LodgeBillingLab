package com.mcs.hotel.view;

import com.mcs.hotel.model.Expense;
import com.mcs.hotel.model.ExpenseCategory;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.MoneyFormat;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Expenses screen: an "Add Expense" form on the left, the list of expenses
 * on the right.
 * VIEW ONLY: it never checks an amount or decides what is allowed. It exposes
 * its inputs and tells the controller when a button is clicked.
 */
public class ExpensesPanel {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HBox root = new HBox(16);

    // Add Expense form
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<ExpenseCategory> categoryBox = new ComboBox<>();
    private final TextField descriptionField = new TextField();
    private final TextField amountField = new TextField();
    private final Button addButton = new Button("Add Expense", images.viewDark("plus.png", 16));
    private final Label messageLabel = new Label();

    // Expenses list
    private final TableView<Expense> table = new TableView<>();
    private final Label totalLabel = new Label();
    private Consumer<Expense> deleteHandler = expense -> {}; // set by the controller

    public ExpensesPanel() {
        datePicker.setMaxWidth(Double.MAX_VALUE);
        categoryBox.getItems().addAll(ExpenseCategory.values());
        categoryBox.setValue(ExpenseCategory.OTHER);
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        descriptionField.setPromptText("e.g. Electricity bill");
        descriptionField.setMaxWidth(Double.MAX_VALUE);
        amountField.setPromptText("0.00");
        amountField.setMaxWidth(Double.MAX_VALUE);

        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setDefaultButton(true); // Enter in the form clicks Add Expense
        addButton.getStyleClass().addAll("primary-button", "save-big");

        // The message line starts hidden and takes no space until it is used
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        VBox formCard = iconCard("Add Expense", images.view("header-plus.png", 30),
                field("Date", datePicker),
                field("Category", categoryBox),
                field("Description", descriptionField),
                field("Amount (K)", IconInput.wrap(amountField, "coin.png")),
                addButton, messageLabel);
        formCard.setPrefWidth(340);
        formCard.setMinWidth(340);
        formCard.setMaxHeight(Region.USE_PREF_SIZE); // don't stretch to the table's height

        buildTable();
        totalLabel.getStyleClass().add("footer-item");
        VBox listCard = iconCard("Expenses", images.view("header-clipboard.png", 30), table, totalLabel);
        VBox.setVgrow(table, Priority.ALWAYS);        // the table fills its card
        listCard.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(listCard, Priority.ALWAYS);     // the list takes the remaining width

        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(formCard, listCard);
    }

    /** Sets up the columns: date, category, description, amount, and a Delete button. */
    private void buildTable() {
        table.getColumns().add(textColumn("Date", e -> DAY.format(e.date())));
        table.getColumns().add(textColumn("Category", e -> e.category().toString()));
        table.getColumns().add(textColumn("Description", Expense::description));
        table.getColumns().add(textColumn("Amount", e -> MoneyFormat.k(e.amount())));
        table.getColumns().add(actionsColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No expenses recorded yet."));
    }

    /** One text column. "text" says how to get the cell's text from an Expense. */
    private TableColumn<Expense, String> textColumn(String title, Function<Expense, String> text) {
        TableColumn<Expense, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Actions column: a Delete button (with a trash icon) in every filled row. */
    private TableColumn<Expense, Void> actionsColumn() {
        TableColumn<Expense, Void> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setMinWidth(110);
        col.setCellFactory(c -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete", images.view("nav-delete.png", 14));

            { // runs once when the cell is created
                deleteButton.getStyleClass().add("danger-button"); // red button (style.css)
                deleteButton.setOnAction(e -> {
                    Expense expense = getTableRow().getItem(); // the expense on THIS row
                    if (expense != null) {
                        deleteHandler.accept(expense);
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

    public LocalDate getDate() { return datePicker.getValue(); }
    public ExpenseCategory getCategory() { return categoryBox.getValue(); }
    public String getDescription() { return descriptionField.getText(); }
    public String getAmountText() { return amountField.getText(); }

    /** The controller passes the method to run when a row's Delete button is clicked. */
    public void onDelete(Consumer<Expense> handler) { this.deleteHandler = handler; }

    /** Replaces the rows of the expenses table. */
    public void setExpenses(List<Expense> expenses) { table.getItems().setAll(expenses); }

    /** The line under the table, e.g. "Total expenses: K 1,250.00". */
    public void setTotal(String text) { totalLabel.setText(text); }

    /** Shows a red error or a green success line under the Add Expense button. */
    public void showMessage(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.getStyleClass().add(error ? "message-error" : "message-success");
        messageLabel.setVisible(true);
    }

    /** Empties the description and amount after a successful add. The date and category stay, for quick entry of several. */
    public void clearForm() {
        descriptionField.clear();
        amountField.clear();
        descriptionField.requestFocus();
    }
}