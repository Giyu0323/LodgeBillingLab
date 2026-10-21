package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.MoneyFormat;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
 * The Guest Payments page: counts of guests staying and gone, two date filters,
 * a table showing who has paid and who still owes, and totals underneath.
 * VIEW ONLY: it shows what it is given and tells the controller what was clicked.
 */
public class GuestPaymentsPanel {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm");

    private final VBox root = new VBox(16);

    private final Label checkedInValue = new Label("0");
    private final Label checkedOutValue = new Label("0");
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker untilPicker = new DatePicker();

    private final TableView<Bill> table = new TableView<>();

    private final Label guestsTotal = new Label();
    private final Label paidTotal = new Label();
    private final Label dueTotal = new Label();
    private final Label asAt = new Label();

    // Set by the controller
    private Consumer<Bill> payNowHandler = bill -> {};
    private Consumer<Bill> detailsHandler = bill -> {};
    private Runnable filtersChanged = () -> {};

    public GuestPaymentsPanel() {
        // ---- Header ----
        Label title = new Label("Guest Payments");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Track guest payments, outstanding balances and payment history.");
        subtitle.getStyleClass().add("page-subtitle");
        HBox header = new HBox(16, images.view("nav-payments.png", 36), new VBox(2, title, subtitle));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("card");

        // ---- Two count cards and two date filters ----
        allowClearing(fromPicker);
        allowClearing(untilPicker);
        HBox stats = new HBox(16,
                countCard("Checked-In Guests", "nav-checkin.png", checkedInValue,
                        "●  Currently staying", "accent-green", "note-green"),
                countCard("Checked-Out Guests", "nav-checkout.png", checkedOutValue,
                        "●  Completed stay", "accent-blue", "note-blue"),
                dateCard("Check-in from", fromPicker),
                dateCard("Check-out until", untilPicker));

        // ---- The table and its totals ----
        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        for (Label label : List.of(guestsTotal, paidTotal, dueTotal, asAt)) {
            label.getStyleClass().add("footer-item");
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(14, guestsTotal, new Label("|"), paidTotal, new Label("|"),
                dueTotal, spacer, asAt);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox tableCard = new VBox(12, table, footer);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        root.setPadding(new Insets(24));
        root.getChildren().addAll(header, stats, tableCard);
    }

    // ---------- Building the cards ----------

    /** A card with an icon, a title, a big number and a coloured note, with an accent bar on the left. */
    private VBox countCard(String title, String iconFile, Label value, String note,
                           String accentClass, String noteClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        value.getStyleClass().add("stat-value");
        Label noteLabel = new Label(note);
        noteLabel.getStyleClass().addAll("stat-note", noteClass);

        HBox row = new HBox(16, images.view(iconFile, 40), new VBox(2, titleLabel, value, noteLabel));
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.getStyleClass().addAll("card", accentClass);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** A card holding one date filter. Leave it empty to ignore it. */
    private VBox dateCard(String title, DatePicker picker) {
        picker.setPromptText("Any date");
        picker.setMaxWidth(Double.MAX_VALUE);

        HBox head = new HBox(10, images.view("calendar.png", 20), new Label(title));
        head.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, head, picker);
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Tells the controller when a date changes, and lets the user clear one by deleting its text. */
    private void allowClearing(DatePicker picker) {
        picker.getEditor().textProperty().addListener((obs, was, now) -> {
            if (now == null || now.isBlank()) {
                picker.setValue(null);
            }
        });
        picker.valueProperty().addListener((obs, was, now) -> filtersChanged.run());
    }

    // ---------- The table ----------

    private void buildTable() {
        table.getColumns().add(textColumn("Guest Name", Bill::guestName));
        table.getColumns().add(textColumn("Room", Bill::roomNumber));
        table.getColumns().add(textColumn("Check-in Date & Time", b -> WHEN.format(b.checkIn())));
        table.getColumns().add(textColumn("Check-out Date & Time", b -> WHEN.format(b.checkOutTime())));
        table.getColumns().add(textColumn("Bill Total", b -> MoneyFormat.k(b.total())));
        table.getColumns().add(textColumn("Amount Paid", b -> MoneyFormat.k(b.amountPaid())));
        table.getColumns().add(textColumn("Balance Due", b -> MoneyFormat.k(b.balance())));
        table.getColumns().add(statusColumn());
        table.getColumns().add(actionsColumn());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No guests found."));
    }

    private TableColumn<Bill, String> textColumn(String title, Function<Bill, String> text) {
        TableColumn<Bill, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Payment Status column: a green "Paid" badge or an orange "Balance Due" badge. */
    private TableColumn<Bill, String> statusColumn() {
        TableColumn<Bill, String> col = textColumn("Payment Status",
                b -> b.isFullyPaid() ? "Paid" : "Balance Due");
        col.setMinWidth(140);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null); // the badge replaces the plain text
                if (empty || status == null) {
                    setGraphic(null); // JavaFX reuses cells, so always reset
                    return;
                }
                Label badge = new Label("●  " + status);
                badge.getStyleClass().addAll("status-badge",
                        status.equals("Paid") ? "status-active" : "status-due");
                setGraphic(badge);
            }
        });
        return col;
    }

    /**
     * The Actions column: Pay Now (only while money is owed) and View Details.
     * The column's value is the whole Bill, so each cell knows its row for sure.
     */
    private TableColumn<Bill, Bill> actionsColumn() {
        TableColumn<Bill, Bill> col = new TableColumn<>("Actions");
        col.setSortable(false);
        col.setMinWidth(210);
        col.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Bill bill, boolean empty) {
                super.updateItem(bill, empty);
                if (empty || bill == null) {
                    setGraphic(null); // JavaFX reuses cells, so always reset
                    return;
                }
                HBox box = new HBox(6);
                if (!bill.isFullyPaid()) {
                    Button payNow = new Button("Pay Now");
                    payNow.getStyleClass().add("view-button"); // orange button (style.css)
                    payNow.setOnAction(e -> payNowHandler.accept(bill));
                    box.getChildren().add(payNow);
                }
                Button details = new Button("View Details");
                details.getStyleClass().add("outline-button");
                details.setOnAction(e -> detailsHandler.accept(bill));
                box.getChildren().add(details);
                setGraphic(box);
            }
        });
        return col;
    }

    // ---- What the controller is allowed to do ----

    public Node getRoot() { return root; }

    /** The controller passes the methods to run for the two row buttons. */
    public void onPayNow(Consumer<Bill> handler) { this.payNowHandler = handler; }
    public void onViewDetails(Consumer<Bill> handler) { this.detailsHandler = handler; }

    /** The controller passes the method to run whenever either date filter changes. */
    public void onFiltersChanged(Runnable listener) { this.filtersChanged = listener; }

    /** The date filters. Null means "no limit". */
    public LocalDate getFrom() { return fromPicker.getValue(); }
    public LocalDate getUntil() { return untilPicker.getValue(); }

    public void setCounts(int checkedIn, int checkedOut) {
        checkedInValue.setText(String.valueOf(checkedIn));
        checkedOutValue.setText(String.valueOf(checkedOut));
    }

    public void setBills(List<Bill> bills) { table.getItems().setAll(bills); }

    /** The line of totals under the table. */
    public void setTotals(int guests, String paid, String due, String asAtText) {
        guestsTotal.setText("Total Guests: " + guests);
        paidTotal.setText("Total Paid: " + paid);
        dueTotal.setText("Total Due: " + due);
        asAt.setText("As at: " + asAtText);
    }
}