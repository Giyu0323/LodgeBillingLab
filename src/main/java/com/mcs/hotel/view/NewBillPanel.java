package com.mcs.hotel.view;

import com.mcs.hotel.model.ExtraSelection;
import com.mcs.hotel.model.ExtraType;
import com.mcs.hotel.model.PricingBasis;
import com.mcs.hotel.model.RoomType;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.MoneyFormat;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The New Bill screen: the form, the extras checklist and the live Bill Summary,
 * with Pay Now and Pay Later buttons.
 * VIEW ONLY: it shows inputs and numbers. It never calculates a price or
 * saves anything. It just tells the controller "something changed".
 */
public class NewBillPanel {

    /** One checklist line. "days" is null for one-time extras (they have no day count). */
    private record ExtraRow(ExtraType extra, CheckBox check, Spinner<Integer> days) {}

    // Column widths shared by the checklist header and its rows, so they line up
    private static final double NAME_COL = 190;
    private static final double PRICE_COL = 110;
    private static final double DAYS_COL = 90;

    private final ScrollPane root = new ScrollPane();

    // Form inputs
    private final TextField guestField = new TextField();
    private final ComboBox<RoomType> roomTypeBox = new ComboBox<>();
    private final Label roomHint = new Label();
    private final Spinner<Integer> nightsSpinner = new Spinner<>(1, 365, 1);
    private final TextField paidField = new TextField();   // the amount to pay NOW
    private final Label depositHint = new Label();
    private final TextField notesField = new TextField();
    private final TextField otherField = new TextField();

    // Extras checklist
    private final VBox extrasBox = new VBox(8);
    private final List<ExtraRow> extraRows = new ArrayList<>();

    // Live summary values
    private final Label roomChargeValue = new Label("K 0.00");
    private final Label discountValue = new Label("K 0.00");
    private final Label extrasValue = new Label("K 0.00");
    private final Label totalValue = new Label("K 0.00");
    private final Label payingValue = new Label("K 0.00");
    private final Label balanceValue = new Label("K 0.00");

    private final Label messageLabel = new Label();
    private final Button clearButton = new Button("Clear", images.view("refresh.png", 16));
    private final Button calculateButton = new Button("Calculate", images.viewDark("calculator.png", 16));
    private final Button payNowButton = new Button("Pay Now", images.viewDark("coin.png", 18));
    private final Button payLaterButton = new Button("Pay Later", images.view("calendar.png", 16));

    // The controller registers this. Every input change calls it.
    private Runnable changeListener = () -> {};

    public NewBillPanel() {
        roomTypeBox.getItems().addAll(RoomType.values());
        roomTypeBox.setValue(RoomType.STANDARD);
        roomTypeBox.setMaxWidth(Double.MAX_VALUE);
        roomTypeBox.getStyleClass().add("combo-icon");     // text clears the icon (style.css)
        nightsSpinner.setEditable(true);
        nightsSpinner.setMaxWidth(Double.MAX_VALUE);
        nightsSpinner.getStyleClass().add("icon-spinner"); // text clears the icon (style.css)

        guestField.setPromptText("Enter guest name");
        paidField.setPromptText("0.00");
        notesField.setPromptText("Any additional notes...");
        otherField.setPromptText("0.00");

        // The text boxes with an icon inside must still stretch to the full width
        guestField.setMaxWidth(Double.MAX_VALUE);
        paidField.setMaxWidth(Double.MAX_VALUE);
        notesField.setMaxWidth(Double.MAX_VALUE);
        otherField.setMaxWidth(Double.MAX_VALUE);

        depositHint.getStyleClass().add("page-subtitle");
        depositHint.setWrapText(true);

        // Any change to an input tells the controller to refresh the summary
        roomTypeBox.valueProperty().addListener((o, a, b) -> fireChange());
        nightsSpinner.valueProperty().addListener((o, a, b) -> {
            syncDaysToNights(); // per-day extras follow the number of nights
            fireChange();
        });
        paidField.textProperty().addListener((o, a, b) -> fireChange());
        otherField.textProperty().addListener((o, a, b) -> fireChange());

        // The message line starts hidden and takes no space until it is used
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        payNowButton.setMaxWidth(Double.MAX_VALUE);
        payNowButton.getStyleClass().addAll("primary-button", "save-big");
        payLaterButton.setMaxWidth(Double.MAX_VALUE);
        payLaterButton.getStyleClass().add("secondary-button");
        calculateButton.getStyleClass().add("primary-button");
        clearButton.getStyleClass().add("secondary-button");

        // Icons float over the left edge of the room type and nights boxes
        StackPane roomBox = new StackPane(roomTypeBox, IconInput.overlay("bed.png"));
        StackPane nightsBox = new StackPane(nightsSpinner, IconInput.overlay("calendar.png"));

        VBox formCard = iconCard("Create New Bill", images.view("header-doc.png", 30),
                field("Guest name", IconInput.wrap(guestField, "user.png")),
                field("Room type", roomBox),
                roomHint,
                new HBox(12,
                        field("Nights", nightsBox),
                        field("Amount to pay now (K)", IconInput.wrap(paidField, "coin.png"))),
                depositHint,
                field("Notes (optional)", IconInput.wrap(notesField, "pencil.png")));

        VBox extrasCard = iconCard("Extra Charges", images.view("header-plus.png", 30),
                extrasHeader(),
                extrasBox,
                field("Other (K), optional one-off charge", IconInput.wrap(otherField, "coin.png")));

        VBox left = new VBox(16, formCard, extrasCard);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Clear and Calculate share the width equally
        clearButton.setMaxWidth(Double.MAX_VALUE);
        calculateButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(clearButton, Priority.ALWAYS);
        HBox.setHgrow(calculateButton, Priority.ALWAYS);
        HBox buttons = new HBox(10, clearButton, calculateButton);

        Region divider = new Region(); // thin orange line above the Total
        divider.getStyleClass().add("orange-line");

        Region filler = new Region();  // pushes the buttons to the bottom of the card
        VBox.setVgrow(filler, Priority.ALWAYS);

        VBox summaryCard = iconCard("Bill Summary", images.view("header-clipboard.png", 30),
                summaryRow("Room charge", roomChargeValue, false),
                summaryRow("Discount", discountValue, false),
                summaryRow("Extras", extrasValue, false),
                divider,
                summaryRow("Total", totalValue, true),
                summaryRow("Paying now", payingValue, false),
                summaryRow("Balance", balanceValue, true),
                filler, buttons, payNowButton, payLaterButton, messageLabel);
        summaryCard.setMinWidth(320);
        summaryCard.setPrefWidth(320);

        HBox content = new HBox(16, left, summaryCard);
        content.setPadding(new Insets(24));

        root.setContent(content);
        root.setFitToWidth(true); // content stretches to the window width
    }

    // ---------- Building the extras checklist ----------

    /** The column titles above the checklist: Name, Price (K), Days. */
    private HBox extrasHeader() {
        Label name = headLabel("Name", NAME_COL);
        name.setPadding(new Insets(0, 0, 0, 28)); // lines up with the text, not the tick box
        return new HBox(12, name, headLabel("Price (K)", PRICE_COL), headLabel("Days", DAYS_COL));
    }

    private Label headLabel(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("extras-head");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        return label;
    }

    /** Builds one row per extra. Called by the controller with the current price list. */
    public void setExtras(List<ExtraType> extras) {
        extrasBox.getChildren().clear();
        extraRows.clear();

        for (ExtraType extra : extras) {
            boolean perDay = extra.getBasis() == PricingBasis.PER_DAY;

            CheckBox check = new CheckBox(extra.getName() + (perDay ? " per day" : " (one-time)"));
            check.setMinWidth(NAME_COL);
            check.setPrefWidth(NAME_COL);
            Label price = new Label(MoneyFormat.k(extra.getPrice()));
            price.setMinWidth(PRICE_COL);
            price.setPrefWidth(PRICE_COL);

            Spinner<Integer> days = null;
            Node daysNode;
            if (perDay) {
                days = new Spinner<>(1, currentNights(), currentNights());
                days.setEditable(true);
                days.setPrefWidth(DAYS_COL);
                days.setDisable(true); // only usable once the box is ticked
                days.valueProperty().addListener((o, a, b) -> fireChange());
                daysNode = days;
            } else {
                daysNode = new Label("1");
            }

            final Spinner<Integer> daysSpinner = days; // effectively-final copy for the lambda
            check.selectedProperty().addListener((o, was, ticked) -> {
                if (daysSpinner != null) {
                    daysSpinner.setDisable(!ticked);
                    if (ticked) {
                        // A newly ticked extra covers the whole stay by default
                        daysSpinner.getValueFactory().setValue(currentNights());
                    }
                }
                fireChange();
            });

            HBox row = new HBox(12, check, price, daysNode);
            row.setAlignment(Pos.CENTER_LEFT);
            extrasBox.getChildren().add(row);
            extraRows.add(new ExtraRow(extra, check, days));
        }
    }

    /** Resets every per-day spinner to the new number of nights (and caps it there). */
    private void syncDaysToNights() {
        int nights = currentNights();
        for (ExtraRow row : extraRows) {
            if (row.days() != null) {
                var factory = (SpinnerValueFactory.IntegerSpinnerValueFactory) row.days().getValueFactory();
                factory.setMax(nights);
                factory.setValue(nights);
            }
        }
    }

    private int currentNights() {
        Integer nights = nightsSpinner.getValue();
        return nights == null ? 1 : nights;
    }

    // ---------- Small layout helpers ----------

    private void fireChange() { changeListener.run(); }

    /** A label above an input, with the input stretched to full width. */
    private VBox field(String label, Region input) {
        input.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(4, new Label(label), input);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
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

    /** "Label ........ value" with the value pushed to the right edge. Bold for Total and Balance. */
    private HBox summaryRow(String name, Label value, boolean strong) {
        Label nameLabel = new Label(name);
        if (strong) {
            nameLabel.getStyleClass().add("summary-strong");
            value.getStyleClass().add("summary-strong");
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(8, nameLabel, spacer, value);
    }

    // ---------- What the controller is allowed to see and do ----------

    public Node getRoot() { return root; }
    public Button getClearButton() { return clearButton; }
    public Button getCalculateButton() { return calculateButton; }
    public Button getPayNowButton() { return payNowButton; }
    public Button getPayLaterButton() { return payLaterButton; }

    /** The controller passes the method to run whenever an input changes. */
    public void onInputChanged(Runnable listener) { this.changeListener = listener; }

    public String getGuestName() { return guestField.getText(); }
    public RoomType getRoomType() { return roomTypeBox.getValue(); }
    public int getNights() { return currentNights(); }
    public String getPaidText() { return paidField.getText(); }
    public String getNotes() { return notesField.getText(); }
    public String getOtherText() { return otherField.getText(); }

    /** Reads the ticked extras. A one-time extra always has quantity 1. */
    public List<ExtraSelection> getSelectedExtras() {
        List<ExtraSelection> selected = new ArrayList<>();
        for (ExtraRow row : extraRows) {
            if (row.check().isSelected()) {
                int quantity = row.days() == null ? 1 : row.days().getValue();
                selected.add(new ExtraSelection(row.extra(), quantity));
            }
        }
        return selected;
    }

    public void setRoomHint(String text) { roomHint.setText(text); }

    /** The small line under the amount box, e.g. the minimum deposit. */
    public void setDepositHint(String text) { depositHint.setText(text); }

    public void setSummary(String roomCharge, String discount, String extras,
                           String total, String paying, String balance) {
        roomChargeValue.setText(roomCharge);
        discountValue.setText(discount);
        extrasValue.setText(extras);
        totalValue.setText(total);
        payingValue.setText(paying);
        balanceValue.setText(balance);
    }

    /** Shows a red error or a green success line under the buttons. */
    public void showMessage(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.getStyleClass().add(error ? "message-error" : "message-success");
        messageLabel.setVisible(true);
    }

    public void clearMessage() { messageLabel.setVisible(false); }

    /** Resets the whole form: empty fields, 1 night, nothing ticked. */
    public void clear() {
        guestField.clear();
        roomTypeBox.setValue(RoomType.STANDARD);
        nightsSpinner.getValueFactory().setValue(1);
        paidField.clear();
        notesField.clear();
        otherField.clear();
        for (ExtraRow row : extraRows) {
            row.check().setSelected(false);
        }
    }
}