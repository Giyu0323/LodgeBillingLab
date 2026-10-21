package com.mcs.hotel.view;

import com.mcs.hotel.model.AnalyticsPeriod;
import com.mcs.hotel.util.images;
import com.mcs.hotel.util.MoneyFormat;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Business Analytics page: period dropdown, four summary cards, three
 * charts, key performance indicators and a monthly table.
 * VIEW ONLY: it shows what it is given. It never works out a business figure;
 * the controller does that and passes the results in. (Turning a number into
 * "K 1,250.00" or a chart tooltip is only presentation, so it lives here.)
 */
public class AnalyticsPanel {

    /** How a change should be coloured: green for good news, red for bad, grey for neither. */
    public enum Tone { GOOD, BAD, NEUTRAL }

    /** The four summary cards across the top. */
    public enum Card { REVENUE, EXPENSES, PROFIT, MARGIN }

    /** The four key performance indicators. */
    public enum Kpi { ROI, OCCUPANCY, ADR, REVPAR }

    /** One row of the monthly table, already formatted as text. "loss" turns the profit red. */
    public record MonthRow(String month, String revenue, String expenses, String profit,
                           String margin, boolean loss) {}

    private final ScrollPane root = new ScrollPane();

    private final ComboBox<AnalyticsPeriod> periodBox = new ComboBox<>();
    private final Label rangeLabel = new Label();
    private final Button exportButton = new Button("Export Report", images.viewDark("an-download.png", 16));
    private final Button capitalButton = new Button("Set capital");

    private final Map<Card, Label> cardValues = new EnumMap<>(Card.class);
    private final Map<Card, Label> cardChanges = new EnumMap<>(Card.class);
    private final Map<Kpi, Label> kpiValues = new EnumMap<>(Kpi.class);
    private final Map<Kpi, Label> kpiNotes = new EnumMap<>(Kpi.class);

    // The boxes the three charts are placed in (rebuilt each time the period changes)
    private final StackPane lineBox = chartBox();
    private final StackPane barBox = chartBox();
    private final StackPane doughnutBox = chartBox();

    private final TableView<MonthRow> table = new TableView<>();

    private Consumer<AnalyticsPeriod> periodHandler = period -> {}; // set by the controller

    public AnalyticsPanel() {
        VBox page = new VBox(16, buildHeader(), buildSummaryCards(), buildMainGrid(), buildFootnote());
        page.setPadding(new Insets(24));

        root.setContent(page);
        root.setFitToWidth(true); // content stretches to the window width
    }

    // ---------- Building the page ----------

    /** Icon, title and dates on the left; the period dropdown and Export button on the right. */
    private HBox buildHeader() {
        StackPane iconBox = new StackPane(images.view("nav-analytics.png", 30));
        iconBox.getStyleClass().add("analytics-icon-box");

        Label title = new Label("Business Analytics");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Revenue, profitability, occupancy and investment performance");
        subtitle.getStyleClass().add("page-subtitle");
        rangeLabel.getStyleClass().add("stat-note");

        periodBox.getItems().addAll(AnalyticsPeriod.values());
        periodBox.setValue(AnalyticsPeriod.THIS_MONTH);
        periodBox.setPrefWidth(200);
        periodBox.getStyleClass().add("combo-icon"); // text clears the calendar icon (style.css)
        periodBox.valueProperty().addListener((obs, was, now) -> {
            if (now != null) {
                periodHandler.accept(now); // the controller redraws everything for the new period
            }
        });
        StackPane periodStack = new StackPane(periodBox, IconInput.overlay("calendar.png"));

        exportButton.getStyleClass().add("primary-button");
        exportButton.setDisable(true); // switched on in step 5
        exportButton.setTooltip(new Tooltip("Export is added in a later step."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, iconBox, new VBox(2, title, subtitle, rangeLabel),
                spacer, periodStack, exportButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /** The four summary cards in a row of equal width. */
    private GridPane buildSummaryCards() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            grid.getColumnConstraints().add(column);
        }
        grid.add(summaryCard(Card.REVENUE, "Total Revenue", "an-trend.png", "tile-green"), 0, 0);
        grid.add(summaryCard(Card.EXPENSES, "Operating Expenses", "coin.png", "tile-orange"), 1, 0);
        grid.add(summaryCard(Card.PROFIT, "Net Profit", "an-wallet.png", "tile-green"), 2, 0);
        grid.add(summaryCard(Card.MARGIN, "Profit Margin", "an-percent.png", "tile-blue"), 3, 0);
        return grid;
    }

    /** One summary card: a coloured icon, the title, a big value, and the change line under it. */
    private VBox summaryCard(Card card, String title, String iconFile, String tileClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        Label value = new Label("-");
        value.getStyleClass().add("stat-value");
        Label change = new Label();
        change.getStyleClass().add("change-none");
        change.setWrapText(true);

        cardValues.put(card, value);
        cardChanges.put(card, change);

        StackPane tile = new StackPane(images.view(iconFile, 26));
        tile.getStyleClass().addAll("icon-tile", tileClass);

        HBox top = new HBox(14, tile, new VBox(2, titleLabel, value));
        top.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, top, change);
        box.getStyleClass().add("card");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /**
     * Charts, KPIs, breakdown and table in a three-column grid that shrinks and
     * grows with the window. Row 1: two charts and the KPIs. Row 2: the
     * breakdown and the table.
     */
    private GridPane buildMainGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        double[] widths = {36, 36, 28};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            grid.getColumnConstraints().add(column);
        }

        grid.add(card("Revenue vs Expenses", lineBox), 0, 0);
        grid.add(card("Monthly Net Profit", barBox), 1, 0);
        grid.add(buildKpiCard(), 2, 0);
        grid.add(card("Revenue Breakdown", doughnutBox), 0, 1);
        grid.add(buildTableCard(), 1, 1, 2, 1); // the table spans the last two columns
        return grid;
    }

    /** The Key Performance Indicators card: four rows, each with a round icon, a value and a note. */
    private VBox buildKpiCard() {
        VBox rows = new VBox(10,
                kpiRow(Kpi.ROI, "ROI", "refresh.png", "tile-green"),
                kpiRow(Kpi.OCCUPANCY, "Occupancy Rate", "bed.png", "tile-blue"),
                kpiRow(Kpi.ADR, "Average Daily Rate", "coin.png", "tile-orange"),
                kpiRow(Kpi.REVPAR, "RevPAR", "nav-analytics.png", "tile-purple"));
        return card("Key Performance Indicators", rows);
    }

    private HBox kpiRow(Kpi kpi, String title, String iconFile, String tileClass) {
        StackPane circle = new StackPane(images.view(iconFile, 22));
        circle.getStyleClass().addAll("kpi-circle", tileClass);

        Label name = new Label(title);
        name.getStyleClass().add("stat-title");
        Label value = new Label("-");
        value.getStyleClass().add("kpi-value");
        Label note = new Label();
        note.getStyleClass().add("stat-note");
        note.setWrapText(true);

        kpiValues.put(kpi, value);
        kpiNotes.put(kpi, note);

        VBox texts = new VBox(1, name, value, note);
        HBox.setHgrow(texts, Priority.ALWAYS);

        HBox row = new HBox(12, circle, texts);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kpi-row");

        if (kpi == Kpi.ROI) {
            // ROI needs the invested capital, so its row has the button that sets it
            capitalButton.getStyleClass().add("small-outline");
            row.getChildren().add(capitalButton);
        }
        return row;
    }

    /** The monthly financial table inside a card. */
    private VBox buildTableCard() {
        table.getColumns().add(textColumn("Month", MonthRow::month));
        table.getColumns().add(textColumn("Revenue", MonthRow::revenue));
        table.getColumns().add(textColumn("Expenses", MonthRow::expenses));
        table.getColumns().add(profitColumn());
        table.getColumns().add(textColumn("Profit Margin", MonthRow::margin));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No figures for this period."));
        table.setMinHeight(220);
        VBox.setVgrow(table, Priority.ALWAYS);
        return card("Monthly Financial Summary", table);
    }

    private HBox buildFootnote() {
        Label note = new Label("Figures are estimates and depend on the expenses and invested capital entered. "
                + "A figure shows Unavailable when there is not enough information to work it out.");
        note.getStyleClass().add("stat-note");
        note.setWrapText(true);
        HBox row = new HBox(8, images.view("an-info.png", 16), note);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------- Small layout helpers ----------

    /** A titled card. Its look comes from the "card" class in style.css. */
    private VBox card(String title, Node... content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        VBox card = new VBox(12, heading);
        card.getChildren().addAll(content);
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    /** An empty box a chart (or a message) is placed in. It takes the card's spare height. */
    private StackPane chartBox() {
        StackPane box = new StackPane();
        box.setMinHeight(230);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private TableColumn<MonthRow, String> textColumn(String title, Function<MonthRow, String> text) {
        TableColumn<MonthRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return col;
    }

    /** The Net Profit column: a month that lost money shows in red. */
    private TableColumn<MonthRow, String> profitColumn() {
        TableColumn<MonthRow, String> col = textColumn("Net Profit", MonthRow::profit);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                getStyleClass().remove("loss-cell"); // JavaFX reuses cells, so always reset
                if (empty || text == null) {
                    setText(null);
                    return;
                }
                setText(text);
                MonthRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null && row.loss()) {
                    getStyleClass().add("loss-cell");
                }
            }
        });
        return col;
    }

    /** Smaller, greyer text for a figure that is unavailable. */
    private void markUnavailable(Label label, boolean unavailable) {
        label.getStyleClass().remove("value-unavailable");
        if (unavailable) {
            label.getStyleClass().add("value-unavailable");
        }
    }

    // ---------- The charts ----------

    /**
     * Rebuilds the three charts. The lists hold one entry per month, oldest first.
     * A chart with nothing to show is replaced by a short message, never by an
     * empty or invented chart.
     */
    public void setCharts(List<String> months, List<Double> revenue, List<Double> expenses,
                          List<Double> profit, double roomRevenue, double extrasRevenue) {
        boolean hasMoney = revenue.stream().anyMatch(v -> v != 0)
                || expenses.stream().anyMatch(v -> v != 0);
        Node line = hasMoney
                ? lineChart(months, revenue, expenses)
                : note("No revenue or expenses in this period.");
        lineBox.getChildren().setAll(line);

        boolean hasProfit = profit.stream().anyMatch(v -> v != 0);
        Node bars = hasProfit
                ? barChart(months, profit)
                : note("No profit or loss in this period.");
        barBox.getChildren().setAll(bars);

        Node doughnut = roomRevenue + extrasRevenue > 0
                ? doughnut(roomRevenue, extrasRevenue)
                : note("No revenue in this period.");
        doughnutBox.getChildren().setAll(doughnut);
    }

    /** Revenue (orange) and Expenses (blue) as two lines, one point per month. */
    private Node lineChart(List<String> months, List<Double> revenue, List<Double> expenses) {
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), moneyAxis());
        chart.getStyleClass().add("line-money"); // series colours come from style.css
        chart.setAnimated(false);
        chart.setLegendSide(Side.TOP);
        chart.setMinHeight(210);

        XYChart.Series<String, Number> revenueSeries = series("Revenue", months, revenue);
        XYChart.Series<String, Number> expenseSeries = series("Expenses", months, expenses);
        chart.getData().addAll(revenueSeries, expenseSeries);

        addTooltips(revenueSeries, false);
        addTooltips(expenseSeries, false);
        return chart;
    }

    /** Net profit per month as orange bars. A month that lost money gets a red bar. */
    private Node barChart(List<String> months, List<Double> profit) {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), moneyAxis());
        chart.getStyleClass().add("profit-bars");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(24);
        chart.setBarGap(0);
        chart.setMinHeight(210);

        XYChart.Series<String, Number> series = series("Net Profit", months, profit);
        chart.getData().add(series);
        addTooltips(series, true);
        return chart;
    }

    /** Room revenue vs Extras as a doughnut, with the total in the middle and a legend beside it. */
    private Node doughnut(double room, double extras) {
        double total = room + extras;

        PieChart pie = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Room Revenue", room),
                new PieChart.Data("Extras", extras)));
        pie.getStyleClass().add("doughnut"); // slice colours come from style.css
        pie.setAnimated(false);
        pie.setLabelsVisible(false);
        pie.setLegendVisible(false);
        pie.setStartAngle(90);

        for (PieChart.Data slice : pie.getData()) {
            String text = slice.getName() + ": " + money(slice.getPieValue());
            if (slice.getNode() != null) {
                Tooltip.install(slice.getNode(), new Tooltip(text));
            } else {
                slice.nodeProperty().addListener((obs, was, node) -> {
                    if (node != null) Tooltip.install(node, new Tooltip(text));
                });
            }
        }

        // JavaFX has no doughnut chart: a circle in the card's colour covers the middle of the pie
        Circle hole = new Circle();
        hole.getStyleClass().add("doughnut-hole");
        hole.radiusProperty().bind(Bindings.min(pie.widthProperty(), pie.heightProperty()).multiply(0.27));
        hole.setMouseTransparent(true);

        Label amount = new Label(money(total));
        amount.getStyleClass().add("kpi-value");
        Label caption = new Label("Total Revenue");
        caption.getStyleClass().add("stat-note");
        VBox center = new VBox(2, amount, caption);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);

        StackPane chartStack = new StackPane(pie, hole, center);
        chartStack.setMinSize(190, 190);
        HBox.setHgrow(chartStack, Priority.ALWAYS);

        VBox legend = new VBox(12,
                legendRow("Room Revenue", "dot-orange", room * 100 / total, room),
                legendRow("Extras", "dot-blue", extras * 100 / total, extras));
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setMinWidth(150);

        HBox row = new HBox(12, chartStack, legend);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    /** One legend line: a coloured dot, the name, and the share and amount on the right. */
    private HBox legendRow(String name, String dotClass, double sharePercent, double amount) {
        Circle dot = new Circle(6);
        dot.getStyleClass().add(dotClass);

        Label percent = new Label(String.format(Locale.US, "%.1f%%", sharePercent));
        percent.getStyleClass().add("kpi-value-small");
        Label money = new Label(money(amount));
        money.getStyleClass().add("stat-note");
        VBox figures = new VBox(1, percent, money);
        figures.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, dot, new Label(name), spacer, figures);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** A vertical axis whose labels read "K12k" instead of a long number. */
    private NumberAxis moneyAxis() {
        NumberAxis axis = new NumberAxis();
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number number) {
                double value = number.doubleValue();
                String sign = value < 0 ? "-" : "";
                double size = Math.abs(value);
                if (size >= 1000) {
                    double thousands = size / 1000;
                    String text = thousands == Math.floor(thousands)
                            ? String.valueOf((long) thousands)
                            : String.format(Locale.US, "%.1f", thousands);
                    return sign + "K" + text + "k";
                }
                return sign + "K" + (long) size;
            }

            @Override
            public Number fromString(String text) {
                return 0; // never used: the axis only displays labels
            }
        });
        return axis;
    }

    /** One line or one set of bars: a point for each month. */
    private XYChart.Series<String, Number> series(String name, List<String> months, List<Double> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        for (int i = 0; i < months.size(); i++) {
            series.getData().add(new XYChart.Data<>(months.get(i), values.get(i)));
        }
        return series;
    }

    /** Hover text on every point or bar; with lossRed, a negative bar is painted red. */
    private void addTooltips(XYChart.Series<String, Number> series, boolean lossRed) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            double value = data.getYValue().doubleValue();
            String text = data.getXValue() + "\n" + series.getName() + ": " + money(value);
            whenNodeExists(data, node -> {
                Tooltip.install(node, new Tooltip(text));
                if (lossRed && value < 0) {
                    node.setStyle("-fx-bar-fill: #ef4444;");
                }
            });
        }
    }

    /** Runs the action once the chart has created this point's node (now, or as soon as it does). */
    private void whenNodeExists(XYChart.Data<String, Number> data, Consumer<Node> action) {
        if (data.getNode() != null) {
            action.accept(data.getNode());
            return;
        }
        data.nodeProperty().addListener((obs, was, node) -> {
            if (node != null) {
                action.accept(node);
            }
        });
    }

    /** A short grey message shown where a chart cannot be drawn. */
    private Node note(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("stat-note");
        label.setWrapText(true);
        return label;
    }

    /** "K 1,250.00", or "-K 237.50" when the amount is negative. */
    private String money(double amount) {
        return amount < 0 ? "-" + MoneyFormat.k(-amount) : MoneyFormat.k(amount);
    }

    // ---------- What the controller is allowed to see and do ----------

    public Node getRoot() { return root; }
    public Button getExportButton() { return exportButton; }

    public AnalyticsPeriod getPeriod() { return periodBox.getValue(); }

    /** The controller passes the method to run when a different period is chosen. */
    public void onPeriodChanged(Consumer<AnalyticsPeriod> handler) { this.periodHandler = handler; }

    /** The controller passes the method to run when "Set capital" is clicked. */
    public void onSetCapital(Runnable handler) { capitalButton.setOnAction(e -> handler.run()); }

    /** The dates being shown, e.g. "Showing 2026-10-01 to 2026-10-19". */
    public void setRangeText(String text) { rangeLabel.setText(text); }

    /** Fills one summary card. "available" is false when the figure cannot be worked out. */
    public void setCard(Card card, String value, boolean available, String changeText, Tone tone) {
        Label valueLabel = cardValues.get(card);
        valueLabel.setText(value);
        markUnavailable(valueLabel, !available);

        Label change = cardChanges.get(card);
        change.setText(changeText);
        change.getStyleClass().removeAll("change-good", "change-bad", "change-none");
        change.getStyleClass().add(switch (tone) {
            case GOOD -> "change-good";
            case BAD -> "change-bad";
            case NEUTRAL -> "change-none";
        });
    }

    /** Fills one KPI row. "available" is false when the figure cannot be worked out. */
    public void setKpi(Kpi kpi, String value, boolean available, String note) {
        Label valueLabel = kpiValues.get(kpi);
        valueLabel.setText(value);
        markUnavailable(valueLabel, !available);
        kpiNotes.get(kpi).setText(note);
    }

    public void setMonths(List<MonthRow> rows) { table.getItems().setAll(rows); }
}