package com.mcs.hotel.view;

import com.mcs.hotel.util.images;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;

import java.util.List;

/**
 * One guest's receipt: logo, details in two columns, itemised lines, totals,
 * and Print / Back buttons. It is dark on screen to match the app, and turns
 * white only while it is being printed.
 * VIEW ONLY: it shows ready-made text. The controller works out what each line says.
 */
public class ReceiptPanel {

    /** One row of the itemised table, already formatted as text. */
    public record Line(String item, String days, String price, String total) {}

    private final ScrollPane root = new ScrollPane();
    private final VBox card = new VBox(12);          // the part that gets printed
    private final VBox infoBox = new VBox(8);        // bill number, date, guest...
    private final GridPane linesGrid = new GridPane();
    private final VBox totalsBox = new VBox(6);
    private final Button printButton = new Button("Print", images.view("printer.png", 16));
    private final Button backButton = new Button("Back to Bills", images.viewDark("arrow-left.png", 16));

    public ReceiptPanel() {
        Label name = new Label("MCS Hotel");
        name.getStyleClass().add("brand-title");
        Label tagline = new Label("STAY · RELAX · ENJOY");
        tagline.getStyleClass().add("brand-tagline");
        VBox heading = new VBox(4, images.view("logo.png", 80), name, tagline);
        heading.setAlignment(Pos.CENTER);

        // Column 0 (item name) takes the spare width; the three number columns are
        // fixed and right-aligned so the figures line up.
        ColumnConstraints itemCol = new ColumnConstraints();
        itemCol.setHgrow(Priority.ALWAYS);
        linesGrid.getColumnConstraints().add(itemCol);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints numberCol = new ColumnConstraints(90);
            numberCol.setHalignment(HPos.RIGHT);
            linesGrid.getColumnConstraints().add(numberCol);
        }
        linesGrid.setHgap(8);
        linesGrid.setVgap(6);

        card.getChildren().addAll(heading, new Separator(), infoBox,
                new Separator(), linesGrid, new Separator(), totalsBox);
        card.setPadding(new Insets(24));
        card.getStyleClass().add("receipt-card"); // dark on screen; "receipt-print" makes it white (style.css)

        printButton.getStyleClass().add("secondary-button");
        backButton.getStyleClass().add("primary-button");
        HBox buttons = new HBox(12, printButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        // The buttons sit OUTSIDE the card, so they don't appear on the printout
        VBox page = new VBox(16, card, buttons);
        page.setMaxWidth(600);

        StackPane holder = new StackPane(page);
        holder.setAlignment(Pos.TOP_CENTER); // receipt centred horizontally
        holder.setPadding(new Insets(24));

        root.setContent(holder);
        root.setFitToWidth(true);
    }

    // ---- What the controller is allowed to do ----

    public Node getRoot() { return root; }
    public Button getPrintButton() { return printButton; }
    public Button getBackButton() { return backButton; }

    /**
     * Fills the details block in two columns: bill number and date on the left,
     * guest, room, room type and nights on the right. "notes" is skipped when empty.
     */
    public void setHeader(String billNumber, String date, String guest,
                          String room, String roomType, String nights, String notes) {
        VBox left = new VBox(6,
                pair("Bill Number:", billNumber),
                pair("Date:", date));
        VBox right = new VBox(6,
                pair("Guest:", guest),
                pair("Room:", room),
                pair("Room Type:", roomType),
                pair("Nights:", nights));

        HBox columns = new HBox(24, left, right);
        HBox.setHgrow(left, Priority.ALWAYS); // pushes the right column to the right edge

        infoBox.getChildren().setAll(columns);
        if (notes != null && !notes.isBlank()) {
            infoBox.getChildren().add(pair("Notes:", notes));
        }
    }

    /** Rebuilds the itemised table: a header row, a divider, then one row per line. */
    public void setLines(List<Line> lines) {
        linesGrid.getChildren().clear();
        addGridRow(0, "Item", "Days", "Price", "Total", true);
        linesGrid.add(new Separator(), 0, 1, 4, 1); // divider under the header, across all 4 columns
        int row = 2;
        for (Line line : lines) {
            addGridRow(row++, line.item(), line.days(), line.price(), line.total(), false);
        }
    }

    public void setTotals(String total, String paid, String balance) {
        totalsBox.getChildren().setAll(
                infoRow("Total", total, true),
                infoRow("Amount Paid", paid, false),
                infoRow("Balance Due", balance, true));
    }

    /**
     * Sends the receipt card to a printer (the user picks it in the system dialog).
     * The card is switched to white paper while printing and back to dark afterwards.
     * Returns false only when this computer has no printer at all.
     */
    public boolean print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            return false;
        }
        if (!job.showPrintDialog(card.getScene().getWindow())) {
            return true; // the user cancelled the dialog: not an error
        }

        card.getStyleClass().add("receipt-print"); // white background, dark text (style.css)
        card.applyCss();                           // apply the new colours before printing
        Scale scale = null;
        try {
            // Shrink the card if it is bigger than the printable area of the page
            PageLayout layout = job.getJobSettings().getPageLayout();
            double factor = Math.min(1.0, Math.min(
                    layout.getPrintableWidth() / card.getLayoutBounds().getWidth(),
                    layout.getPrintableHeight() / card.getLayoutBounds().getHeight()));
            scale = new Scale(factor, factor);
            card.getTransforms().add(scale);
            boolean printed = job.printPage(card);
            if (printed) {
                job.endJob();
            } else {
                job.cancelJob();
            }
        } finally {
            // Always put the on-screen receipt back, even if printing failed
            if (scale != null) {
                card.getTransforms().remove(scale);
            }
            card.getStyleClass().remove("receipt-print");
            card.applyCss();
        }
        return true;
    }

    // ---- Small layout helpers ----

    /** "Label:  value" side by side, the label in a softer colour. */
    private HBox pair(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("receipt-label");
        return new HBox(8, name, new Label(value));
    }

    /** "Label ........ value" with the value pushed to the right edge. Bold when "strong". */
    private HBox infoRow(String name, String value, boolean strong) {
        Label nameLabel = new Label(name);
        Label valueLabel = new Label(value);
        if (strong) {
            nameLabel.getStyleClass().add("summary-strong");
            valueLabel.getStyleClass().add("summary-strong");
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(8, nameLabel, spacer, valueLabel);
    }

    private void addGridRow(int row, String item, String days, String price, String total,
                            boolean header) {
        String[] cells = {item, days, price, total};
        for (int col = 0; col < cells.length; col++) {
            Label label = new Label(cells[col]);
            if (header) {
                label.getStyleClass().add("receipt-head"); // bold, from style.css
            }
            linesGrid.add(label, col, row);
        }
    }
}