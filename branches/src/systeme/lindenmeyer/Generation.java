package systeme.lindenmeyer;

/**
 * Cette interface définit les méthodes à utiliser pour générer un système de Lindenmeyer.
 * <p>
 * Elle ne dépend pas de JavaFX : le dessin est décrit par {@code systeme.affichage.GenerationGraphique},
 * qui l'étend, et réalisé dans le paquetage {@code systeme.affichage}.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public interface Generation {
    /**
     * Applique le moteur de réécriture à l'axiome du système, sans dessiner.
     *
     * @param nbIterations le nombre de réécritures à appliquer
     * @return la chaîne obtenue après nbIterations réécritures
     * @throws IllegalArgumentException si la chaîne devient trop longue pour être dessinée
     */
    public String genereChaine(int nbIterations);

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
