package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import com.mcs.hotel.util.images;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The Dashboard home screen: heading, four tinted stat cards, the Saved Bills
 * table, Room Prices and Quick Info, each card with its own header icon.
 * VIEW ONLY: it receives ready-made data and shows it.
 */
public class HomePanel {

    private final ScrollPane root = new ScrollPane(); // scrolls if the window is short
    private final VBox content = new VBox(20);

    // Stat card labels. The controller updates these through the setters below.
    private final Label roomsValue = new Label("0");
    private final Label roomsNote = new Label();
    private final Label guestsValue = new Label("0");
    private final Label guestsNote = new Label();
    private final Label revenueValue = new Label("K 0.00");
    private final Label revenueNote = new Label();
    private final Label billsValue = new Label("0");
    private final Label billsNote = new Label();

    private final BillTable billTable = new BillTable();

    // Containers whose rows are rebuilt each time the data changes
    private final VBox roomPricesBox = new VBox();
    private final VBox quickInfoBox = new VBox(8);

    public HomePanel() {
        Label title = new Label("Welcome Back");
        title.getStyleClass().add("page-title");

        // The second word picks each card's tint (see style.css)
        HBox stats = new HBox(16,
                buildStatCard("Total Rooms", "stat-rooms.png", "stat-blue", roomsValue, roomsNote),
                buildStatCard("Today's Guests", "stat-guests.png", "stat-green", guestsValue, guestsNote),
                buildStatCard("Total Revenue", "stat-revenue.png", "stat-orange", revenueValue, revenueNote),
                buildStatCard("Bills This Session", "stat-bills.png", "stat-purple", billsValue, billsNote));

        billTable.setFixedHeight(220);

        HBox lowerRow = new HBox(16,
                buildCard("Room Prices", images.view("bed.png", 22), roomPricesBox),
                buildCard("Quick Info", images.view("header-bulb.png", 30), quickInfoBox));

        content.setPadding(new Insets(24));
        content.getChildren().addAll(title, stats,
                buildCard("Saved Bills", images.view("header-list.png", 22), billTable.getNode()),
                lowerRow);

        root.setContent(content);
        root.setFitToWidth(true); // content stretches to the window width
    }

    /** One stat card: a coloured icon on top, then the title, a big number and a note. */
    private VBox buildStatCard(String title, String iconFile, String tint, Label value, Label note) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        value.getStyleClass().add("stat-value");
        note.getStyleClass().add("stat-note");

        VBox card = new VBox(6, images.view(iconFile, 40), titleLabel, value, note);
        styleAsCard(card);
        card.getStyleClass().addAll("stat-card", tint);
        return card;
    }

    /** A larger card: a header (icon + title) with some content below it. */
    private VBox buildCard(String title, Node icon, Node body) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");

        HBox header = new HBox(10, icon, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, header, body);
        styleAsCard(card);
        return card;
    }

    /** Shared card layout: equal share of the row. The look comes from "card" in style.css. */
    private void styleAsCard(VBox card) {
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS); // cards split the row evenly
    }

    // ---- What the controller is allowed to do ----

    public Node getRoot() { return root; }

    public void setRooms(int total, int occupied, int available) {
        roomsValue.setText(String.valueOf(total));
        roomsNote.setText(occupied + " occupied • " + available + " available");
    }

    public void setTodaysGuests(int count) {
        guestsValue.setText(String.valueOf(count));
        guestsNote.setText("Checked in today");
    }

    public void setRevenue(String amount) {
        revenueValue.setText(amount);
        revenueNote.setText("Amount received so far");
    }

    public void setBillCount(int count, String lastUpdated) {
        billsValue.setText(String.valueOf(count));
        billsNote.setText("Last updated: " + lastUpdated);
    }

    /** Replaces the rows of the Saved Bills table. */
    public void setBills(List<Bill> bills) { billTable.setBills(bills); }

    /** The controller passes the method to run when a row's View button is clicked. */
    public void onViewBill(Consumer<Bill> handler) { billTable.onView(handler); }

    /** The controller passes the method to run when a row's Delete button is clicked. */
    public void onDeleteBill(Consumer<Bill> handler) { billTable.onDelete(handler); }

    /**
     * Rebuilds the Room Prices rows. Each entry is e.g. "Standard" -> "K 350.00".
     * The small "per night" is added under every price here.
     */
    public void setRoomPrices(Map<String, String> rows) {
        roomPricesBox.getChildren().clear();
        rows.forEach((name, price) -> {
            Label amount = new Label(price);
            amount.getStyleClass().add("price-amount");
            Label unit = new Label("per night");
            unit.getStyleClass().add("price-unit");
            VBox priceBox = new VBox(1, amount, unit);
            priceBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS); // pushes the price to the right edge

            HBox row = new HBox(8, new Label(name), spacer, priceBox);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("price-row"); // thin line under each row
            roomPricesBox.getChildren().add(row);
        });
    }

    /** Rebuilds the Quick Info list, one orange tick per line. */
    public void setQuickInfo(List<String> lines) {
        quickInfoBox.getChildren().clear();
        for (String line : lines) {
            Label row = new Label(line, images.view("check.png", 16));
            row.setGraphicTextGap(10);
            quickInfoBox.getChildren().add(row);
        }
    }
}