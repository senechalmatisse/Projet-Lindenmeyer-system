package systeme.affichage;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import systeme.lindenmeyer.Regle;
import systeme.lindenmeyer.RegleParametrique;

/**
 * Le tableau « Règles comprises » : la façon dont le programme a compris chaque règle saisie
 * (symbole, contextes, poids, remplacement). Il est mis à jour à chaque frappe.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class TableRegles extends TableView<String[]> {
    /** Les colonnes affichées pour les règles des types déterministe, stochastique et contextuel. */
    static final String[] COLONNES_CLASSIQUES = {"Symbole", "Contexte gauche", "Contexte droit", "Poids", "Remplacement"};
    /** Les colonnes affichées pour les règles paramétriques. */
    static final String[] COLONNES_PARAMETRIQUES = {"Prédécesseur", "Condition", "Poids", "Successeur"};

    private String[] entetes = new String[0];

    TableRegles() {
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        setPrefHeight(170);
        setPlaceholder(new Label("Aucune règle"));
        colonnes(COLONNES_CLASSIQUES);
    }

    /**
     * Change les colonnes du tableau (si elles sont différentes).
     * @param noms les titres des colonnes
     */
    void colonnes(String[] noms) {
        if (java.util.Arrays.equals(noms, entetes))
            return;
        entetes = noms.clone();
        getColumns().clear();
        for (int i = 0; i < noms.length; i++) {
            final int indice = i;
            TableColumn<String[], String> colonne = new TableColumn<>(noms[i]);
            colonne.setSortable(false);
            colonne.setCellValueFactory(ligne -> new ReadOnlyStringWrapper(
                indice < ligne.getValue().length ? ligne.getValue()[indice] : ""));
            getColumns().add(colonne);
        }
    }

    /**
     * Affiche les règles (types classiques) telles que le programme les comprend.
     * @param texte le contenu du champ « Règle(s) »
     */
    void afficheReglesClassiques(String texte) {
        colonnes(COLONNES_CLASSIQUES);
        try {
            List<String[]> lignes = new ArrayList<>();
            for (Regle regle : Regle.analyse(texte))
                lignes.add(new String[] {
                    String.valueOf(regle.symbole()),
                    regle.gauche() == Regle.AUCUN ? "" : String.valueOf(regle.gauche()),
                    regle.droite() == Regle.AUCUN ? "" : String.valueOf(regle.droite()),
                    regle.poidsExplicite() ? Formats.nombre(regle.poids()) : "1",
                    regle.remplacement().isEmpty() ? "(vide)" : regle.remplacement()});
            affiche(lignes, null);
        } catch (IllegalArgumentException e) {
            affiche(List.of(), e.getMessage());
        }
    }

    /**
     * Affiche les règles paramétriques telles que le programme les comprend.
     * @param texte le contenu du champ « Règle(s) »
     */
    void afficheReglesParametriques(String texte) {
        colonnes(COLONNES_PARAMETRIQUES);
        try {
            List<String[]> lignes = new ArrayList<>();
            for (RegleParametrique regle : RegleParametrique.analyse(texte))
                lignes.add(new String[] {regle.predecesseur(), regle.condition(),
                    regle.poids().isEmpty() ? "1" : regle.poids(), regle.successeur().isEmpty() ? "(vide)" : regle.successeur()});
            affiche(lignes, null);
        } catch (IllegalArgumentException e) {
            affiche(List.of(), e.getMessage());
        }
    }

    /**
     * Remplit le tableau.
     * @param lignes les lignes (une cellule par colonne)
     * @param erreur un message à afficher à la place des lignes, ou null
     */
    void affiche(List<String[]> lignes, String erreur) {
        getItems().setAll(lignes);
        Label vide = new Label(erreur == null ? "Aucune règle" : erreur);
        vide.setWrapText(true);
        if (erreur != null)
            vide.setStyle("-fx-text-fill: #c00000;");
        setPlaceholder(vide);
    }
}
