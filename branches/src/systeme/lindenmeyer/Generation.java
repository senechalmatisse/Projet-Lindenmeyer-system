package systeme.lindenmeyer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Cette interface définit les méthodes à utiliser pour générer un système de Lindenmeyer.
 * 
 * @author Ali Azou, Matisse Senechal, Rafik Halit, Rayane Farhi, Université de Caen Normandie, France 
 */
public interface Generation {
    /**
     * Génère et dessine le système de Lindenmeyer sur le canvas avec les paramètres donnés.
     *
     * @param iterations le nombre d'itérations
     * @param angle l'angle (en degrés) pour tourner lors de l'affichage
     * @param longueur la longueur du dessin
     * @param couleur la couleur du dessin
     * @param canvas le canvas sur lequel on veut dessiner
     */
    public void generationSysteme(int nbIterations, double angle, int longueur, Color couleur, Canvas canvas);
    
    /**
     * Dessine un système de L-système à partir de la chaîne de caractères représentant l'axiome initial.
     * @param chaine la chaîne qui contient les instructions pour dessiner l'arbre
     * @param longueur la longueur du dessin
     * @param couleur la couleur à utiliser pour dessiner l'arbre
     * @param canvas le canvas sur lequel dessiner l'arbre
     * @param gc le contexte graphique associé au canvas
     */
    public void dessineSysteme(String chaine, int longueur, Color couleur, Canvas canvas, GraphicsContext gc);
    /**
     * Retourne l'angle utilisé par le L-Système.
     * @return l'angle utilisé par le L-Système
     */
    public double getAngle();
    /**
     * Retourne le nombre d'itérations pour le L-Système.
     * @return le nombre d'itérations pour le L-Système
     */
    public int getIteration();
    /**
     * Retourne la longueur du dessin.
     * @return la longueur du dessin
     */
    public int getLongueur();
}