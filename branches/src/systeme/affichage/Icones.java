package systeme.affichage;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Petites icônes vectorielles pour les boutons (elles s'affichent de la même façon sur tous les
 * systèmes, contrairement aux caractères ◀ ▶ ⏸ qui manquent dans certaines polices).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class Icones {
    private Icones() {
    }

    private static Node icone(String chemin) {
        SVGPath forme = new SVGPath();
        forme.setContent(chemin);
        forme.setFill(Color.web("#333333"));
        return forme;
    }

    /**
     * Retourne un triangle pointant à gauche (génération précédente).
     * @return un triangle pointant à gauche (génération précédente)
     */
    static Node precedent() {
        return icone("M9,0 L9,12 L0,6 Z");
    }

    /**
     * Retourne un triangle pointant à droite (génération suivante, lecture).
     * @return un triangle pointant à droite (génération suivante, lecture)
     */
    static Node suivant() {
        return icone("M0,0 L9,6 L0,12 Z");
    }

    /**
     * Retourne deux barres verticales (pause).
     * @return deux barres verticales (pause)
     */
    static Node pause() {
        return icone("M0,0 H3.5 V12 H0 Z M6.5,0 H10 V12 H6.5 Z");
    }

    /**
     * Retourne une barre et un triangle pointant à gauche (retour au début).
     * @return une barre et un triangle pointant à gauche (retour au début)
     */
    static Node debut() {
        return icone("M0,0 H2 V12 H0 Z M11,0 L11,12 L2,6 Z");
    }
}
