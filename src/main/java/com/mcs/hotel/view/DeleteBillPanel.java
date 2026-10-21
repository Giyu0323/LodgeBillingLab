package com.mcs.hotel.view;

import com.mcs.hotel.model.Bill;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** The "Delete Bill" screen: every saved bill with View and Delete buttons. VIEW ONLY. */
public class DeleteBillPanel {

    private final VBox root = new VBox(12);
    private final BillTable table = new BillTable();

    public DeleteBillPanel() {
        Label title = new Label("Delete Bill");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Click Delete on a bill. Its room becomes available again.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox.setVgrow(table.getNode(), Priority.ALWAYS); // the table takes the leftover height

        root.setPadding(new Insets(24));
        root.getChildren().addAll(new VBox(4, title, subtitle), table.getNode());
    }

    public Node getRoot() { return root; }
    public void setBills(List<Bill> bills) { table.setBills(bills); }
    public void onViewBill(Consumer<Bill> handler) { table.onView(handler); }
    public void onDeleteBill(Consumer<Bill> handler) { table.onDelete(handler); }
}