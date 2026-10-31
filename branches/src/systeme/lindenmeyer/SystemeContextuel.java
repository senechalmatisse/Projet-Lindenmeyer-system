package systeme.lindenmeyer;

/**
 * Cette classe représente un système de Lindenmayer contextuel.
 * Une règle contextuelle {@code G<S>D=remplacement} ne réécrit le symbole S que s'il est précédé de G
 * et/ou suivi de D (chaque contexte est facultatif). Le contexte d'un symbole est son voisin immédiat :
 * les branches {@code [...]} sont sautées, et pour un symbole en début de branche le contexte gauche
 * est le symbole qui précède la branche. Quand plusieurs règles s'appliquent, celle qui a le plus de
 * contextes l'emporte (à égalité, la première écrite).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public class SystemeContextuel extends SystemeLindenmeyer implements Generation {
    /**
     * Construit une nouvelle instance d'un système de Lindenmayer contextuel.
     * @param axiome le (ou les) axiome(s) du système
     * @param regles la (ou les) règle(s) du système
     * @param angle l'angle (en degrés) pour tourner lors de l'affichage
     * @param iteration le nombre d'itérations
     * @param longueur la longueur du dessin
     * @throws IllegalArgumentException si une règle est mal formée ou pondérée
     */
    public SystemeContextuel(String axiome, String regles, double angle, int iteration, int longueur) {
        super(axiome, regles, angle, iteration, longueur);
        verifieRegles("contextuel", true, false, true);
    }

    /**
     * Un signal (S ou T) parcourt le tronc de bas en haut, une feuille F par itération :
     * en passant, il fait pousser une branche (à gauche pour S, à droite pour T) puis passe le relais à l'autre.
     * Aucun système sans contexte ne peut produire cette figure.
     */
    public SystemeContextuel() {
        this("SFFFFFFFFFF", "S= T= S<F=F[+FF]T T<F=F[-FF]S", 30, 10, 40);
    }
}
