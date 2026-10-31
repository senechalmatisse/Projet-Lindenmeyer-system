package systeme.affichage;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Mise en forme des nombres affichés dans l'interface (pas dans les fichiers : ceux-ci utilisent
 * toujours {@code Locale.ROOT}).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class Formats {
    private Formats() {
    }

    /**
     * Formate un entier avec des séparateurs de milliers (« 2 155 »).
     * @param n l'entier
     * @return le texte
     */
    static String entier(long n) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(n);
    }

    /**
     * Formate un nombre sans zéros inutiles, avec un point décimal (comme dans les champs de saisie) :
     * 25 → « 25 », 25.7 → « 25.7 ».
     * @param x le nombre
     * @return le texte
     */
    static String nombre(double x) {
        if (x == Math.rint(x) && Math.abs(x) < 1e15)
            return String.valueOf((long) x);
        return String.valueOf(x);
    }

    /**
     * Formate une durée en millisecondes.
     * @param nanosecondes la durée en nanosecondes
     * @return le texte (« 3 ms »)
     */
    static String duree(long nanosecondes) {
        return entier(Math.round(nanosecondes / 1e6)) + " ms";
    }
}
