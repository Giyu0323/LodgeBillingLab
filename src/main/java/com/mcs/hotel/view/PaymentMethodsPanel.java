package com.mcs.hotel.view;

import com.mcs.hotel.model.PaymentMethod;
import com.mcs.hotel.util.images;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Payment Method Dashboard: one card per payment method, each with its
 * tile, an Active or Disabled badge, a description and a Manage row.
 * VIEW ONLY: it shows what it is given and tells the controller when Manage is clicked.
 */
public class PaymentMethodsPanel {

    /** One card's data: the method and whether it is switched on. */
    public record Item(PaymentMethod method, boolean active) {}

    private final ScrollPane root = new ScrollPane();
    private final GridPane grid = new GridPane();
    private Consumer<PaymentMethod> manageHandler = method -> {}; // set by the controller

    public PaymentMethodsPanel() {
        Label title = new Label("Payment Method Dashboard");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage and view available payment methods for your hotel.");
        subtitle.getStyleClass().add("page-subtitle");

        HBox header = new HBox(16, images.view("nav-payments.png", 36), new VBox(2, title, subtitle));
        header.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region(); // thin line under the header
        divider.getStyleClass().add("card-divider");

        // Two equal columns of cards
        grid.setHgap(18);
        grid.setVgap(18);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            grid.getColumnConstraints().add(half);
        }

        VBox outer = new VBox(18, header, divider, grid);
        outer.getStyleClass().add("card");

        VBox page = new VBox(outer);
        page.setPadding(new Insets(24));

        root.setContent(page);
        root.setFitToWidth(true); // content stretches to the window width
    }

    /** One method's card. A disabled method is dimmed. */
    private VBox buildCard(PaymentMethod method, boolean active) {
        Label name = new Label(method.toString());
        name.getStyleClass().add("method-name");

        Label status = new Label(active ? "●  Active" : "●  Disabled");
        status.getStyleClass().addAll("status-badge", active ? "status-active" : "status-off");
        HBox badgeRow = new HBox(status); // keeps the badge as small as its text

        Label description = new Label(method.description());
        description.setWrapText(true);
        description.getStyleClass().add("method-desc");

        VBox texts = new VBox(8, name, badgeRow, description);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox top = new HBox(18, images.view(method.iconFile(), 72), texts);
        top.setAlignment(Pos.TOP_LEFT);

        Region line = new Region();
        line.getStyleClass().add("card-divider");

        // The whole row is clickable, like in the design: gear, "Manage", chevron
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ImageView gear = images.view("nav-prices.png", 18);
        ImageView chevron = images.view("chevron-right.png", 14);
        HBox manage = new HBox(10, gear, new Label("Manage"), spacer, chevron);
        manage.setAlignment(Pos.CENTER_LEFT);
        manage.getStyleClass().add("manage-row");
        manage.setOnMouseClicked(e -> manageHandler.accept(method));

        VBox card = new VBox(14, top, line, manage);
        card.getStyleClass().add("method-card");
        if (!active) {
            card.getStyleClass().add("method-off");
        }
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // ---- What the controller is allowed to do ----

    public Node getRoot() { return root; }

    /** The controller passes the method to run when a card's Manage row is clicked. */
    public void onManage(Consumer<PaymentMethod> handler) { this.manageHandler = handler; }

    /** Rebuilds all the cards. */
    public void setMethods(List<Item> items) {
        grid.getChildren().clear();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            grid.add(buildCard(item.method(), item.active()), i % 2, i / 2);
        }
    }
}