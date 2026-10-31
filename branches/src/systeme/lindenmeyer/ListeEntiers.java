package systeme.lindenmeyer;

import java.util.Arrays;

/**
 * Une liste d'entiers sans boîtes (pour la provenance des règles et les débuts de paramètres).
 * Une liste peut être abandonnée (la provenance d'une génération trop longue n'est pas conservée).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class ListeEntiers {
    private int[] valeurs = new int[64];
    private int taille;
    private boolean abandonnee;

    /** Abandonne la liste : elle est vidée et ne reçoit plus rien. */
    void abandonne() {
        abandonnee = true;
        valeurs = new int[0];
        taille = 0;
    }

    /**
     * Retourne true si la liste a été abandonnée.
     * @return true si la liste a été abandonnée
     */
    boolean estAbandonnee() {
        return abandonnee;
    }

    void ajoute(int valeur) {
        if (abandonnee)
            return;
        if (taille == valeurs.length)
            valeurs = Arrays.copyOf(valeurs, 2 * taille);
        valeurs[taille++] = valeur;
    }

    int taille() {
        return taille;
    }

    int[] tableau() {
        return Arrays.copyOf(valeurs, taille);
    }
}
