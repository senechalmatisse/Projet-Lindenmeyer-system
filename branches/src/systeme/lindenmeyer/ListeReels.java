package systeme.lindenmeyer;

import java.util.Arrays;

/**
 * Une liste de nombres réels sans boîtes (paramètres des modules en construction).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class ListeReels {
    private double[] valeurs = new double[64];
    private int taille;

    void ajoute(double valeur) {
        if (taille == valeurs.length)
            valeurs = Arrays.copyOf(valeurs, 2 * taille);
        valeurs[taille++] = valeur;
    }

    int taille() {
        return taille;
    }

    double[] tableau() {
        return Arrays.copyOf(valeurs, taille);
    }
}
