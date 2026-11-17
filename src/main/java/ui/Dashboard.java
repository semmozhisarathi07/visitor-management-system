package ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Visitor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import service.VisitorService;

/**
 * Dashboard.java
 *
 * The overview page. Everything on it comes from the database:
 *   - four summary cards (all visitors, inside now, today, completed today)
 *   - a seven day check-in chart built from real visit dates
 *   - the five most recent visitor records
 *
 * When the database is empty the page shows a helpful empty state instead of
 * a blank chart.
 */
public class Dashboard extends BorderPane {

    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_TOOLTIP =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);

    private static final int RECENT_LIMIT = 5;

    private final VisitorService service = new VisitorService();

    private final StatCard totalCard =
            new StatCard("Total visitors", FontAwesomeSolid.USERS, StatCard.Tone.ACCENT);
    private final StatCard insideCard =
            new StatCard("Currently inside", FontAwesomeSolid.DOOR_OPEN, StatCard.Tone.SUCCESS);
    private final StatCard todayCard =
            new StatCard("Visits today", FontAwesomeSolid.CALENDAR_DAY, StatCard.Tone.WARNING);
    private final StatCard completedCard =
            new StatCard("Completed today", FontAwesomeSolid.CHECK_CIRCLE, StatCard.Tone.NEUTRAL);

    private final InlineMessage message = new InlineMessage();

    private final GridPane statsGrid = new GridPane();
    private int statsColumns = 0;

    private final VBox chartHolder = new VBox();
    private final Label chartSummary = new Label();

    private final TableView<Visitor> recentTable = new TableView<>();
    private final ObservableList<Visitor> recentRows = FXCollections.observableArrayList();
    private final VBox recentHolder = new VBox();

    private final Button refreshButton =
            Ui.button("Refresh", FontAwesomeSolid.SYNC_ALT, "btn-secondary");

    public Dashboard() {
        Button registerShortcut =
                Ui.button("Register visitor", FontAwesomeSolid.USER_PLUS, "btn-primary");

        refreshButton.setOnAction(e -> loadStatistics());
        registerShortcut.setOnAction(e -> Main.App.go(Main.Page.REGISTER));

        setTop(Ui.pageHeader(
                "Dashboard",
                "A live overview of visitor activity at the front desk.",
                Ui.dateChip(),
                refreshButton));

        statsGrid.setHgap(16);
        statsGrid.setVgap(16);
        layoutStats(4);

        // Four cards on a wide window, two by two when the window is narrower.
        widthProperty().addListener((obs, oldWidth, newWidth) ->
                layoutStats(newWidth.doubleValue() < 840 ? 2 : 4));

        VBox content = new VBox(18,
                message,
                statsGrid,
                buildActivityCard(),
                buildRecentCard());
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(scroll);

        loadStatistics();
    }

    /** Places the four summary cards in the requested number of columns. */
    private void layoutStats(int columns) {
        if (columns == statsColumns) {
            return;
        }
        statsColumns = columns;

        StatCard[] cards = {totalCard, insideCard, todayCard, completedCard};
        statsGrid.getChildren().clear();
        statsGrid.getColumnConstraints().clear();

        for (int i = 0; i < cards.length; i++) {
            cards[i].setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(cards[i], Priority.ALWAYS);
            statsGrid.add(cards[i], i % columns, i / columns);
        }
        for (int c = 0; c < columns; c++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / columns);
            constraint.setHgrow(Priority.ALWAYS);
            constraint.setFillWidth(true);
            statsGrid.getColumnConstraints().add(constraint);
        }
    }

    // =========================================================
    // Activity chart
    // =========================================================

    private VBox buildActivityCard() {
        chartSummary.getStyleClass().add("section-caption");
        VBox card = Ui.card(16,
                Ui.sectionHeader("Visitor activity", "Check-ins recorded per day (last 7 days).", chartSummary),
                Ui.divider(),
                chartHolder);
        return card;
    }

    /**
     * Builds a small bar chart using plain JavaFX layout nodes. Keeping it
     * hand-built (instead of a chart library) means it follows the design
     * system exactly and needs no extra dependency.
     */
    private Node buildChart(Map<LocalDate, Integer> counts) {
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        HBox barsRow = new HBox(10);
        barsRow.setAlignment(Pos.BOTTOM_CENTER);
        barsRow.setPrefHeight(104);

        HBox labelsRow = new HBox(10);
        labelsRow.setAlignment(Pos.TOP_CENTER);

        LocalDate today = LocalDate.now();

        for (Map.Entry<LocalDate, Integer> entry : counts.entrySet()) {
            LocalDate day = entry.getKey();
            int value = entry.getValue();
            boolean isToday = day.equals(today);

            Label valueLabel = new Label(String.valueOf(value));
            valueLabel.getStyleClass().add("chart-value");
            if (value == 0) {
                valueLabel.getStyleClass().add("text-muted");
            }

            double height = max == 0 ? 4 : Math.max(4, 92.0 * value / max);
            Region bar = new Region();
            bar.getStyleClass().add(isToday ? "chart-bar-today" : "chart-bar");
            bar.setMinWidth(26);
            bar.setPrefWidth(26);
            bar.setMaxWidth(26);
            bar.setMinHeight(height);
            bar.setPrefHeight(height);
            bar.setMaxHeight(height);

            VBox column = new VBox(6, valueLabel, bar);
            column.setAlignment(Pos.BOTTOM_CENTER);
            column.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(column, Priority.ALWAYS);

            Tooltip.install(bar, new Tooltip(
                    DAY_TOOLTIP.format(day) + ": " + value + (value == 1 ? " visit" : " visits")));

            Label dayLabel = new Label(isToday ? "Today" : DAY_LABEL.format(day));
            dayLabel.getStyleClass().add("chart-day");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            HBox.setHgrow(dayLabel, Priority.ALWAYS);

            barsRow.getChildren().add(column);
            labelsRow.getChildren().add(dayLabel);
        }

        chartSummary.setText(total + (total == 1 ? " visit in 7 days" : " visits in 7 days"));

        VBox chart = new VBox(10, barsRow, Ui.divider(), labelsRow);
        return chart;
    }

    // =========================================================
    // Recent visitors
    // =========================================================

    private VBox buildRecentCard() {
        configureRecentTable();
        recentHolder.getChildren().setAll(recentTable);

        Button viewAll = Ui.button("View all", FontAwesomeSolid.ARROW_RIGHT, "btn-ghost");
        viewAll.setOnAction(e -> Main.App.go(Main.Page.LIST));

        return Ui.card(14,
                Ui.sectionHeader("Recent visitors", "The newest records in the register.", viewAll),
                Ui.divider(),
                recentHolder);
    }

    private void configureRecentTable() {
        recentTable.getStyleClass().add("compact-table");
        recentTable.setItems(recentRows);
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        recentTable.setPrefHeight(240);
        recentTable.setMinHeight(200);
        recentTable.setFocusTraversable(true);

        recentTable.getColumns().addAll(
                column("ID", 60, v -> String.valueOf(v.getVisitorId())),
                column("Name", 150, Visitor::getVisitorName),
                column("Person to Meet", 140, Visitor::getPersonToMeet),
                column("In Time", 90, v -> VisitorList.formatTime(v.getInTime())),
                statusColumn());
    }

    private TableColumn<Visitor, String> column(String title, double width,
                                                java.util.function.Function<Visitor, String> value) {
        TableColumn<Visitor, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                value.apply(data.getValue()) == null ? "" : value.apply(data.getValue())));
        return col;
    }

    private TableColumn<Visitor, String> statusColumn() {
        TableColumn<Visitor, String> col = new TableColumn<>("Status");
        col.setPrefWidth(100);
        col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : Ui.statusBadge(item));
            }
        });
        return col;
    }

    // =========================================================
    // Data
    // =========================================================

    /** Reads every number on the page from the service layer. */
    private void loadStatistics() {
        message.clear();
        refreshButton.setDisable(true);

        try {
            int totalVisitors = service.countAll();
            totalCard.setValue(totalVisitors);
            insideCard.setValue(service.countInside());
            todayCard.setValue(service.countVisitorsToday());
            completedCard.setValue(service.countCompletedToday());

            Map<LocalDate, Integer> counts = service.getVisitCountsForLastDays(7);
            if (totalVisitors == 0) {
                chartHolder.getChildren().setAll(Ui.emptyState(
                        FontAwesomeSolid.CHART_BAR,
                        "No visits recorded yet",
                        "Register the first visitor and today's activity will show up in this chart."));
                chartSummary.setText("No data");
            } else {
                chartHolder.getChildren().setAll(buildChart(counts));
            }

            recentRows.setAll(service.getRecentVisitors(RECENT_LIMIT));
            updateRecentPlaceholder();
        } catch (Exception e) {
            totalCard.setUnavailable();
            insideCard.setUnavailable();
            todayCard.setUnavailable();
            completedCard.setUnavailable();
            chartHolder.getChildren().setAll(Ui.emptyState(
                    FontAwesomeSolid.CHART_BAR,
                    "Activity is unavailable",
                    "The visitor statistics could not be loaded from the database."));
            chartSummary.setText("—");
            recentRows.clear();
            recentTable.setPlaceholder(Ui.emptyState(
                    FontAwesomeSolid.EXCLAMATION_TRIANGLE,
                    "Could not load visitors",
                    "Check that MySQL is running, then press Refresh."));
            message.showError("Could not load the dashboard",
                    friendly(e) + " Press Refresh to try again.");
        } finally {
            refreshButton.setDisable(false);
        }
    }

    private void updateRecentPlaceholder() {
        if (!recentRows.isEmpty()) {
            recentTable.setPlaceholder(new Label("No visitors yet."));
            return;
        }
        Button firstVisitor = Ui.button("Register the first visitor",
                FontAwesomeSolid.USER_PLUS, "btn-primary");
        firstVisitor.setOnAction(e -> Main.App.go(Main.Page.REGISTER));
        recentTable.setPlaceholder(Ui.emptyState(
                FontAwesomeSolid.INBOX,
                "No visitors yet",
                "Visitor records appear here as soon as the first visitor is registered.",
                firstVisitor));
    }

    /** Returns a short, non-technical message for an exception. */
    private String friendly(Exception e) {
        String text = e.getMessage();
        return (text == null || text.trim().isEmpty())
                ? "An unexpected problem occurred."
                : text;
    }
}
