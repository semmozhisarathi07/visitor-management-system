package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Visitor;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import service.VisitorService;

/**
 * SearchVisitor.java
 *
 * Finds visitors by visitor ID, name or phone number - the same search the
 * service layer already supported. Results are shown in the shared visitor
 * table; feedback is shown inside the page instead of in extra pop-ups.
 */
public class SearchVisitor extends BorderPane {

    private final VisitorService service = new VisitorService();
    private final TableView<Visitor> table = VisitorList.createVisitorTable();
    private final TextField searchField = new TextField();
    private final Label resultLabel = new Label();
    private final InlineMessage message = new InlineMessage();

    private final Button searchButton =
            Ui.button("Search", FontAwesomeSolid.SEARCH, "btn-primary");
    private final Button clearButton =
            Ui.button("Clear", FontAwesomeSolid.TIMES, "btn-secondary");

    public SearchVisitor() {
        setTop(Ui.pageHeader(
                "Search Visitors",
                "Find any visitor by visitor ID, name or phone number.",
                Ui.dateChip()));

        searchField.setPromptText("Enter a visitor ID, name or phone number…");
        searchField.getStyleClass().add("search-field");
        searchField.setAccessibleText("Search visitors by ID, name or phone number");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                doSearch();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                resetSearch();
            }
        });

        searchButton.setDefaultButton(true);
        searchButton.setOnAction(e -> doSearch());
        clearButton.setOnAction(e -> resetSearch());

        HBox searchRow = new HBox(10, searchField, clearButton, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        VBox searchCard = Ui.card(12,
                Ui.sectionHeader("Search the register",
                        "Searching is not case sensitive and matches partial values.", null),
                searchRow);

        resultLabel.getStyleClass().add("section-caption");
        resultLabel.setVisible(false);
        resultLabel.setManaged(false);

        message.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(12, searchCard, message, resultLabel, table);
        content.setFillWidth(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        setCenter(content);

        showInitialPlaceholder();
        searchField.requestFocus();
    }

    /** Runs the search and fills the table with matching records. */
    private void doSearch() {
        message.clear();
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();

        if (keyword.isEmpty()) {
            table.setItems(FXCollections.observableArrayList());
            resultLabel.setVisible(false);
            resultLabel.setManaged(false);
            showInitialPlaceholder();
            message.showWarning("Enter something to search",
                    "Type a visitor ID, a name or a phone number, then press Search.");
            searchField.requestFocus();
            return;
        }

        try {
            ObservableList<Visitor> rows =
                    FXCollections.observableArrayList(service.search(keyword));
            table.setItems(rows);

            if (rows.isEmpty()) {
                resultLabel.setText("No results for \"" + keyword + "\"");
                Button register = Ui.button("Register this visitor",
                        FontAwesomeSolid.USER_PLUS, "btn-primary");
                register.setOnAction(e -> Main.App.go(Main.Page.REGISTER));

                table.setPlaceholder(Ui.emptyState(
                        FontAwesomeSolid.SEARCH,
                        "No matching visitors",
                        "Nothing matches \"" + keyword + "\". Try a shorter search term, "
                                + "or register this visitor.",
                        register));
            } else {
                resultLabel.setText(rows.size() == 1
                        ? "1 result for \"" + keyword + "\""
                        : rows.size() + " results for \"" + keyword + "\"");
                table.setPlaceholder(new Label("No visitors to show."));
            }

            resultLabel.setVisible(true);
            resultLabel.setManaged(true);

        } catch (IllegalArgumentException e) {
            message.showWarning("Search needs a value", e.getMessage());
            searchField.requestFocus();
        } catch (Exception e) {
            message.showError("Search failed", friendly(e));
        }
    }

    /** Clears the search box, the results and the feedback. */
    private void resetSearch() {
        searchField.clear();
        table.setItems(FXCollections.observableArrayList());
        resultLabel.setText("");
        resultLabel.setVisible(false);
        resultLabel.setManaged(false);
        message.clear();
        showInitialPlaceholder();
        searchField.requestFocus();
    }

    private void showInitialPlaceholder() {
        table.setPlaceholder(Ui.emptyState(
                FontAwesomeSolid.SEARCH,
                "Search the visitor register",
                "Results appear here. You can search by visitor ID (for example 12), "
                        + "by name, or by phone number."));
    }

    private String friendly(Exception e) {
        String text = e.getMessage();
        return (text == null || text.trim().isEmpty())
                ? "An unexpected problem occurred."
                : text;
    }
}
