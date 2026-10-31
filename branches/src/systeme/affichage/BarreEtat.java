package systeme.affichage;

import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * La barre d'état, en bas de la fenêtre : un résumé de la figure affichée
 * (« Génération 4/4 | 596 symboles | 256 segments | 3 ms »), les avertissements en orange
 * et un message d'information.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class BarreEtat extends HBox {
    private final Label resume = new Label();
    private final Label avertissements = new Label();
    private final Label message = new Label();

    BarreEtat() {
        super(18);
        setPadding(new Insets(4, 10, 4, 10));
        setAlignment(Pos.CENTER_LEFT);
        setMinHeight(26);
        setStyle("-fx-background-color: #ececec; -fx-border-color: #c8c8c8; -fx-border-width: 1 0 0 0;");
        resume.setMinWidth(Label.USE_PREF_SIZE);
        avertissements.setStyle("-fx-text-fill: #b35900;");
        avertissements.setMinWidth(0);
        message.setStyle("-fx-text-fill: #505050;");
        message.setMinWidth(0);
        HBox.setHgrow(avertissements, Priority.SOMETIMES);
        HBox.setHgrow(message, Priority.ALWAYS);
        getChildren().addAll(resume, avertissements, message);
    }

    /**
     * Affiche le résumé de la figure.
     * @param texte le résumé (vide pour l'effacer)
     */
    void resume(String texte) {
        resume.setText(texte);
    }

    /**
     * Affiche les avertissements (en orange), séparés par un point médian ; l'infobulle les liste tous.
     * @param liste les avertissements (vide pour les effacer)
     */
    void avertissements(List<String> liste) {
        avertissements.setText(String.join("  ·  ", liste));
        avertissements.setTooltip(liste.isEmpty() ? null : new Tooltip(String.join("\n", liste)));
    }

    /**
     * Affiche un message d'information (remplace le précédent).
     * @param texte le message (vide pour l'effacer)
     */
    void message(String texte) {
        message.setText(texte);
        message.setTooltip(texte == null || texte.isEmpty() ? null : new Tooltip(texte));
    }

    /**
     * Retourne le message affiché.
     * @return le message affiché
     */
    String message() {
        return message.getText();
    }

    /**
     * Retourne le résumé affiché.
     * @return le résumé affiché
     */
    String resume() {
        return resume.getText();
    }

    /**
     * Retourne les avertissements affichés.
     * @return les avertissements affichés
     */
    String avertissements() {
        return avertissements.getText();
    }

    /** Efface tout le contenu de la barre. */
    void efface() {
        resume("");
        avertissements(List.of());
        message("");
    }
}
