package ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Ui.java
 *
 * Small factory of shared interface pieces (page headers, cards, badges,
 * banners, empty states, dialogs). Every page builds its layout from these
 * helpers, so spacing, colours and typography stay consistent.
 *
 * Presentation only: this class never touches the database or the service layer.
 */
public final class Ui {

    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);

    private Ui() {
    }

    // =========================================================
    // Icons
    // =========================================================

    /**
     * Creates a vector icon from a bundled icon font (Ikonli / Font Awesome 5).
     * Falls back to an empty region if the icon font is ever unavailable, so a
     * missing icon can never crash the interface.
     */
    public static Node icon(Ikon ikon, int size) {
        try {
            FontIcon icon = new FontIcon(ikon);
            icon.setIconSize(size);
            return icon;
        } catch (RuntimeException e) {
            return new Region();
        }
    }

    /** Same as {@link #icon(Ikon, int)} but with a colour style class. */
    public static Node icon(Ikon ikon, int size, String colorStyleClass) {
        Node node = icon(ikon, size);
        if (node instanceof FontIcon) {
            node.getStyleClass().add(colorStyleClass);
        }
        return node;
    }

    // =========================================================
    // Labels
    // =========================================================

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        if (styleClass != null && !styleClass.isEmpty()) {
            label.getStyleClass().add(styleClass);
        }
        return label;
    }

    /** Muted wrapping paragraph used for supporting text and hints. */
    public static Label paragraph(String text, String styleClass) {
        Label label = label(text, styleClass);
        label.setWrapText(true);
        return label;
    }

    // =========================================================
    // Page header
    // =========================================================

    /**
     * Standard page header: title, one-line description and optional actions
     * (refresh buttons, counters, ...) aligned to the right.
     */
    public static HBox pageHeader(String title, String description, Node... actions) {
        VBox titles = new VBox(4);
        titles.setAlignment(Pos.CENTER_LEFT);
        titles.getChildren().add(label(title, "page-title"));
        if (description != null && !description.isEmpty()) {
            titles.getChildren().add(paragraph(description, "page-subtitle"));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(16, titles, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("page-header");

        if (actions != null && actions.length > 0) {
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            actionBox.setMinWidth(Region.USE_PREF_SIZE);
            actionBox.getStyleClass().add("page-header-actions");
            for (Node action : actions) {
                if (action instanceof Region) {
                    // Actions keep their natural size; only the title area shrinks.
                    ((Region) action).setMinWidth(Region.USE_PREF_SIZE);
                }
                actionBox.getChildren().add(action);
            }
            header.getChildren().add(actionBox);
        }
        return header;
    }

    /** Subtle "Wednesday, 22 Oct 2026" chip for the page header. */
    public static Label dateChip() {
        return label(LONG_DATE.format(LocalDate.now()), "date-chip");
    }

    /** Small outlined counter, e.g. "12 visitors". */
    public static Label counterChip(String text) {
        return label(text, "date-chip");
    }

    // =========================================================
    // Containers
    // =========================================================

    /** White surface with border, radius and a very soft shadow. */
    public static VBox card(double spacing, Node... children) {
        VBox card = new VBox(spacing);
        card.getStyleClass().add("card");
        card.getChildren().addAll(children);
        return card;
    }

    /** Header row inside a card: title + optional caption and right-hand node. */
    public static HBox sectionHeader(String title, String caption, Node right) {
        VBox titles = new VBox(3);
        titles.getChildren().add(label(title, "section-title"));
        if (caption != null && !caption.isEmpty()) {
            titles.getChildren().add(paragraph(caption, "section-caption"));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, titles, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("card-header");
        if (right != null) {
            header.getChildren().add(right);
        }
        return header;
    }

    public static Region divider() {
        Region divider = new Region();
        divider.getStyleClass().add("divider");
        divider.setMaxWidth(Double.MAX_VALUE);
        return divider;
    }

    // =========================================================
    // Status badge
    // =========================================================

    /**
     * Rounded status badge with a coloured dot AND a text label, so the status
     * is readable even without relying on colour.
     */
    public static HBox statusBadge(String status) {
        boolean inside = "INSIDE".equalsIgnoreCase(status == null ? "" : status.trim());

        Circle dot = new Circle(3.5);
        dot.setFill(Color.web(inside ? "#16865C" : "#7C8CA6"));

        Label text = new Label(inside ? "INSIDE" : "OUT");
        text.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold;");
        text.setTextFill(Color.web(inside ? "#116B49" : "#5A6880"));

        HBox badge = new HBox(6, dot, text);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        badge.getStyleClass().addAll("status-badge", inside ? "status-badge-inside" : "status-badge-out");
        return badge;
    }

    // =========================================================
    // Empty states
    // =========================================================

    /**
     * Friendly empty state: soft icon circle, short title, explanation and
     * optional action buttons.
     */
    public static VBox emptyState(Ikon ikon, String title, String text, Node... actions) {
        StackPane iconCircle = new StackPane(icon(ikon, 18, "icon-muted"));
        iconCircle.getStyleClass().add("empty-state-icon");

        VBox box = new VBox(iconCircle,
                label(title, "empty-state-title"),
                paragraph(text, "empty-state-text"));
        box.setAlignment(Pos.CENTER);
        box.setSpacing(8);
        box.getStyleClass().add("empty-state");

        if (actions != null && actions.length > 0) {
            HBox actionBox = new HBox(8, actions);
            actionBox.setAlignment(Pos.CENTER);
            VBox.setMargin(actionBox, new javafx.geometry.Insets(6, 0, 0, 0));
            box.getChildren().add(actionBox);
        }
        return box;
    }

    // =========================================================
    // Buttons
    // =========================================================

    /** Text button with an optional leading icon. */
    public static Button button(String text, Ikon ikon, String styleClass) {
        Button button = new Button(text);
        if (ikon != null) {
            button.setGraphic(icon(ikon, 13));
        }
        button.getStyleClass().add(styleClass);
        return button;
    }

    /** Icon-only button with an accessible name and a tooltip. */
    public static Button iconButton(Ikon ikon, String accessibleName) {
        Button button = new Button();
        button.setGraphic(icon(ikon, 14, "icon-neutral"));
        button.getStyleClass().add("btn-icon");
        button.setAccessibleText(accessibleName);
        button.setTooltip(new Tooltip(accessibleName));
        return button;
    }

    // =========================================================
    // Dialogs
    // =========================================================

    /** Applies the application stylesheet to a dialog so it matches the app. */
    private static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (Ui.class.getResource("/styles.css") != null) {
            pane.getStylesheets().add(Ui.class.getResource("/styles.css").toExternalForm());
        }
        Window owner = Main.window();
        if (owner != null) {
            dialog.initOwner(owner);
        }
    }

    /** Shows an informational dialog (used for blocking notices). */
    public static void showInfo(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    /** Shows a warning dialog (invalid input that could not be shown inline). */
    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    /** Shows an error dialog with a friendly, non-technical message. */
    public static void showError(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.setResizable(true);
        styleDialog(alert);
        alert.showAndWait();
    }

    /**
     * Asks the user to confirm an action.
     *
     * @return true only when the user explicitly confirms.
     */
    public static boolean confirm(String title, String header, String message,
                                  String confirmText, String cancelText) {
        ButtonType confirmType = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, cancelType, confirmType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setResizable(true);
        styleDialog(alert);

        Node confirmButton = alert.getDialogPane().lookupButton(confirmType);
        if (confirmButton != null) {
            confirmButton.getStyleClass().add("btn-primary");
        }
        Node cancelButton = alert.getDialogPane().lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.getStyleClass().add("btn-secondary");
        }

        return alert.showAndWait().filter(confirmType::equals).isPresent();
    }
}
