package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Une règle d'un système paramétrique :
 * {@code S(p1,p2,…)[:condition][~poids]=successeur}.
 * <ul>
 * <li>{@code S(p1,p2,…)} : le symbole réécrit et les noms de ses paramètres (aucun : {@code S} ou {@code S()}) ;
 * la règle ne s'applique qu'aux modules qui ont exactement ce nombre de paramètres ;</li>
 * <li>{@code :condition} (facultatif) : la règle ne s'applique que si la condition est vraie (non nulle),
 * par exemple {@code :l>1&&w<2} ;</li>
 * <li>{@code ~poids} (facultatif) : poids pour le tirage au hasard entre plusieurs règles applicables (1 par défaut) ;</li>
 * <li>{@code successeur} : des modules dont les paramètres sont des expressions des paramètres de la règle,
 * par exemple {@code F(l)[+(30)A(l*0.7)]}.</li>
 * </ul>
 * Le signe {@code =} qui sépare les deux côtés est le premier qui n'appartient pas à une comparaison
 * ({@code ==}, {@code <=}, {@code >=}, {@code !=}) : dans une condition, l'égalité s'écrit {@code ==}.
 * <p>
 * Les règles sont séparées par des points-virgules (elles peuvent alors contenir des espaces), ou, s'il n'y a
 * aucun point-virgule, par des espaces (comme pour les autres types) ; sans point-virgule, un texte dont un morceau
 * n'a pas de signe {@code =} est lu comme une seule règle écrite avec des espaces.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class RegleParametrique {
    private final String texte;
    private final char symbole;
    private final List<String> parametres;
    private final Expression condition, poids;
    private final Modules successeur;

    /** Une suite de modules dont les paramètres sont des expressions (successeur d'une règle, axiome). */
    static final class Modules {
        final String symboles;
        final Expression[][] arguments;

        Modules(String symboles, Expression[][] arguments) {
            this.symboles = symboles;
            this.arguments = arguments;
        }
    }

    private RegleParametrique(String jeton) {
        this.texte = jeton.trim();
        int egal = separateur(texte);
        if (egal < 0)
            throw erreur("le signe '=' est manquant (dans une condition, l'égalité s'écrit ==)");
        String gauche = texte.substring(0, egal).trim();
        if (gauche.isEmpty())
            throw erreur("le symbole à réécrire est manquant");
        symbole = gauche.charAt(0);
        if ("()=,:~;".indexOf(symbole) >= 0 || Character.isWhitespace(symbole))
            throw erreur("« " + symbole + " » ne peut pas être le symbole réécrit");
        int i = 1;
        List<String> noms = new ArrayList<>();
        if (i < gauche.length() && gauche.charAt(i) == '(') {
            int fin = gauche.indexOf(')', i);
            if (fin < 0)
                throw erreur("parenthèse fermante manquante après les paramètres");
            String liste = gauche.substring(i + 1, fin).trim();
            if (!liste.isEmpty())
                for (String nom : liste.split(",")) {
                    String n = nom.trim();
                    if (!n.matches("[A-Za-z_][A-Za-z0-9_]*"))
                        throw erreur("« " + n + " » n'est pas un nom de paramètre (lettres, chiffres et _)");
                    if (noms.contains(n))
                        throw erreur("le paramètre « " + n + " » est répété");
                    noms.add(n);
                }
            i = fin + 1;
        }
        parametres = Collections.unmodifiableList(noms);
        String reste = gauche.substring(i).trim();
        String texteCondition = null, textePoids = null;
        if (reste.startsWith(":")) {
            int tilde = reste.indexOf('~');
            texteCondition = (tilde < 0 ? reste.substring(1) : reste.substring(1, tilde)).trim();
            reste = tilde < 0 ? "" : reste.substring(tilde);
        }
        if (reste.startsWith("~")) {
            textePoids = reste.substring(1).trim();
            reste = "";
        }
        if (!reste.isEmpty())
            throw erreur("« " + reste + " » inattendu : la partie gauche a la forme S(paramètres):condition~poids");
        try {
            condition = texteCondition == null ? null : Expression.analyse(texteCondition, parametres);
            poids = textePoids == null ? null : Expression.analyse(textePoids, parametres);
            if (poids != null && poids.estConstante() && !(poids.evalue(new double[0]) > 0))
                throw new IllegalArgumentException("le poids doit être strictement positif");
            successeur = analyseModules(texte.substring(egal + 1), parametres);
        } catch (IllegalArgumentException e) {
            throw erreur(e.getMessage());
        }
    }

    private IllegalArgumentException erreur(String raison) {
        return new IllegalArgumentException("Règle « " + texte + " » invalide : " + raison + ".");
    }

    /**
     * Analyse les règles d'un système paramétrique.
     * @param texte les règles, séparées par des points-virgules (ou par des espaces s'il n'y a aucun point-virgule
     *        et que chaque morceau contient un signe =)
     * @return les règles, dans l'ordre
     * @throws IllegalArgumentException si une règle est mal formée
     */
    public static List<RegleParametrique> analyse(String texte) {
        List<RegleParametrique> regles = new ArrayList<>();
        if (texte == null || texte.trim().isEmpty())
            return regles;
        String[] jetons = texte.indexOf(';') >= 0 ? texte.split(";") : texte.trim().split("\\s+");
        if (texte.indexOf(';') < 0)
            for (String jeton : jetons)
                if (separateur(jeton) < 0) {
                    // Sans point-virgule, des morceaux sans « = » : c'est une seule règle écrite avec des espaces.
                    jetons = new String[] {texte};
                    break;
                }
        for (String jeton : jetons)
            if (!jeton.trim().isEmpty())
                regles.add(new RegleParametrique(jeton));
        return regles;
    }

    /**
     * Retourne la position du signe = qui sépare les deux côtés de la règle, ou −1.
     * @return la position du signe = qui sépare les deux côtés de la règle, ou −1
     */
    private static int separateur(String jeton) {
        int profondeur = 0;
        for (int i = 0; i < jeton.length(); i++) {
            char c = jeton.charAt(i);
            if (c == '(') {
                profondeur++;
            } else if (c == ')') {
                profondeur--;
            } else if (c == '=' && profondeur == 0) {
                char avant = i > 0 ? jeton.charAt(i - 1) : ' ', apres = i + 1 < jeton.length() ? jeton.charAt(i + 1) : ' ';
                if (apres == '=')
                    i++;
                else if ("<>!".indexOf(avant) < 0)
                    return i;
            }
        }
        return -1;
    }

    /**
     * Analyse une suite de modules : chaque symbole peut être suivi, sans espace, de ses paramètres entre
     * parenthèses, séparés par des virgules ({@code F(l*0.7)}, {@code +(30)}). Les espaces entre modules sont ignorés.
     * @param texte les modules
     * @param variables les variables autorisées dans les paramètres
     * @return les symboles et les expressions de leurs paramètres
     * @throws IllegalArgumentException si les parenthèses ne sont pas équilibrées ou si une expression est invalide
     */
    static Modules analyseModules(String texte, List<String> variables) {
        StringBuilder symboles = new StringBuilder();
        List<Expression[]> arguments = new ArrayList<>();
        int i = 0, n = texte.length();
        while (i < n) {
            char c = texte.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '(' || c == ')' || c == ',')
                throw new IllegalArgumentException("« " + c + " » inattendu en position " + (i + 1)
                    + " (les paramètres s'écrivent juste après un symbole : F(1))");
            symboles.append(c);
            i++;
            if (i < n && texte.charAt(i) == '(') {
                int profondeur = 0, debut = i + 1;
                List<Expression> liste = new ArrayList<>();
                for (; i < n; i++) {
                    char d = texte.charAt(i);
                    if (d == '(') {
                        profondeur++;
                    } else if (d == ')') {
                        profondeur--;
                        if (profondeur == 0) {
                            ajouteArgument(texte.substring(debut, i), liste, variables, i == debut);
                            break;
                        }
                    } else if (d == ',' && profondeur == 1) {
                        ajouteArgument(texte.substring(debut, i), liste, variables, false);
                        debut = i + 1;
                    }
                }
                if (profondeur != 0)
                    throw new IllegalArgumentException("parenthèse fermante manquante après « " + c + "( »");
                i++;
                arguments.add(liste.toArray(new Expression[0]));
            } else {
                arguments.add(new Expression[0]);
            }
        }
        return new Modules(symboles.toString(), arguments.toArray(new Expression[0][]));
    }

    private static void ajouteArgument(String texte, List<Expression> liste, List<String> variables, boolean vide) {
        if (vide)
            return;
        if (texte.trim().isEmpty())
            throw new IllegalArgumentException("paramètre vide");
        liste.add(Expression.analyse(texte, variables));
    }

    /**
     * Construit un mot à partir de modules dont les paramètres sont constants (axiome, texte d'une génération).
     * @param texte les modules
     * @return le mot
     * @throws IllegalArgumentException si le texte est mal formé ou utilise une variable
     */
    static Mot mot(String texte) {
        Modules modules = analyseModules(texte, List.of());
        ListeEntiers debuts = new ListeEntiers();
        double[] valeurs = new double[16];
        int nbValeurs = 0;
        for (Expression[] arguments : modules.arguments) {
            debuts.ajoute(nbValeurs);
            for (Expression argument : arguments) {
                if (nbValeurs == valeurs.length)
                    valeurs = java.util.Arrays.copyOf(valeurs, 2 * nbValeurs);
                valeurs[nbValeurs++] = argument.evalue(new double[0]);
            }
        }
        debuts.ajoute(nbValeurs);
        return new Mot(modules.symboles, debuts.tableau(), java.util.Arrays.copyOf(valeurs, nbValeurs));
    }

    /**
     * Retourne le symbole réécrit.
     * @return le symbole réécrit
     */
    public char symbole() { return symbole; }
    /**
     * Retourne le nombre de paramètres des modules auxquels la règle s'applique.
     * @return le nombre de paramètres des modules auxquels la règle s'applique
     */
    public int arite() { return parametres.size(); }
    /**
     * Retourne les noms des paramètres.
     * @return les noms des paramètres
     */
    public List<String> parametres() { return parametres; }
    /**
     * Retourne la condition telle qu'elle a été écrite, ou une chaîne vide.
     * @return la condition telle qu'elle a été écrite, ou une chaîne vide
     */
    public String condition() { return condition == null ? "" : condition.toString(); }
    /**
     * Retourne le poids tel qu'il a été écrit, ou une chaîne vide (poids 1).
     * @return le poids tel qu'il a été écrit, ou une chaîne vide (poids 1)
     */
    public String poids() { return poids == null ? "" : poids.toString(); }
    /**
     * Retourne les symboles du successeur, sans les paramètres.
     * @return les symboles du successeur, sans les paramètres
     */
    public String symbolesSuccesseur() { return successeur.symboles; }

    /**
     * Retourne le successeur tel qu'il a été écrit.
     * @return le successeur tel qu'il a été écrit
     */
    public String successeur() {
        return texte.substring(separateur(texte) + 1).trim();
    }

    /**
     * Retourne le prédécesseur tel qu'il a été écrit : {@code A(l,w)}.
     * @return le prédécesseur tel qu'il a été écrit : {@code A(l,w)}
     */
    public String predecesseur() {
        return symbole + (parametres.isEmpty() ? "" : "(" + String.join(",", parametres) + ")");
    }

    /**
     * Teste la condition de la règle.
     * @param valeurs les paramètres du module
     * @return true si la règle s'applique (pas de condition, ou condition non nulle)
     */
    boolean accepte(double[] valeurs) {
        return condition == null || condition.evalue(valeurs) != 0;
    }

    /**
     * Retourne le poids de la règle pour ce module (1 par défaut).
     * @param valeurs les paramètres du module
     * @return le poids de la règle pour ce module (1 par défaut)
     */
    double poids(double[] valeurs) {
        return poids == null ? 1 : poids.evalue(valeurs);
    }

    /**
     * Ajoute le successeur, avec ses paramètres calculés, à la fin d'un mot en construction.
     * @param valeurs les paramètres du module réécrit
     * @param symboles les symboles du mot en construction
     * @param debuts le début des paramètres de chaque module
     * @param sortie les paramètres du mot en construction
     */
    void produit(double[] valeurs, StringBuilder symboles, ListeEntiers debuts, ListeReels sortie) {
        for (int m = 0; m < successeur.symboles.length(); m++) {
            symboles.append(successeur.symboles.charAt(m));
            debuts.ajoute(sortie.taille());
            for (Expression argument : successeur.arguments[m])
                sortie.ajoute(argument.evalue(valeurs));
        }
    }

    /**
     * Retourne la règle telle qu'elle a été écrite.
     * @return la règle telle qu'elle a été écrite
     */
    @Override
    public String toString() {
        return texte;
    }
}
