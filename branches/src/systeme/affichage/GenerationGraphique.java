package systeme.affichage;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systeme.lindenmeyer.Generation;

/**
 * Un système de Lindenmayer qui sait aussi se dessiner sur un canvas JavaFX.
 * Ces méthodes étaient dans {@link Generation} ; elles en ont été séparées pour que le moteur
 * ({@code systeme.lindenmeyer}) n'importe plus JavaFX. Voir {@link SystemeGraphique}.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public interface GenerationGraphique extends Generation {
    /**
     * Génère puis dessine le système de Lindenmayer sur le canvas.
     *
     * @param nbIterations le nombre d'itérations
     * @param angle ignoré : l'angle utilisé est celui du système ({@link #getAngle()}, en degrés)
     * @param longueur la longueur d'un segment
     * @param couleur la couleur du dessin
     * @param canvas le canvas sur lequel on veut dessiner
     */
    public void generationSysteme(int nbIterations, double angle, int longueur, Color couleur, Canvas canvas);

    /**
     * Dessine la chaîne donnée : efface le canvas, puis trace la figure centrée,
     * réduite si nécessaire pour qu'elle tienne entièrement dans le canvas.
     * @param chaine la chaîne qui contient les instructions pour dessiner la figure
     * @param longueur la longueur d'un segment
     * @param couleur la couleur à utiliser pour dessiner la figure
     * @param canvas le canvas sur lequel dessiner la figure
     * @param gc le contexte graphique associé au canvas
     * @throws IllegalArgumentException si la chaîne contient un crochet fermant sans crochet ouvrant
     */
    public void dessineSysteme(String chaine, int longueur, Color couleur, Canvas canvas, GraphicsContext gc);
}
