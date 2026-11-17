package ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;

/**
 * StatCard.java
 *
 * One summary card on the dashboard: a muted caption, a large number and a
 * soft icon tile. The value is always supplied by the service layer - this
 * component never invents data.
 */
public class StatCard extends VBox {

    /** Colour variants used by the icon tile. */
    public enum Tone {
        ACCENT("stat-icon-accent", "icon-accent"),
        SUCCESS("stat-icon-success", "icon-success"),
        WARNING("stat-icon-warning", "icon-warning"),
        NEUTRAL("stat-icon-neutral", "icon-neutral");

        private final String tileStyleClass;
        private final String iconStyleClass;

        Tone(String tileStyleClass, String iconStyleClass) {
            this.tileStyleClass = tileStyleClass;
            this.iconStyleClass = iconStyleClass;
        }
    }

    private final Label valueLabel = new Label("0");

    public StatCard(String caption, Ikon ikon, Tone tone) {
        super(12);
        getStyleClass().add("stat-card");
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-label");
        captionLabel.setWrapText(true);

        StackPane iconTile = new StackPane(Ui.icon(ikon, 16, tone.iconStyleClass));
        iconTile.getStyleClass().addAll("stat-icon", tone.tileStyleClass);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(10, captionLabel, spacer, iconTile);
        topRow.setAlignment(Pos.CENTER_LEFT);

        valueLabel.getStyleClass().add("stat-value");

        getChildren().addAll(topRow, valueLabel);
    }

    /** Shows the number. Pass a negative/unknown value only when truly unavailable. */
    public void setValue(int value) {
        valueLabel.setText(String.valueOf(Math.max(0, value)));
    }

    /** Shows a placeholder while the real value is not available yet. */
    public void setUnavailable() {
        valueLabel.setText("—");
    }
}
