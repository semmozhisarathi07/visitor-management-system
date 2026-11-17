package ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * InlineMessage.java
 *
 * A feedback banner shown inside a page (instead of yet another pop-up):
 * green for success, red for errors, amber for things to check, blue for
 * neutral information. It stays hidden until a message is set.
 */
public class InlineMessage extends HBox {

    public enum Type {
        SUCCESS, ERROR, WARNING, INFO
    }

    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();
    private final StackPane iconHolder = new StackPane(new Label());

    public InlineMessage() {
        super(10);
        getStyleClass().add("banner");
        setAlignment(Pos.TOP_LEFT);
        setVisible(false);
        setManaged(false);

        titleLabel.getStyleClass().add("banner-title");
        messageLabel.getStyleClass().add("banner-message");
        messageLabel.setWrapText(true);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        VBox texts = new VBox(2, titleLabel, messageLabel);
        texts.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(iconHolder, texts);
        clear();
    }

    public void showSuccess(String title, String message) {
        apply(Type.SUCCESS, title, message);
    }

    public void showError(String title, String message) {
        apply(Type.ERROR, title, message);
    }

    public void showWarning(String title, String message) {
        apply(Type.WARNING, title, message);
    }

    public void showInfo(String title, String message) {
        apply(Type.INFO, title, message);
    }

    /** Hides the banner. */
    public void clear() {
        setVisible(false);
        setManaged(false);
        titleLabel.setText("");
        messageLabel.setText("");
    }

    private void apply(Type type, String title, String message) {
        Ikon ikon;
        String bannerClass;
        String iconClass;

        switch (type) {
            case SUCCESS:
                ikon = FontAwesomeSolid.CHECK_CIRCLE;
                bannerClass = "banner-success";
                iconClass = "icon-success";
                break;
            case ERROR:
                ikon = FontAwesomeSolid.TIMES_CIRCLE;
                bannerClass = "banner-error";
                iconClass = "icon-danger";
                break;
            case WARNING:
                ikon = FontAwesomeSolid.EXCLAMATION_TRIANGLE;
                bannerClass = "banner-warning";
                iconClass = "icon-warning";
                break;
            case INFO:
            default:
                ikon = FontAwesomeSolid.INFO_CIRCLE;
                bannerClass = "banner-info";
                iconClass = "icon-accent";
                break;
        }

        getStyleClass().removeAll("banner-success", "banner-error", "banner-warning", "banner-info");
        getStyleClass().add(bannerClass);

        iconHolder.getChildren().setAll(Ui.icon(ikon, 15, iconClass));

        titleLabel.setText(title == null ? "" : title);
        titleLabel.setManaged(title != null && !title.isEmpty());
        titleLabel.setVisible(title != null && !title.isEmpty());

        messageLabel.setText(message == null ? "" : message);
        messageLabel.setManaged(message != null && !message.isEmpty());
        messageLabel.setVisible(message != null && !message.isEmpty());

        setVisible(true);
        setManaged(true);
    }
}
