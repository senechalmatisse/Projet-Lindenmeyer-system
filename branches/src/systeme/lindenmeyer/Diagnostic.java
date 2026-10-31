package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Avertissements sur un système et la chaîne qu'il a produite : ils expliquent une figure vide ou
 * inattendue, sans jamais empêcher la génération (les erreurs bloquantes restent des exceptions).
 * <ul>
 * <li>W1 : la chaîne ne contient aucun {@code F} (rien à tracer) ;</li>
 * <li>W2 : un symbole de l'axiome ou d'un remplacement n'est ni dessiné, ni réécrit, ni utilisé comme contexte ;</li>
 * <li>W3 : une règle ne sera jamais appliquée (son symbole n'est jamais produit à partir de l'axiome) ;</li>
 * <li>W4 : un crochet ouvrant n'est jamais refermé ;</li>
 * <li>W5 : une accolade ouvrante n'est jamais refermée (le polygone est ignoré) ;</li>
 * <li>W6 : 0 itération alors qu'il y a des règles (seul l'axiome est dessiné).</li>
 * </ul>
 * Les contextes ne sont pas pris en compte pour W3 : pas de faux positif, mais une règle dont le
 * contexte n'est jamais satisfait n'est pas détectée.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Diagnostic {
    /** Les symboles interprétés par la tortue ; tous les autres ne servent qu'à la réécriture. */
    public static final String SYMBOLES_INTERPRETES = "Ff+-[]&^\\/|!'{}";
    /** Nombre maximal de symboles cités par l'avertissement W2. */
    private static final int MAX_SYMBOLES_CITES = 10;

    /** Une règle réduite à ce qui compte pour les avertissements (règles classiques ou paramétriques). */
    private static final class Resume {
        final char symbole, gauche, droite;
        final String remplacement, texte;

        Resume(char symbole, char gauche, char droite, String remplacement, String texte) {
            this.symbole = symbole;
            this.gauche = gauche;
            this.droite = droite;
            this.remplacement = remplacement;
            this.texte = texte;
        }
    }

    private Diagnostic() {
    }

    /**
     * Analyse un système (de n'importe quel type, paramétrique compris) et une de ses générations.
     * @param systeme le système
     * @param mot la génération dessinée
     * @param iterations le numéro de cette génération
     * @return les avertissements (liste vide si tout va bien)
     */
    public static List<String> analyse(SystemeLindenmeyer systeme, Mot mot, int iterations) {
        List<Resume> resumes = new ArrayList<>();
        if (systeme instanceof SystemeParametrique) {
            for (RegleParametrique regle : ((SystemeParametrique) systeme).getReglesParametriques())
                resumes.add(new Resume(regle.symbole(), Regle.AUCUN, Regle.AUCUN, regle.symbolesSuccesseur(), regle.toString()));
        } else {
            resumes = resumes(systeme.getReglesAnalysees());
        }
        return analyseResumes(systeme.motInitial().symboles(), resumes, mot.symboles(), iterations);
    }

    private static List<Resume> resumes(List<Regle> regles) {
        List<Resume> resumes = new ArrayList<>();
        for (Regle regle : regles)
            resumes.add(new Resume(regle.symbole(), regle.gauche(), regle.droite(), regle.remplacement(), regle.toString()));
        return resumes;
    }

    /**
     * Analyse un système et la chaîne affichée.
     * @param axiome l'axiome (les espaces sont ignorés)
     * @param regles les règles du système
     * @param chaineFinale la chaîne dessinée
     * @param iterations le nombre d'itérations qui a produit cette chaîne
     * @return les avertissements, dans l'ordre W1 à W6 (liste vide si tout va bien)
     */
    public static List<String> analyse(String axiome, List<Regle> regles, String chaineFinale, int iterations) {
        return analyseResumes(axiome == null ? "" : axiome.replaceAll("\\s+", ""), resumes(regles),
            chaineFinale == null ? "" : chaineFinale, iterations);
    }

    private static List<String> analyseResumes(String depart, List<Resume> regles, String chaine, int iterations) {
        List<String> avertissements = new ArrayList<>();

        // W1 : rien à tracer (ni segment, ni polygone).
        if (chaine.indexOf('F') < 0 && chaine.indexOf('{') < 0)
            avertissements.add("Rien à tracer : la chaîne générée ne contient aucun F.");

        // W2 : symboles sans effet (ni dessinés, ni réécrits, ni contexte d'une règle).
        Set<Character> utiles = new HashSet<>();
        for (Resume regle : regles) {
            utiles.add(regle.symbole);
            utiles.add(regle.gauche);
            utiles.add(regle.droite);
        }
        Set<Character> sansEffet = new LinkedHashSet<>();
        ajouteSansEffet(depart, utiles, sansEffet);
        for (Resume regle : regles)
            ajouteSansEffet(regle.remplacement, utiles, sansEffet);
        if (!sansEffet.isEmpty())
            avertissements.add(messageSansEffet(sansEffet));

        // W3 : règles jamais appliquées (symboles atteignables depuis l'axiome, jusqu'au point fixe).
        Set<Character> atteints = new HashSet<>();
        ajouteSymboles(depart, atteints);
        boolean change = true;
        while (change) {
            change = false;
            for (Resume regle : regles)
                if (atteints.contains(regle.symbole))
                    change |= ajouteSymboles(regle.remplacement, atteints);
        }
        for (Resume regle : regles)
            if (!atteints.contains(regle.symbole))
                avertissements.add("La règle " + regle.texte + " ne sera jamais appliquée : le symbole "
                    + regle.symbole + " n'apparaît jamais dans la chaîne.");

        // W4 : crochets ouvrants jamais refermés (un crochet fermant orphelin est une erreur bloquante).
        int ouverts = 0;
        for (int i = 0; i < chaine.length(); i++) {
            char c = chaine.charAt(i);
            if (c == '[')
                ouverts++;
            else if (c == ']' && ouverts > 0)
                ouverts--;
        }
        if (ouverts == 1)
            avertissements.add("Crochet ouvrant sans crochet fermant.");
        else if (ouverts > 1)
            avertissements.add(ouverts + " crochets ouvrants sans crochet fermant.");

        // W5 : polygone jamais fermé (une accolade fermante orpheline est une erreur bloquante).
        int dernierOuvrant = chaine.lastIndexOf('{');
        if (dernierOuvrant >= 0 && chaine.indexOf('}', dernierOuvrant) < 0)
            avertissements.add("Accolade ouvrante « { » sans accolade fermante : le dernier polygone est ignoré.");

        // W6 : aucune réécriture.
        if (iterations == 0 && !regles.isEmpty())
            avertissements.add("0 itération : seul l'axiome est dessiné.");
        return avertissements;
    }

    /**
     * Indique si un symbole est interprété par la tortue.
     * @param symbole le symbole
     * @return true si la tortue réagit à ce symbole
     */
    public static boolean estInterprete(char symbole) {
        return SYMBOLES_INTERPRETES.indexOf(symbole) >= 0;
    }

    private static void ajouteSansEffet(String texte, Set<Character> utiles, Set<Character> sansEffet) {
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (!Character.isWhitespace(c) && !estInterprete(c) && !utiles.contains(c))
                sansEffet.add(c);
        }
    }

    private static boolean ajouteSymboles(String texte, Set<Character> ensemble) {
        boolean ajout = false;
        for (int i = 0; i < texte.length(); i++)
            ajout |= ensemble.add(texte.charAt(i));
        return ajout;
    }

    private static String messageSansEffet(Set<Character> symboles) {
        StringBuilder liste = new StringBuilder();
        int n = 0;
        for (char c : symboles) {
            if (n == MAX_SYMBOLES_CITES) {
                liste.append(", …");
                break;
            }
            if (n > 0)
                liste.append(", ");
            liste.append(c);
            n++;
        }
        return symboles.size() == 1
            ? "Symbole sans effet : " + liste + " (ni dessiné, ni réécrit). Faute de frappe ?"
            : "Symboles sans effet : " + liste + " (ni dessinés, ni réécrits). Faute de frappe ?";
    }
}
