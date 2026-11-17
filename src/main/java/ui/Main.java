package ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import database.DBConnection;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Main.java
 *
 * Starts the application and builds the application shell:
 *
 *   +----------------+--------------------------------------------+
 *   |  dark sidebar  |  page header                               |
 *   |  brand + nav   |  page content (dashboard, forms, tables)   |
 *   +----------------+--------------------------------------------+
 *
 * Navigation lives in the sidebar only; each page puts its own title,
 * description and actions in a standard header row.
 *
 * The public navigation methods (showDashboard(), showRegister(), ...) and the
 * message helpers (showAlert(), showError()) keep the same names as before, so
 * the page classes continue to work unchanged.
 */
public class Main {

    public static final String APP_TITLE = "Visitor Management System";

    /** The pages of the application. */
    public enum Page {
        DASHBOARD, REGISTER, LIST, SEARCH, CHECKOUT
    }

    public static void main(String[] args) {
        // Application.launch() must be started from a class that does NOT
        // extend Application - that is why Main and App are separated.
        App.launch(App.class, args);
    }

    // =========================================================
    // Messages (used by every page)
    // =========================================================

    /** Shows a popup message. No stack traces - only plain text. */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        switch (type) {
            case INFORMATION:
                Ui.showInfo(title, message);
                break;
            case WARNING:
                Ui.showWarning(title, message);
                break;
            case ERROR:
                Ui.showError(title, message);
                break;
            default:
                Ui.showInfo(title, message);
        }
    }

    /** Shows an error popup using the exception's friendly message. */
    public static void showError(String title, Exception e) {
        String message = (e.getMessage() == null || e.getMessage().trim().isEmpty())
                ? "Unexpected error. Please try again."
                : e.getMessage();
        Ui.showError(title, message);
    }

    /** The main window, used to centre dialogs over the application. */
    public static Window window() {
        return App.instance == null ? null : App.instance.stage;
    }

    // =========================================================
    // The JavaFX application itself
    // =========================================================

    public static class App extends Application {

        private static App instance;

        private Stage stage;
        private BorderPane root;
        private StackPane pageHost;

        // Navigation buttons, kept so the active page can be highlighted.
        private final Map<Page, Button> navButtons = new EnumMap<>(Page.class);

        // Sidebar pieces that change when the window becomes narrow.
        private VBox sidebar;
        private VBox brandText;
        private VBox sidebarFooter;
        private Label navLabel;
        private boolean sidebarCollapsed;

        /** Below this window width the sidebar collapses to icons only. */
        private static final double COLLAPSE_BELOW = 1120;

        @Override
        public void start(Stage stage) {
            instance = this;
            this.stage = stage;

            // --- 1. Test the database connection before showing the window ---
            try {
                DBConnection.getConnection();
            } catch (Exception e) {
                showDatabaseErrorAndExit(e);
                return;
            }

            // --- 2. Build the shell ---
            pageHost = new StackPane();
            pageHost.getStyleClass().add("content-area");

            root = new BorderPane();
            root.getStyleClass().add("app-root");
            root.setLeft(createSidebar());
            root.setCenter(pageHost);

            Scene scene = new Scene(root, 1220, 780);

            // Load the stylesheet (src/main/resources/styles.css).
            if (getClass().getResource("/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            }

            registerShortcuts(scene);

            stage.setTitle(APP_TITLE);
            stage.setScene(scene);
            stage.setMinWidth(980);
            stage.setMinHeight(640);
            stage.setOnCloseRequest(e -> DBConnection.close());
            stage.show();

            stage.widthProperty().addListener((obs, oldWidth, newWidth) ->
                    updateSidebar(newWidth.doubleValue() < COLLAPSE_BELOW));
            updateSidebar(stage.getWidth() < COLLAPSE_BELOW);

            // --- 3. Show the first page ---
            showDashboard();
        }

        // ---------------------------------------------------------
        // Sidebar
        // ---------------------------------------------------------

        private VBox createSidebar() {
            sidebar = new VBox(4);
            sidebar.getStyleClass().add("sidebar");

            FontIcon brandIcon = new FontIcon(FontAwesomeSolid.USER_SHIELD);
            brandIcon.setIconSize(17);
            brandIcon.setIconColor(Color.WHITE);
            StackPane brandMark = new StackPane(brandIcon);
            brandMark.getStyleClass().add("brand-mark");

            Label brandTitle = new Label("Visitor Management");
            brandTitle.getStyleClass().add("brand-title");
            Label brandSubtitle = new Label("Front desk console");
            brandSubtitle.getStyleClass().add("brand-subtitle");

            brandText = new VBox(1, brandTitle, brandSubtitle);
            brandText.setAlignment(Pos.CENTER_LEFT);

            HBox brand = new HBox(11, brandMark, brandText);
            brand.setAlignment(Pos.CENTER_LEFT);
            brand.setPadding(new Insets(0, 6, 0, 6));

            navLabel = new Label("NAVIGATION");
            navLabel.getStyleClass().add("nav-label");
            VBox.setMargin(navLabel, new Insets(26, 0, 6, 6));

            navButtons.put(Page.DASHBOARD,
                    createNavButton(Page.DASHBOARD, "Dashboard", FontAwesomeSolid.TH_LARGE));
            navButtons.put(Page.REGISTER,
                    createNavButton(Page.REGISTER, "Register Visitor", FontAwesomeSolid.USER_PLUS));
            navButtons.put(Page.LIST,
                    createNavButton(Page.LIST, "View Visitors", FontAwesomeSolid.USERS));
            navButtons.put(Page.SEARCH,
                    createNavButton(Page.SEARCH, "Search Visitors", FontAwesomeSolid.SEARCH));
            navButtons.put(Page.CHECKOUT,
                    createNavButton(Page.CHECKOUT, "Check Out", FontAwesomeSolid.SIGN_OUT_ALT));

            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);

            Region divider = new Region();
            divider.getStyleClass().add("sidebar-divider");
            divider.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(divider, new Insets(14, 6, 14, 6));

            Label todayLabel = new Label("TODAY");
            todayLabel.getStyleClass().add("sidebar-footer-text");
            Label dateLabel = new Label(DateTimeFormatter
                    .ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
                    .format(LocalDate.now()));
            dateLabel.getStyleClass().add("sidebar-footer-title");
            sidebarFooter = new VBox(3, todayLabel, dateLabel);
            sidebarFooter.setPadding(new Insets(0, 6, 0, 6));

            sidebar.getChildren().addAll(brand, navLabel);
            sidebar.getChildren().addAll(navButtons.values());
            sidebar.getChildren().addAll(spacer, divider, sidebarFooter);
            return sidebar;
        }

        private Button createNavButton(Page page, String text, Ikon ikon) {
            Button button = new Button(text);
            button.setGraphic(Ui.icon(ikon, 14));
            button.getStyleClass().add("nav-item");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setAccessibleText(text);
            button.getProperties().put("navText", text);
            button.setOnAction(e -> navigate(page));
            return button;
        }

        /**
         * Switches the sidebar between the labelled and the icon-only layout.
         * Keeping the icons (with tooltips) means navigation stays obvious and
         * the data pages get their horizontal space back on small windows.
         */
        private void updateSidebar(boolean collapsed) {
            if (sidebar == null || collapsed == sidebarCollapsed) {
                return;
            }
            sidebarCollapsed = collapsed;

            if (collapsed) {
                if (!sidebar.getStyleClass().contains("sidebar-collapsed")) {
                    sidebar.getStyleClass().add("sidebar-collapsed");
                }
            } else {
                sidebar.getStyleClass().remove("sidebar-collapsed");
            }

            brandText.setVisible(!collapsed);
            brandText.setManaged(!collapsed);
            navLabel.setVisible(!collapsed);
            navLabel.setManaged(!collapsed);
            sidebarFooter.setVisible(!collapsed);
            sidebarFooter.setManaged(!collapsed);

            navButtons.values().forEach(button -> {
                String text = (String) button.getProperties().get("navText");
                button.setText(collapsed ? "" : text);
                button.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
                button.setTooltip(collapsed ? new javafx.scene.control.Tooltip(text) : null);
            });
        }

        // ---------------------------------------------------------
        // Navigation
        // ---------------------------------------------------------

        private void registerShortcuts(Scene scene) {
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN),
                    this::showDashboard);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN),
                    this::showRegister);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN),
                    this::showVisitorList);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN),
                    this::showSearch);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN),
                    this::showCheckout);
        }

        /** Navigates to a page. Used by the sidebar and by in-page actions. */
        private void navigate(Page page) {
            switch (page) {
                case REGISTER:
                    showRegister();
                    break;
                case LIST:
                    showVisitorList();
                    break;
                case SEARCH:
                    showSearch();
                    break;
                case CHECKOUT:
                    showCheckout();
                    break;
                case DASHBOARD:
                default:
                    showDashboard();
            }
        }

        /**
         * Navigates from anywhere inside the application (for example from an
         * empty-state button on the dashboard).
         */
        public static void go(Page page) {
            if (instance != null) {
                instance.navigate(page);
            }
        }

        /** Puts a page in the content area, highlights its nav item and fades it in. */
        private void showPage(Node page, Page active) {
            pageHost.getChildren().setAll(page);
            navButtons.forEach((key, button) -> {
                button.getStyleClass().remove("nav-item-active");
                if (key == active) {
                    button.getStyleClass().add("nav-item-active");
                }
            });

            FadeTransition fade = new FadeTransition(Duration.millis(160), page);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }

        public void showDashboard() {
            showPage(new Dashboard(), Page.DASHBOARD);
        }

        public void showRegister() {
            showPage(new RegisterVisitor(), Page.REGISTER);
        }

        public void showVisitorList() {
            showPage(new VisitorList(), Page.LIST);
        }

        public void showSearch() {
            showPage(new SearchVisitor(), Page.SEARCH);
        }

        public void showCheckout() {
            showPage(new CheckoutVisitor(), Page.CHECKOUT);
        }

        /**
         * Called when MySQL cannot be reached at startup.
         * Explains the problem and closes the application.
         */
        private void showDatabaseErrorAndExit(Exception e) {
            Ui.showError("Cannot connect to MySQL",
                    String.valueOf(e.getMessage())
                            + "\n\nHow to fix it:\n"
                            + "1. Make sure MySQL Server is running.\n"
                            + "2. Run database.sql to create the database.\n"
                            + "3. Check USER and PASSWORD in src/main/java/database/DBConnection.java\n"
                            + "4. Start the application again.");
            Platform.exit();
        }
    }
}
