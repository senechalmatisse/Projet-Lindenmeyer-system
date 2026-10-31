package systeme.lindenmeyer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

/**
 * Une génération d'un système : une suite de modules. Un module est un symbole, éventuellement suivi de
 * paramètres numériques (systèmes paramétriques : {@code F(1.5)}, {@code +(30)}).
 * <p>
 * Les symboles forment une chaîne ordinaire ({@link #symboles()}) : le module i est le caractère i, ce qui permet
 * de compter les {@code F}, de vérifier les crochets ou de relier un segment à son symbole sans se soucier des
 * paramètres. Le texte affiché ({@link #texte()}) écrit les paramètres entre parenthèses.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Mot {
    private static final MathContext CHIFFRES = new MathContext(6);

    private final String symboles;
    /** Paramètres du module i : valeurs[debuts[i] .. debuts[i + 1]) ; null si aucun module n'a de paramètre. */
    private final int[] debuts;
    private final double[] valeurs;
    private String texte;

    /**
     * Un mot sans paramètres (systèmes déterministes, stochastiques et contextuels).
     * @param symboles les symboles
     */
    public Mot(String symboles) {
        this.symboles = symboles;
        this.debuts = null;
        this.valeurs = null;
        this.texte = symboles;
    }

    /**
     * Un mot avec paramètres.
     * @param symboles les symboles
     * @param debuts le début des paramètres de chaque module dans {@code valeurs} (taille + 1 éléments)
     * @param valeurs les paramètres de tous les modules, à la suite
     */
    Mot(String symboles, int[] debuts, double[] valeurs) {
        this.symboles = symboles;
        this.debuts = debuts;
        this.valeurs = valeurs;
    }

    /**
     * Retourne le nombre de modules.
     * @return le nombre de modules
     */
    public int taille() {
        return symboles.length();
    }

    /**
     * Retourne le symbole du module i.
     * @param i le numéro du module
     * @return le symbole du module i
     */
    public char symbole(int i) {
        return symboles.charAt(i);
    }

    /**
     * Retourne les symboles, sans les paramètres (un caractère par module).
     * @return les symboles, sans les paramètres (un caractère par module)
     */
    public String symboles() {
        return symboles;
    }

    /**
     * Retourne true si au moins un module peut avoir des paramètres.
     * @return true si au moins un module peut avoir des paramètres
     */
    public boolean aDesParametres() {
        return debuts != null;
    }

    /**
     * Retourne le nombre de paramètres du module i.
     * @param i le numéro du module
     * @return le nombre de paramètres du module i
     */
    public int nbParametres(int i) {
        return debuts == null ? 0 : debuts[i + 1] - debuts[i];
    }

    /**
     * Retourne le paramètre j du module i.
     * @param i le numéro du module
     * @param j le numéro du paramètre
     * @return le paramètre j du module i
     */
    public double parametre(int i, int j) {
        return valeurs[debuts[i] + j];
    }

    /**
     * Retourne une estimation de la place occupée, en « symboles » (pour borner la mémoire des dérivations).
     * @return une estimation de la place occupée, en « symboles » (pour borner la mémoire des dérivations)
     */
    long cout() {
        return debuts == null ? taille() : taille() + 3L * (debuts[taille()]) + taille() / 2;
    }

    /**
     * Retourne le texte du mot, paramètres compris : {@code F(1)[+(30)A(0.7)]}.
     * @return le texte du mot, paramètres compris : {@code F(1)[+(30)A(0.7)]}
     */
    public String texte() {
        if (texte == null)
            texte = texte(Integer.MAX_VALUE).texte;
        return texte;
    }

    /** Le début d'un texte, avec la position de chaque module dans ce texte. */
    public static final class Texte {
        /** Le texte (au plus le nombre de caractères demandé, sans couper un module). */
        public final String texte;
        /** Pour chaque module affiché, la position de son symbole dans le texte (croissante). */
        public final int[] debuts;
        /** Le nombre de modules affichés. */
        public final int nbModules;

        Texte(String texte, int[] debuts, int nbModules) {
            this.texte = texte;
            this.debuts = debuts;
            this.nbModules = nbModules;
        }

        /**
         * Retrouve le module d'un caractère.
         * @param position la position d'un caractère du texte
         * @return le numéro du module qui contient ce caractère
         */
        public int module(int position) {
            int i = Arrays.binarySearch(debuts, 0, nbModules, position);
            return i >= 0 ? i : -i - 2;
        }

        /**
         * Retourne la position du caractère qui suit ce module dans le texte.
         * @param module le numéro d'un module affiché
         * @return la position du caractère qui suit ce module dans le texte
         */
        public int fin(int module) {
            return module + 1 < nbModules ? debuts[module + 1] : texte.length();
        }
    }

    /**
     * Écrit le début du mot.
     * @param maxCaracteres le nombre maximal de caractères (les modules qui ne tiennent pas sont omis)
     * @return le texte et la position de chaque module affiché
     */
    public Texte texte(int maxCaracteres) {
        int n = taille();
        if (debuts == null) {
            int m = Math.min(n, maxCaracteres);
            int[] positions = new int[m];
            for (int i = 0; i < m; i++)
                positions[i] = i;
            return new Texte(m == n ? symboles : symboles.substring(0, m), positions, m);
        }
        StringBuilder resultat = new StringBuilder();
        int[] positions = new int[Math.min(n, Math.max(16, maxCaracteres))];
        int m = 0;
        StringBuilder module = new StringBuilder();
        for (int i = 0; i < n; i++) {
            module.setLength(0);
            module.append(symboles.charAt(i));
            int nb = nbParametres(i);
            if (nb > 0) {
                module.append('(');
                for (int j = 0; j < nb; j++) {
                    if (j > 0)
                        module.append(',');
                    module.append(nombre(parametre(i, j)));
                }
                module.append(')');
            }
            if ((long) resultat.length() + module.length() > maxCaracteres)
                break;
            if (m == positions.length)
                positions = Arrays.copyOf(positions, 2 * m);
            positions[m++] = resultat.length();
            resultat.append(module);
        }
        return new Texte(resultat.toString(), positions, m);
    }

    /**
     * Écrit un paramètre de façon compacte (indépendamment de la langue du système) : un entier sans décimale,
     * sinon au plus 6 chiffres significatifs, sans zéro inutile (0.7, 0.49, 0.333333).
     * @param x le nombre
     * @return le texte
     */
    public static String nombre(double x) {
        if (x == Math.rint(x) && Math.abs(x) < 1e15)
            return String.valueOf((long) x);
        if (!Double.isFinite(x))
            return String.valueOf(x);
        return new BigDecimal(x).round(CHIFFRES).stripTrailingZeros().toPlainString();
    }

    /**
     * Retourne le texte du mot.
     * @return le texte du mot
     */
    @Override
    public String toString() {
        return texte();
    }
}
