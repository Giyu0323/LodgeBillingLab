package com.mcs.hotel.view;

import com.mcs.hotel.model.PaymentDetails;
import com.mcs.hotel.model.PaymentMethod;
import com.mcs.hotel.model.PaymentRequest;
import com.mcs.hotel.model.PaymentStatus;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.Theme;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * The payment window, in two steps.
 * Step 1: choose a method and enter the guest's phone, NRC and email (a card
 * also needs the bank account number). Staff NEVER enter a PIN.
 * Step 2: a request has been sent to the guest's phone. The window waits and
 * shows a countdown until the guest approves, declines, or time runs out.
 *
 * VIEW ONLY: it knows no payment rules. Everything is done through the Flow
 * that the controller supplies.
 */
public final class PaymentDialog {

    /** What this window needs from the controller. */
    public interface Flow {
        /** Checks the details and sends the request. Throws a friendly message if something is wrong. */
        PaymentRequest send(PaymentMethod method, PaymentDetails details);

        /** Where the request is now (pending, approved, declined, expired...). */
        PaymentStatus check(PaymentRequest request);

        /** DEMO ONLY: stands in for the guest tapping approve or decline on their phone. */
        void guestResponds(PaymentRequest request, boolean approve);

        /** Staff cancelled the request. */
        void cancel(PaymentRequest request);

        /** The guest approved: finish the job (save the bill). Returns an error message, or null on success. */
        String finish(PaymentRequest request);
    }

    private PaymentDialog() {} // utility class: no instances

    /**
     * Opens the window and waits until it is closed.
     *
     * @param methods    the methods to offer (the ones an admin has left switched on)
     * @param amountText the amount to pay, already formatted, e.g. "K 1,000.00"
     * @return true if the guest approved and the payment was completed, false otherwise
     */
    public static boolean show(List<PaymentMethod> methods, String amountText, Flow flow) {
        return new Window(methods, amountText, flow).showAndWait();
    }

    // ------------------------------------------------------------------
    // The window itself
    // ------------------------------------------------------------------
    private static final class Window {

        private final Stage stage = new Stage();
        private final Flow flow;
        private final String amountText;

        // Step 1: choosing and typing
        private final ToggleGroup group = new ToggleGroup();
        private final TextField phone = new TextField();
        private final TextField nrc = new TextField();
        private final TextField email = new TextField();
        private final TextField account = new TextField();
        private final VBox accountBox;
        private final VBox fields;
        private final Label note = new Label();
        private final Label error = new Label();
        private final Button sendButton = new Button("Send payment request");
        private final VBox detailsView = new VBox(14);

        // Step 2: waiting for the guest
        private final VBox waitingView = new VBox(16);
        private final Label waitingText = new Label();
        private final Label countdown = new Label();

        private final Timeline timer = new Timeline(); // checks the request twice a second
        private PaymentRequest current;                // the request being waited on
        private boolean paid;

        Window(List<PaymentMethod> methods, String amountText, Flow flow) {
            this.flow = flow;
            this.amountText = amountText;

            Label amount = new Label("Amount to pay: " + amountText);
            amount.getStyleClass().add("card-title");

            // ---- The method tiles, two per row ----
            GridPane tiles = new GridPane();
            tiles.setHgap(12);
            tiles.setVgap(12);
            for (int i = 0; i < 2; i++) {
                ColumnConstraints half = new ColumnConstraints();
                half.setPercentWidth(50);
                tiles.getColumnConstraints().add(half);
            }
            for (int i = 0; i < methods.size(); i++) {
                PaymentMethod offered = methods.get(i);
                ToggleButton tile = new ToggleButton(offered.toString(),
                        images.view(offered.iconFile(), 40));
                tile.setUserData(offered);
                tile.setToggleGroup(group);
                tile.setMaxWidth(Double.MAX_VALUE);
                tile.getStyleClass().add("method-tile");
                GridPane.setHgrow(tile, Priority.ALWAYS);
                tiles.add(tile, i % 2, i / 2);
            }

            // ---- The guest's details ----
            phone.setPromptText("e.g. 0977 123 456");
            restrict(phone, "[0-9+ ]*", 14);
            nrc.setPromptText("e.g. 123456/78/1");
            restrict(nrc, "[0-9/]*", 11);
            email.setPromptText("e.g. guest@example.com");
            restrict(email, ".*", 100);
            account.setPromptText("Bank account number");
            restrict(account, "[0-9]*", PaymentMethod.ACCOUNT_MAX_DIGITS);

            accountBox = new VBox(labelled("Bank account number", account));
            fields = new VBox(10,
                    labelled("Guest's phone number", phone),
                    labelled("Guest's NRC number", nrc),
                    labelled("Guest's email", email),
                    accountBox);
            fields.setVisible(false); // hidden until a method is chosen
            fields.managedProperty().bind(fields.visibleProperty());

            note.setWrapText(true);
            note.setMinHeight(Region.USE_PREF_SIZE);
            note.getStyleClass().add("page-subtitle");
            note.setVisible(false);
            note.managedProperty().bind(note.visibleProperty());

            error.setWrapText(true);
            error.setMinHeight(Region.USE_PREF_SIZE);
            error.getStyleClass().add("message-error");
            error.setVisible(false);
            error.managedProperty().bind(error.visibleProperty());

            // Choosing a method shows that method's boxes
            group.selectedToggleProperty().addListener((obs, was, now) -> {
                if (now == null) {                  // clicking the chosen tile again must not un-choose it
                    if (was != null) group.selectToggle(was);
                    return;
                }
                PaymentMethod chosen = (PaymentMethod) now.getUserData();
                setShown(accountBox, chosen.isBank());
                note.setText(chosen.confirmationNote());
                note.setVisible(true);
                fields.setVisible(true);
                error.setVisible(false);
                sendButton.setDisable(false);
                if (stage.isShowing()) stage.sizeToScene(); // grow to fit what just appeared
            });

            sendButton.getStyleClass().add("primary-button");
            sendButton.setDisable(true); // nothing to send until a method is chosen
            sendButton.setOnAction(e -> send());
            Button cancel = new Button("Cancel");
            cancel.getStyleClass().add("secondary-button");
            cancel.setOnAction(e -> stage.close());
            HBox buttons = new HBox(10, cancel, sendButton);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            detailsView.getChildren().addAll(new Label("Choose a payment method"), tiles,
                    fields, note, error, buttons);

            // ---- Step 2: the waiting view ----
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(48, 48);
            spinner.setMinSize(48, 48);

            waitingText.setWrapText(true);
            waitingText.setMaxWidth(380);
            waitingText.setMinHeight(Region.USE_PREF_SIZE);
            countdown.getStyleClass().add("countdown");
            HBox waitingRow = new HBox(16, spinner, new VBox(6, waitingText, countdown));
            waitingRow.setAlignment(Pos.CENTER_LEFT);

            // A real gateway replaces this box: the provider reports the guest's answer
            Label demoTitle = new Label("Demo only: stands in for the guest's phone");
            demoTitle.getStyleClass().add("demo-title");
            Button approve = new Button("Guest approves");
            approve.getStyleClass().add("primary-button");
            approve.setOnAction(e -> flow.guestResponds(current, true));
            Button decline = new Button("Guest declines");
            decline.getStyleClass().add("secondary-button");
            decline.setOnAction(e -> flow.guestResponds(current, false));
            VBox demo = new VBox(8, demoTitle, new HBox(10, approve, decline));
            demo.getStyleClass().add("demo-box");

            Button cancelRequest = new Button("Cancel request");
            cancelRequest.getStyleClass().add("secondary-button");
            cancelRequest.setOnAction(e -> cancelRequest());

            waitingView.getChildren().addAll(waitingRow, demo, cancelRequest);

            // ---- Put it all in the window ----
            VBox root = new VBox(16, amount, detailsView, waitingView);
            root.setPadding(new Insets(24));
            root.setPrefWidth(520);
            showDetails();

            timer.getKeyFrames().add(new KeyFrame(Duration.millis(500), e -> tick()));
            timer.setCycleCount(Timeline.INDEFINITE);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(Theme.STYLESHEET); // the window is separate, so style it too
            stage.setScene(scene);
            stage.setTitle("Payment");
            stage.initModality(Modality.APPLICATION_MODAL); // the main window waits until this closes
            stage.setResizable(false);
            stage.setOnHidden(e -> {                         // closed with X, Cancel or after paying
                timer.stop();
                if (current != null && !paid) {
                    flow.cancel(current);                    // never leave a request waiting
                }
            });
        }

        boolean showAndWait() {
            stage.showAndWait();
            return paid;
        }

        // ---------- What the buttons do ----------

        /** Send payment request: check the details, send, and switch to the waiting view. */
        private void send() {
            Toggle selected = group.getSelectedToggle();
            if (selected == null) {
                showError("Choose a payment method.");
                return;
            }
            PaymentMethod method = (PaymentMethod) selected.getUserData();
            PaymentDetails typed = new PaymentDetails(
                    phone.getText(), nrc.getText(), email.getText(), account.getText());
            try {
                current = flow.send(method, typed);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showError(ex.getMessage());
                return;
            }
            error.setVisible(false);
            waitingText.setText("Request for " + amountText + " sent to " + current.maskedPhone()
                    + " (" + method + "). Ask the guest to check their phone and approve it.");
            countdown.setText("Expires in " + current.secondsLeft() + " seconds");
            showWaiting();
            timer.playFromStart();
        }

        /** Runs twice a second while waiting: has the guest answered yet? */
        private void tick() {
            PaymentStatus status = flow.check(current);
            if (status == PaymentStatus.PENDING) {
                countdown.setText("Expires in " + current.secondsLeft() + " seconds");
                return;
            }
            timer.stop();
            switch (status) {
                case APPROVED -> {
                    String problem = flow.finish(current);
                    if (problem == null) {
                        paid = true;
                        stage.close();
                    } else {
                        backToDetails(problem);
                    }
                }
                case DECLINED -> backToDetails("The guest declined the payment. You can send it again.");
                case EXPIRED -> backToDetails("The guest did not confirm in time. You can send it again.");
                default -> backToDetails("The request was cancelled.");
            }
        }

        /** Cancel request: stop waiting and go back to the details. */
        private void cancelRequest() {
            timer.stop();
            flow.cancel(current);
            backToDetails("Request cancelled. Nothing was charged.");
        }

        // ---------- Switching between the two steps ----------

        private void backToDetails(String message) {
            showDetails();
            showError(message);
        }

        private void showError(String message) {
            error.setText(message);
            error.setVisible(true);
            if (stage.isShowing()) stage.sizeToScene();
        }

        private void showDetails() {
            setShown(detailsView, true);
            setShown(waitingView, false);
            if (stage.isShowing()) stage.sizeToScene();
        }

        private void showWaiting() {
            setShown(detailsView, false);
            setShown(waitingView, true);
            stage.sizeToScene();
        }
    }

    // ---------- Small helpers ----------

    /** A label above an input, with the input stretched to full width. */
    private static VBox labelled(String label, Node input) {
        if (input instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return new VBox(4, new Label(label), input);
    }

    /** Shows or hides a block, and removes the gap it would leave. */
    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /** Stops wrong characters and over-long text from being typed at all. */
    private static void restrict(TextField field, String allowedCharacters, int maxLength) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String text = change.getControlNewText();
            return text.matches(allowedCharacters) && text.length() <= maxLength ? change : null;
        }));
    }
}