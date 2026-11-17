package ui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Visitor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import service.VisitorService;

/**
 * VisitorList.java
 *
 * Shows every visitor in a readable table and keeps the shared table and
 * formatting helpers used by SearchVisitor, CheckoutVisitor and Dashboard.
 *
 * The table keeps all ten original columns. Long values are shortened with an
 * ellipsis and the full text is available as a tooltip; the status column uses
 * a badge with a text label so it never depends on colour alone.
 */
public class VisitorList extends BorderPane {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("dd-MMM-yy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    private final VisitorService service = new VisitorService();
    private final TableView<Visitor> table = createVisitorTable();
    private final Label countLabel = new Label();
    private final InlineMessage message = new InlineMessage();
    private final Button refreshButton =
            Ui.button("Refresh", FontAwesomeSolid.SYNC_ALT, "btn-secondary");

    public VisitorList() {
        refreshButton.setOnAction(e -> loadVisitors());

        countLabel.getStyleClass().add("date-chip");

        setTop(Ui.pageHeader(
                "View Visitors",
                "Every visitor record in the register, newest first.",
                countLabel,
                refreshButton));

        message.setMaxWidth(Double.MAX_VALUE);
        VBox content = new VBox(12, message, table);
        content.setFillWidth(true);
        VBox.setVgrow(table, Priority.ALWAYS);
        setCenter(content);

        loadVisitors();
    }

    /** Loads all visitors from MySQL into the table. */
    private void loadVisitors() {
        message.clear();
        refreshButton.setDisable(true);
        try {
            ObservableList<Visitor> rows =
                    FXCollections.observableArrayList(service.getAllVisitors());
            table.setItems(rows);
            countLabel.setText(rows.size() == 1 ? "1 record" : rows.size() + " records");
            updatePlaceholder(rows.isEmpty());
        } catch (Exception e) {
            table.setItems(FXCollections.observableArrayList());
            countLabel.setText("—");
            table.setPlaceholder(Ui.emptyState(
                    FontAwesomeSolid.EXCLAMATION_TRIANGLE,
                    "Could not load visitors",
                    "Check that MySQL is running, then press Refresh."));
            message.showError("Could not load the visitor list", friendly(e));
        } finally {
            refreshButton.setDisable(false);
        }
    }

    private void updatePlaceholder(boolean empty) {
        if (!empty) {
            table.setPlaceholder(new Label("No visitors to show."));
            return;
        }
        Button register = Ui.button("Register a visitor", FontAwesomeSolid.USER_PLUS, "btn-primary");
        register.setOnAction(e -> Main.App.go(Main.Page.REGISTER));
        table.setPlaceholder(Ui.emptyState(
                FontAwesomeSolid.INBOX,
                "No visitor records yet",
                "Registered visitors will appear here with their check-in time, duration and status.",
                register));
    }

    // =========================================================
    // Shared helpers (used by SearchVisitor, CheckoutVisitor and Dashboard)
    // =========================================================

    /** Creates the visitor table with all ten original columns. */
    public static TableView<Visitor> createVisitorTable() {
        TableView<Visitor> table = new TableView<>();
        table.setPlaceholder(new Label("No visitors to show."));
        table.setFocusTraversable(true);

        TableColumn<Visitor, String> idCol = textColumn("ID", 52,
                v -> String.valueOf(v.getVisitorId()), false);
        TableColumn<Visitor, String> nameCol = textColumn("Name", 128,
                Visitor::getVisitorName, true);
        TableColumn<Visitor, String> phoneCol = textColumn("Phone", 96,
                Visitor::getPhone, false);
        TableColumn<Visitor, String> meetCol = textColumn("Person to Meet", 112,
                Visitor::getPersonToMeet, true);
        TableColumn<Visitor, String> reasonCol = textColumn("Reason", 112,
                Visitor::getReason, true);
        TableColumn<Visitor, String> dateCol = textColumn("Date", 90,
                v -> v.getVisitDate() == null ? "" : formatDateShort(v.getVisitDate()), false);
        TableColumn<Visitor, String> inCol = textColumn("In Time", 78,
                v -> formatTime(v.getInTime()), false);
        TableColumn<Visitor, String> outCol = textColumn("Out Time", 88,
                v -> formatTime(v.getOutTime()), false);
        TableColumn<Visitor, String> durCol = textColumn("Duration", 92,
                v -> Visitor.formatDuration(v.getTotalDuration()), false);

        TableColumn<Visitor, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(92);
        statusCol.setMinWidth(80);
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStatus() == null ? "" : data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : Ui.statusBadge(item));
            }
        });

        table.getColumns().addAll(idCol, nameCol, phoneCol, meetCol, reasonCol,
                dateCol, inCol, outCol, durCol, statusCol);

        // Columns share the available width so the table always fits the window.
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    /** Creates a text column, optionally showing the full value as a tooltip. */
    private static TableColumn<Visitor, String> textColumn(
            String title, double width, Function<Visitor, String> value, boolean tooltip) {
        TableColumn<Visitor, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(data -> {
            String text = value.apply(data.getValue());
            return new SimpleStringProperty(text == null ? "" : text);
        });

        if (tooltip) {
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setTooltip(empty || item == null || item.length() < 16
                            ? null
                            : new Tooltip(item));
                }
            });
        }
        return col;
    }

    /** Formats a date as 05-Oct-2026 (returns "" when null). */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    /** Compact date used inside table columns: 05-Oct-26. */
    public static String formatDateShort(LocalDate date) {
        return date == null ? "" : DATE_FORMAT_SHORT.format(date);
    }

    /** Formats a time as 10:30 AM (returns "-" when null). */
    public static String formatTime(LocalTime time) {
        return time == null ? "-" : TIME_FORMAT.format(time);
    }

    /** Builds a short text summary of a visitor - used inside success messages. */
    public static String describe(Visitor v) {
        return "Visitor ID: " + v.getVisitorId()
                + "\nName: " + v.getVisitorName()
                + "\nPhone: " + v.getPhone()
                + "\nIn time: " + formatTime(v.getInTime())
                + "\nOut time: " + formatTime(v.getOutTime())
                + "\nDuration: " + Visitor.formatDuration(v.getTotalDuration())
                + "\nStatus: " + v.getStatus();
    }

    // =========================================================
    // Small helpers
    // =========================================================

    private String friendly(Exception e) {
        String text = e.getMessage();
        return (text == null || text.trim().isEmpty())
                ? "An unexpected problem occurred."
                : text;
    }
}
