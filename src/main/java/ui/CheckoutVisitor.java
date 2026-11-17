package ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Visitor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import service.VisitorService;

/**
 * CheckoutVisitor.java
 *
 * Shows the visitors who are currently INSIDE. Selecting a row fills the
 * detail bar with that visitor's information; checking out is a deliberate
 * two-step action (select, confirm) so a visit cannot be closed by accident.
 *
 * The business rules are unchanged: the service records the automatic out time,
 * calculates the duration and updates the status from INSIDE to OUT. A visitor
 * who is already OUT never appears here in the first place.
 */
public class CheckoutVisitor extends BorderPane {

    private final VisitorService service = new VisitorService();

    private final TableView<Visitor> table = VisitorList.createVisitorTable();
    private final ObservableList<Visitor> insideVisitors = FXCollections.observableArrayList();
    private final FilteredList<Visitor> filteredList = new FilteredList<>(insideVisitors);

    private final TextField filterField = new TextField();
    private final Label counterLabel = new Label();
    private final InlineMessage message = new InlineMessage();
    private final VBox detailBar = new VBox();

    private final Button checkoutButton =
            Ui.button("Check Out Visitor", FontAwesomeSolid.SIGN_OUT_ALT, "btn-primary");
    private final Button refreshButton =
            Ui.button("Refresh", FontAwesomeSolid.SYNC_ALT, "btn-secondary");
    private final Button clearFilterButton = Ui.iconButton(FontAwesomeSolid.TIMES, "Clear filter");

    private Visitor selectedVisitor;

    public CheckoutVisitor() {
        refreshButton.setOnAction(e -> loadInsideVisitors());
        clearFilterButton.setOnAction(e -> {
            filterField.clear();
            applyFilter();
            filterField.requestFocus();
        });

        counterLabel.getStyleClass().add("date-chip");

        setTop(Ui.pageHeader(
                "Check Out",
                "Close a visit for someone who is currently inside.",
                counterLabel,
                refreshButton));

        // ---------- Filter row ----------
        filterField.setPromptText("Filter the inside list by ID, name or phone…");
        filterField.setAccessibleText("Filter visitors who are currently inside");
        filterField.getStyleClass().add("search-field");
        filterField.setPrefWidth(420);
        filterField.setMaxWidth(420);
        HBox.setHgrow(filterField, Priority.NEVER);
        filterField.textProperty().addListener((obs, oldText, newText) -> applyFilter());

        HBox filterRow = new HBox(8, filterField, clearFilterButton);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        // ---------- Table ----------
        table.setItems(filteredList);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showDetails(newValue));

        // Double click selects a row and opens the confirmation dialog.
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                checkOutSelected();
            }
        });

        VBox tableColumn = new VBox(10, filterRow, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ---------- Detail bar ----------
        detailBar.setFillWidth(true);

        message.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(12, message, tableColumn, detailBar);
        content.setFillWidth(true);
        VBox.setVgrow(tableColumn, Priority.ALWAYS);
        setCenter(content);

        showDetails(null);
        loadInsideVisitors();
    }

    // =========================================================
    // Data
    // =========================================================

    /** Loads every visitor that is still inside the building. */
    private void loadInsideVisitors() {
        refreshButton.setDisable(true);
        try {
            Visitor previouslySelected = selectedVisitor;
            List<Visitor> list = service.getInsideVisitors();
            insideVisitors.setAll(list);
            counterLabel.setText(list.size() == 1 ? "1 inside" : list.size() + " inside");

            // Keep a visitor selected after a refresh when they are still inside.
            if (previouslySelected != null) {
                insideVisitors.stream()
                        .filter(v -> v.getVisitorId() == previouslySelected.getVisitorId())
                        .findFirst()
                        .ifPresent(v -> table.getSelectionModel().select(v));
            }
            updatePlaceholder(list.isEmpty());
            applyFilter();
        } catch (Exception e) {
            insideVisitors.clear();
            counterLabel.setText("—");
            table.setPlaceholder(Ui.emptyState(
                    FontAwesomeSolid.EXCLAMATION_TRIANGLE,
                    "Could not load visitors",
                    "Check that MySQL is running, then press Refresh."));
            message.showError("Could not load the inside list", friendly(e));
        } finally {
            refreshButton.setDisable(false);
        }
    }

    private void updatePlaceholder(boolean empty) {
        if (!empty) {
            table.setPlaceholder(new Label("No visitors to show."));
            return;
        }
        Button register = Ui.button("Register a visitor", FontAwesomeSolid.USER_PLUS, "btn-secondary");
        register.setOnAction(e -> Main.App.go(Main.Page.REGISTER));
        register.setMinWidth(Region.USE_PREF_SIZE);
        table.setPlaceholder(Ui.emptyState(
                FontAwesomeSolid.DOOR_OPEN,
                "Nobody is inside right now",
                "Visitors appear in this list after they are registered. "
                        + "Once a visit is completed it is marked OUT.",
                register));
    }

    /** Keeps only the rows that match the text in the filter box. */
    private void applyFilter() {
        String text = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        filteredList.setPredicate(v -> {
            if (text.isEmpty()) {
                return true;
            }
            return String.valueOf(v.getVisitorId()).contains(text)
                    || (v.getVisitorName() != null && v.getVisitorName().toLowerCase().contains(text))
                    || (v.getPhone() != null && v.getPhone().toLowerCase().contains(text))
                    || (v.getPersonToMeet() != null
                        && v.getPersonToMeet().toLowerCase().contains(text));
        });

        if (!text.isEmpty() && filteredList.isEmpty() && !insideVisitors.isEmpty()) {
            table.setPlaceholder(Ui.emptyState(
                    FontAwesomeSolid.FILTER,
                    "No matching visitors",
                    "Nobody inside matches \"" + filterField.getText().trim()
                            + "\". Clear the filter to see everyone who is inside."));
        } else {
            updatePlaceholder(insideVisitors.isEmpty());
        }
    }

    // =========================================================
    // Selection detail bar
    // =========================================================

    /** Rebuilds the bottom bar for the selected visitor (or the empty hint). */
    private void showDetails(Visitor visitor) {
        selectedVisitor = visitor;
        detailBar.getChildren().clear();

        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 16, 14, 16));
        bar.getStyleClass().add("card");

        if (visitor == null) {
            VBox hint = new VBox(2,
                    Ui.label("No visitor selected", "section-title"),
                    Ui.paragraph("Select a visitor from the list to review their details "
                            + "and check them out.", "section-caption"));
            bar.getChildren().add(hint);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            checkoutButton.setDisable(true);
            checkoutButton.setMinWidth(Region.USE_PREF_SIZE);
            HBox.setHgrow(checkoutButton, Priority.NEVER);
            bar.getChildren().addAll(spacer, checkoutButton);
            detailBar.getChildren().add(bar);
            return;
        }

        StackPane avatar = new StackPane(Ui.label(initialOf(visitor.getVisitorName()), "avatar-initial"));
        avatar.getStyleClass().add("avatar");

        Label name = Ui.label(visitor.getVisitorName(), "section-title");
        Label meta = Ui.label(
                "ID " + visitor.getVisitorId()
                        + "   ·   " + visitor.getPhone()
                        + "   ·   Meeting " + visitor.getPersonToMeet()
                        + "   ·   Checked in " + VisitorList.formatTime(visitor.getInTime()),
                "section-caption");
        Label elapsed = Ui.label("Inside for " + elapsedSoFar(visitor), "field-hint");

        meta.setWrapText(true);
        VBox texts = new VBox(3, name, meta, elapsed);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        checkoutButton.setMinWidth(Region.USE_PREF_SIZE);
        checkoutButton.setDisable(false);
        checkoutButton.setOnAction(e -> checkOutSelected());

        bar.getChildren().addAll(avatar, texts, spacer, checkoutButton);
        detailBar.getChildren().add(bar);
    }

    // =========================================================
    // Check out
    // =========================================================

    /** Confirms, then checks the selected visitor out. */
    private void checkOutSelected() {
        Visitor selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            message.showWarning("Select a visitor first",
                    "Choose the visitor who is leaving from the list, then press Check Out.");
            return;
        }

        String duration = Visitor.formatDuration((int) Math.max(0,
                Duration.between(
                        LocalDateTime.of(selected.getVisitDate(), selected.getInTime()),
                        LocalDateTime.now()).toMinutes()));

        boolean confirmed = Ui.confirm(
                "Check out visitor",
                "Check out " + selected.getVisitorName() + "?",
                "Visitor ID: " + selected.getVisitorId()
                        + "\nPhone: " + selected.getPhone()
                        + "\nMeeting: " + selected.getPersonToMeet()
                        + "\nChecked in: " + VisitorList.formatTime(selected.getInTime())
                        + "\nDuration so far: " + duration
                        + "\n\nThis records the check-out time and marks the visit as OUT.",
                "Check Out",
                "Cancel");

        if (!confirmed) {
            return;
        }

        checkoutButton.setDisable(true);
        message.clear();
        try {
            Visitor updated = service.checkOutVisitor(selected.getVisitorId());

            message.showSuccess(
                    "Check-out complete — " + updated.getVisitorName(),
                    "Out time " + VisitorList.formatTime(updated.getOutTime())
                            + " · Total duration " + Visitor.formatDuration(updated.getTotalDuration())
                            + " · Visitor ID " + updated.getVisitorId()
                            + " is now marked " + updated.getStatus() + ".");

            selectedVisitor = null;
            table.getSelectionModel().clearSelection();
            loadInsideVisitors();
            showDetails(null);

        } catch (IllegalStateException | IllegalArgumentException e) {
            message.showWarning("Could not check out this visitor",
                    e.getMessage() + " The list has been refreshed.");
            selectedVisitor = null;
            table.getSelectionModel().clearSelection();
            loadInsideVisitors();
            showDetails(null);
        } catch (Exception e) {
            message.showError("Check out failed", friendly(e));
        } finally {
            checkoutButton.setDisable(selectedVisitor == null);
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Human readable time the visitor has already been inside. */
    private String elapsedSoFar(Visitor visitor) {
        if (visitor.getVisitDate() == null || visitor.getInTime() == null) {
            return "unknown time";
        }
        long minutes = Duration.between(
                LocalDateTime.of(visitor.getVisitDate(), visitor.getInTime()),
                LocalDateTime.now()).toMinutes();
        return Visitor.formatDuration((int) Math.max(0, minutes));
    }

    private String initialOf(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        return name.trim().substring(0, 1).toUpperCase();
    }

    private String friendly(Exception e) {
        String text = e.getMessage();
        return (text == null || text.trim().isEmpty())
                ? "An unexpected problem occurred."
                : text;
    }
}
