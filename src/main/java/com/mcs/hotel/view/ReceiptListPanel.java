package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** The "View Receipts" screen: every saved bill with a View button. VIEW ONLY. */
public class ReceiptListPanel {

    private final VBox root = new VBox(12);
    private final BillTable table = new BillTable();

    public ReceiptListPanel() {
        Label title = new Label("View Receipts");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Click View to open a guest's receipt.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox.setVgrow(table.getNode(), Priority.ALWAYS); // the table takes the leftover height

        root.setPadding(new Insets(24));
        root.getChildren().addAll(new VBox(4, title, subtitle), table.getNode());
    }

    public Node getRoot() { return root; }
    public void setBills(List<Bill> bills) { table.setBills(bills); }
    public void onViewBill(Consumer<Bill> handler) { table.onView(handler); }
}