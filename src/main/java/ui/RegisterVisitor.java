package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
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
 * RegisterVisitor.java
 *
 * The registration form. The visitor types the details; visitor ID, visit date,
 * in time and status (INSIDE) are still filled in automatically by the service
 * layer exactly as before.
 *
 * Improvements over the previous version:
 *   - labelled fields with clear, specific inline error messages
 *   - required-field markers and length limits that match the database columns
 *   - an in-page success message showing the generated visitor ID
 *   - the submit button is disabled while saving, so a double click is harmless
 */
public class RegisterVisitor extends BorderPane {

    // These limits match the column sizes in database.sql.
    private static final int MAX_NAME = 100;
    private static final int MAX_PHONE = 20;
    private static final int MAX_PERSON = 100;
    private static final int MAX_REASON = 200;

    private final VisitorService service = new VisitorService();

    private final TextField nameField = styledField("e.g. Ravi Kumar", MAX_NAME);
    private final TextField phoneField = styledField("e.g. 9876543210", MAX_PHONE);
    private final TextField meetField = styledField("e.g. Mrs. Sharma (HR)", MAX_PERSON);
    private final TextField reasonField = styledField("e.g. Document submission", MAX_REASON);

    private final Label nameError = errorLabel();
    private final Label phoneError = errorLabel();
    private final Label meetError = errorLabel();
    private final Label reasonError = errorLabel();

    private final InlineMessage message = new InlineMessage();

    private final Button saveButton =
            Ui.button("Register Visitor", FontAwesomeSolid.USER_CHECK, "btn-primary");
    private final Button clearButton =
            Ui.button("Clear Form", FontAwesomeSolid.TIMES, "btn-secondary");

    public RegisterVisitor() {
        setTop(Ui.pageHeader(
                "Register Visitor",
                "Add a visitor to the register. The visitor ID, visit date and check-in time are created automatically.",
                Ui.dateChip()));

        ScrollPane scroll = new ScrollPane(buildForm());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scroll);

        nameField.requestFocus();
    }

    // =========================================================
    // Layout
    // =========================================================

    private VBox buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(16);

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);

        grid.add(fieldBlock("Visitor name", true, nameField, nameError,
                "Full name of the person visiting."), 0, 0);
        grid.add(fieldBlock("Phone number", true, phoneField, phoneError,
                "7 to 15 digits. Spaces, dashes and + are allowed."), 1, 0);
        grid.add(fieldBlock("Person to meet", true, meetField, meetError,
                "Who is the visitor here to see?"), 0, 1);
        grid.add(fieldBlock("Reason for visit", false, reasonField, reasonError,
                "Optional. Up to 200 characters."), 1, 1);

        saveButton.setDefaultButton(true);
        saveButton.setOnAction(e -> saveVisitor());
        clearButton.setOnAction(e -> clearForm());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label autoHint = Ui.paragraph(
                "Visit date, check-in time and status are set by the system.",
                "field-hint");

        HBox buttons = new HBox(10, autoHint, spacer, clearButton, saveButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox card = Ui.card(18,
                Ui.sectionHeader("Visitor details", "Fields marked with * are required.", null),
                Ui.divider(),
                grid,
                Ui.divider(),
                buttons);
        card.setMaxWidth(840);

        VBox content = new VBox(16, message, card);
        content.setFillWidth(true);
        content.setPadding(new Insets(0, 0, 8, 0));

        // Re-validate a field as soon as the user starts fixing it.
        clearErrorOnEdit(nameField, nameError);
        clearErrorOnEdit(phoneField, phoneError);
        clearErrorOnEdit(meetField, meetError);
        clearErrorOnEdit(reasonField, reasonError);

        return content;
    }

    private VBox fieldBlock(String labelText, boolean required, TextField field,
                            Label error, String hint) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        label.setLabelFor(field);

        HBox labelRow = new HBox(3, label);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        if (required) {
            Label mark = new Label("*");
            mark.getStyleClass().add("required-mark");
            labelRow.getChildren().add(mark);
        }

        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        VBox block = new VBox(6, labelRow, field, error,
                Ui.paragraph(hint, "field-hint"));
        block.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(block, Priority.ALWAYS);

        field.setAccessibleText(labelText);
        return block;
    }

    // =========================================================
    // Validation + saving
    // =========================================================

    /**
     * Validates every field and highlights the problems in place.
     *
     * @return true when the form can be submitted.
     */
    private boolean validateForm() {
        clearFieldErrors();

        boolean valid = true;
        String name = textOf(nameField);
        String phone = textOf(phoneField);
        String person = textOf(meetField);
        String reason = textOf(reasonField);

        if (name.isEmpty()) {
            showError(nameField, nameError, "Enter the visitor's full name.");
            valid = false;
        } else if (name.length() > MAX_NAME) {
            showError(nameField, nameError, "Use 100 characters or fewer.");
            valid = false;
        }

        String digits = phone.replace(" ", "").replace("-", "");
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        if (phone.isEmpty()) {
            showError(phoneField, phoneError, "Enter the visitor's phone number.");
            valid = false;
        } else if (!digits.matches("[0-9]{7,15}")) {
            showError(phoneField, phoneError,
                    "Use 7 to 15 digits, for example 9876543210.");
            valid = false;
        }

        if (person.isEmpty()) {
            showError(meetField, meetError, "Enter who the visitor is meeting.");
            valid = false;
        } else if (person.length() > MAX_PERSON) {
            showError(meetField, meetError, "Use 100 characters or fewer.");
            valid = false;
        }

        if (reason.length() > MAX_REASON) {
            showError(reasonField, reasonError, "Use 200 characters or fewer.");
            valid = false;
        }

        if (!valid) {
            message.showWarning("Check the highlighted fields",
                    "Some details are missing or in the wrong format. Fix them and register again.");
            focusFirstInvalid();
        }
        return valid;
    }

    private void focusFirstInvalid() {
        for (TextField field : new TextField[] {nameField, phoneField, meetField, reasonField}) {
            if (field.getStyleClass().contains("field-invalid")) {
                field.requestFocus();
                return;
            }
        }
    }

    private void saveVisitor() {
        if (!validateForm()) {
            return;
        }

        saveButton.setDisable(true);
        try {
            Visitor saved = service.registerVisitor(
                    nameField.getText(),
                    phoneField.getText(),
                    meetField.getText(),
                    reasonField.getText());

            message.showSuccess("Visitor registered — ID " + saved.getVisitorId(),
                    saved.getVisitorName() + " checked in at "
                            + VisitorList.formatTime(saved.getInTime()) + " on "
                            + VisitorList.formatDate(saved.getVisitDate())
                            + " to meet " + saved.getPersonToMeet()
                            + ". Status: " + saved.getStatus() + ".");

            clearForm();

        } catch (IllegalArgumentException e) {
            // The service layer rejected a value (normally caught above).
            message.showWarning("Check the form", e.getMessage());
        } catch (Exception e) {
            message.showError("Could not register the visitor", friendly(e));
        } finally {
            saveButton.setDisable(false);
        }
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        meetField.clear();
        reasonField.clear();
        clearFieldErrors();
        nameField.requestFocus();
    }

    private void clearFieldErrors() {
        for (TextField field : new TextField[] {nameField, phoneField, meetField, reasonField}) {
            field.getStyleClass().remove("field-invalid");
        }
        for (Label error : new Label[] {nameError, phoneError, meetError, reasonError}) {
            error.setText("");
            error.setVisible(false);
        }
    }

    private void showError(TextField field, Label error, String text) {
        if (!field.getStyleClass().contains("field-invalid")) {
            field.getStyleClass().add("field-invalid");
        }
        error.setText(text);
        error.setVisible(true);
    }

    private void clearErrorOnEdit(TextField field, Label error) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            field.getStyleClass().remove("field-invalid");
            error.setVisible(false);
        });
    }

    // =========================================================
    // Small helpers
    // =========================================================

    private static String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static TextField styledField(String prompt, int maxLength) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(40);
        // Never allow more characters than the database column accepts.
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
        return field;
    }

    private static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("field-error");
        label.setWrapText(true);
        return label;
    }

    private String friendly(Exception e) {
        String text = e.getMessage();
        return (text == null || text.trim().isEmpty())
                ? "An unexpected problem occurred. Please try again."
                : text;
    }
}
