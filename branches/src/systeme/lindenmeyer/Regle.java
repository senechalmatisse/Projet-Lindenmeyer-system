package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Une règle de production d'un système de Lindenmayer.
 * <p>
 * Format d'une règle (sans espace) : {@code [G<]S[>D][(poids)]=remplacement}
 * <ul>
 * <li>{@code S} : le symbole à réécrire ;</li>
 * <li>{@code G<} et {@code >D} : contextes gauche et droit facultatifs (un seul symbole chacun) ;</li>
 * <li>{@code (poids)} : poids relatif facultatif, utilisé par les systèmes stochastiques (1 par défaut) ;</li>
 * <li>{@code remplacement} : la chaîne qui remplace {@code S} (peut être vide).</li>
 * </ul>
 * La classe est publique pour que l'interface puisse afficher les règles comprises (tableau
 * « Règles comprises ») et valider la saisie pendant la frappe.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Regle {
    /** Valeur d'un contexte absent. */
    public static final char AUCUN = '\0';

    private static final Pattern FORMAT = Pattern.compile(
        "^(?:([^\\s=<>()])<)?([^\\s=<>()])(?:>([^\\s=<>()]))?(?:\\(([^)]*)\\))?$");

    private final String texte;
    private final char gauche, symbole, droite;
    private final double poids;
    private final boolean poidsExplicite;
    private final String remplacement;

    /**
     * Analyse une règle.
     * @param jeton la règle, sans espace
     * @throws IllegalArgumentException si la règle est mal formée
     */
    private Regle(String jeton) {
        int egal = jeton.indexOf('=');
        if (egal < 0)
            throw erreur(jeton, "le signe '=' est manquant");
        Matcher m = FORMAT.matcher(jeton.substring(0, egal));
        if (!m.matches())
            throw erreur(jeton, "la partie gauche doit avoir la forme [G<]S[>D][(poids)]");
        this.texte = jeton;
        this.gauche = m.group(1) == null ? AUCUN : m.group(1).charAt(0);
        this.symbole = m.group(2).charAt(0);
        this.droite = m.group(3) == null ? AUCUN : m.group(3).charAt(0);
        this.poidsExplicite = m.group(4) != null;
        double p = 1;
        if (poidsExplicite) {
            try {
                p = Double.parseDouble(m.group(4).trim());
            } catch (NumberFormatException e) {
                throw erreur(jeton, "le poids « " + m.group(4) + " » n'est pas un nombre");
            }
            if (!(p > 0) || Double.isInfinite(p))
                throw erreur(jeton, "le poids doit être un nombre strictement positif");
        }
        this.poids = p;
        this.remplacement = jeton.substring(egal + 1);
    }

    /**
     * Analyse une chaîne contenant des règles séparées par des espaces.
     * @param texte les règles (peut être vide ou null : aucune règle)
     * @return la liste des règles, dans l'ordre de la chaîne
     * @throws IllegalArgumentException si une règle est mal formée
     */
    public static List<Regle> analyse(String texte) {
        List<Regle> regles = new ArrayList<>();
        if (texte == null)
            return regles;
        for (String jeton : texte.trim().split("\\s+"))
            if (!jeton.isEmpty())
                regles.add(new Regle(jeton));
        return regles;
    }

    private static IllegalArgumentException erreur(String jeton, String raison) {
        return new IllegalArgumentException("Règle « " + jeton + " » invalide : " + raison + ".");
    }

    /**
     * Retourne le symbole réécrit par cette règle.
     * @return le symbole réécrit par cette règle
     */
    public char symbole() { return symbole; }
    /**
     * Retourne le contexte gauche, ou {@link #AUCUN}.
     * @return le contexte gauche, ou {@link #AUCUN}
     */
    public char gauche() { return gauche; }
    /**
     * Retourne le contexte droit, ou {@link #AUCUN}.
     * @return le contexte droit, ou {@link #AUCUN}
     */
    public char droite() { return droite; }
    /**
     * Retourne le poids relatif de la règle (1 par défaut).
     * @return le poids relatif de la règle (1 par défaut)
     */
    public double poids() { return poids; }
    /**
     * Retourne true si le poids a été écrit dans la règle.
     * @return true si le poids a été écrit dans la règle
     */
    public boolean poidsExplicite() { return poidsExplicite; }
    /**
     * Retourne la chaîne de remplacement.
     * @return la chaîne de remplacement
     */
    public String remplacement() { return remplacement; }
    /**
     * Retourne le nombre de contextes (0, 1 ou 2) : plus il est grand, plus la règle est prioritaire.
     * @return le nombre de contextes (0, 1 ou 2) : plus il est grand, plus la règle est prioritaire
     */
    public int specificite() { return (gauche != AUCUN ? 1 : 0) + (droite != AUCUN ? 1 : 0); }

    /**
     * Retourne la règle telle qu'elle a été écrite.
     * @return la règle telle qu'elle a été écrite
     */
    @Override
    public String toString() {
        return texte;
    }
}
