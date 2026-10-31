package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.List;

/**
 * Une expression arithmétique des systèmes paramétriques, analysée une fois puis évaluée autant de fois que
 * nécessaire (descente récursive écrite à la main : {@code javax.script} n'existe plus depuis Java 15).
 * <p>
 * Priorités, de la plus faible à la plus forte :
 * {@code ||} ; {@code &&} ; comparaisons {@code == != < <= > >=} ; {@code + -} ; {@code * / %} ;
 * signes {@code - + !} ; puissance {@code ^} (à droite : {@code 2^3^2 = 2^9}, et {@code -2^2 = -4}).
 * Les comparaisons et les opérateurs logiques valent 1 (vrai) ou 0 (faux) ; toute valeur non nulle est vraie.
 * Fonctions : {@code sqrt}, {@code abs}, {@code min}, {@code max}, {@code floor}. Les variables sont les paramètres
 * formels de la règle ({@code l}, {@code w}…).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Expression {
    /** Un nœud de l'arbre de l'expression. */
    private interface Noeud {
        double evalue(double[] variables);
    }

    private final String texte;
    private final Noeud racine;
    private final boolean constante;

    private Expression(String texte, Noeud racine, boolean constante) {
        this.texte = texte;
        this.racine = racine;
        this.constante = constante;
    }

    /**
     * Analyse une expression.
     * @param texte l'expression (les espaces sont ignorés)
     * @param variables les noms des variables autorisées, dans l'ordre des valeurs passées à {@link #evalue}
     * @return l'expression analysée
     * @throws IllegalArgumentException si l'expression est mal formée ou utilise une variable inconnue
     */
    public static Expression analyse(String texte, List<String> variables) {
        Analyseur analyseur = new Analyseur(texte, variables);
        Noeud racine = analyseur.ou();
        analyseur.saute();
        if (analyseur.position < texte.length())
            throw analyseur.erreur("caractère inattendu « " + texte.charAt(analyseur.position) + " »");
        return new Expression(texte.trim(), racine, !analyseur.variableUtilisee);
    }

    /**
     * Calcule la valeur de l'expression.
     * @param variables la valeur de chaque variable
     * @return la valeur
     * @throws IllegalArgumentException en cas de division par zéro ou de résultat qui n'est pas un nombre fini
     */
    public double evalue(double[] variables) {
        double valeur = racine.evalue(variables);
        if (!Double.isFinite(valeur))
            throw new IllegalArgumentException("Le calcul de « " + texte + " » ne donne pas un nombre fini.");
        return valeur;
    }

    /**
     * Retourne true si l'expression n'utilise aucune variable.
     * @return true si l'expression n'utilise aucune variable
     */
    public boolean estConstante() {
        return constante;
    }

    /**
     * Retourne l'expression telle qu'elle a été écrite.
     * @return l'expression telle qu'elle a été écrite
     */
    @Override
    public String toString() {
        return texte;
    }

    /** L'analyseur : une méthode par niveau de priorité. */
    private static final class Analyseur {
        private final String texte;
        private final List<String> variables;
        private int position;
        private boolean variableUtilisee;

        Analyseur(String texte, List<String> variables) {
            this.texte = texte;
            this.variables = variables;
        }

        IllegalArgumentException erreur(String raison) {
            return new IllegalArgumentException("Expression « " + texte.trim() + " » invalide : " + raison + ".");
        }

        void saute() {
            while (position < texte.length() && Character.isWhitespace(texte.charAt(position)))
                position++;
        }

        /** Lit un opérateur s'il est présent (en évitant de prendre le début de « <= » pour « < »). */
        boolean lit(String operateur) {
            saute();
            if (!texte.startsWith(operateur, position))
                return false;
            int apres = position + operateur.length();
            if (operateur.length() == 1 && "<>=!".indexOf(operateur.charAt(0)) >= 0
                && apres < texte.length() && texte.charAt(apres) == '=')
                return false;
            position = apres;
            return true;
        }

        Noeud ou() {
            Noeud gauche = et();
            while (lit("||")) {
                Noeud a = gauche, b = et();
                gauche = v -> a.evalue(v) != 0 || b.evalue(v) != 0 ? 1 : 0;
            }
            return gauche;
        }

        Noeud et() {
            Noeud gauche = comparaison();
            while (lit("&&")) {
                Noeud a = gauche, b = comparaison();
                gauche = v -> a.evalue(v) != 0 && b.evalue(v) != 0 ? 1 : 0;
            }
            return gauche;
        }

        Noeud comparaison() {
            Noeud a = somme();
            String[] operateurs = {"==", "!=", "<=", ">=", "<", ">"};
            for (String operateur : operateurs)
                if (lit(operateur)) {
                    Noeud b = somme();
                    switch (operateur) {
                        case "==": return v -> a.evalue(v) == b.evalue(v) ? 1 : 0;
                        case "!=": return v -> a.evalue(v) != b.evalue(v) ? 1 : 0;
                        case "<=": return v -> a.evalue(v) <= b.evalue(v) ? 1 : 0;
                        case ">=": return v -> a.evalue(v) >= b.evalue(v) ? 1 : 0;
                        case "<": return v -> a.evalue(v) < b.evalue(v) ? 1 : 0;
                        default: return v -> a.evalue(v) > b.evalue(v) ? 1 : 0;
                    }
                }
            return a;
        }

        Noeud somme() {
            Noeud gauche = produit();
            while (true) {
                Noeud a = gauche;
                if (lit("+")) {
                    Noeud b = produit();
                    gauche = v -> a.evalue(v) + b.evalue(v);
                } else if (lit("-")) {
                    Noeud b = produit();
                    gauche = v -> a.evalue(v) - b.evalue(v);
                } else {
                    return gauche;
                }
            }
        }

        Noeud produit() {
            Noeud gauche = unaire();
            while (true) {
                Noeud a = gauche;
                if (lit("*")) {
                    Noeud b = unaire();
                    gauche = v -> a.evalue(v) * b.evalue(v);
                } else if (lit("/") || lit("%")) {
                    boolean division = texte.charAt(position - 1) == '/';
                    Noeud b = unaire();
                    String expression = texte.trim();
                    gauche = v -> {
                        double diviseur = b.evalue(v);
                        if (diviseur == 0)
                            throw new IllegalArgumentException("Division par zéro dans « " + expression + " ».");
                        return division ? a.evalue(v) / diviseur : a.evalue(v) % diviseur;
                    };
                } else {
                    return gauche;
                }
            }
        }

        Noeud unaire() {
            if (lit("-")) {
                Noeud a = unaire();
                return v -> -a.evalue(v);
            }
            if (lit("+"))
                return unaire();
            if (lit("!")) {
                Noeud a = unaire();
                return v -> a.evalue(v) == 0 ? 1 : 0;
            }
            return puissance();
        }

        Noeud puissance() {
            Noeud base = primaire();
            if (lit("^")) {
                Noeud exposant = unaire();
                return v -> Math.pow(base.evalue(v), exposant.evalue(v));
            }
            return base;
        }

        Noeud primaire() {
            saute();
            if (position >= texte.length())
                throw erreur("expression incomplète");
            char c = texte.charAt(position);
            if (c == '(') {
                position++;
                Noeud interieur = ou();
                if (!lit(")"))
                    throw erreur("parenthèse fermante manquante");
                return interieur;
            }
            if (Character.isDigit(c) || c == '.')
                return nombre();
            if (Character.isLetter(c) || c == '_')
                return nom();
            throw erreur("caractère inattendu « " + c + " »");
        }

        Noeud nombre() {
            int debut = position;
            while (position < texte.length() && (Character.isDigit(texte.charAt(position)) || texte.charAt(position) == '.'))
                position++;
            if (position < texte.length() && (texte.charAt(position) == 'e' || texte.charAt(position) == 'E')) {
                int exposant = position + 1;
                if (exposant < texte.length() && (texte.charAt(exposant) == '+' || texte.charAt(exposant) == '-'))
                    exposant++;
                if (exposant < texte.length() && Character.isDigit(texte.charAt(exposant))) {
                    position = exposant;
                    while (position < texte.length() && Character.isDigit(texte.charAt(position)))
                        position++;
                }
            }
            try {
                double valeur = Double.parseDouble(texte.substring(debut, position));
                return v -> valeur;
            } catch (NumberFormatException e) {
                throw erreur("nombre « " + texte.substring(debut, position) + " » illisible");
            }
        }

        Noeud nom() {
            int debut = position;
            while (position < texte.length()
                && (Character.isLetterOrDigit(texte.charAt(position)) || texte.charAt(position) == '_'))
                position++;
            String nom = texte.substring(debut, position);
            saute();
            if (position < texte.length() && texte.charAt(position) == '(') {
                position++;
                List<Noeud> arguments = new ArrayList<>();
                if (!lit(")")) {
                    do {
                        arguments.add(ou());
                    } while (lit(","));
                    if (!lit(")"))
                        throw erreur("parenthèse fermante manquante après les arguments de " + nom);
                }
                return fonction(nom, arguments);
            }
            int indice = variables.indexOf(nom);
            if (indice < 0)
                throw erreur("variable inconnue « " + nom + " »" + (variables.isEmpty()
                    ? " (aucune variable ici : seulement des nombres)"
                    : " (variables : " + String.join(", ", variables) + ")"));
            variableUtilisee = true;
            return v -> v[indice];
        }

        Noeud fonction(String nom, List<Noeud> arguments) {
            int attendus = "min".equals(nom) || "max".equals(nom) ? 2 : 1;
            if (!List.of("sqrt", "abs", "min", "max", "floor").contains(nom))
                throw erreur("fonction inconnue « " + nom + " » (fonctions : sqrt, abs, min, max, floor)");
            if (arguments.size() != attendus)
                throw erreur(nom + " attend " + attendus + (attendus > 1 ? " arguments" : " argument"));
            Noeud a = arguments.get(0), b = attendus > 1 ? arguments.get(1) : null;
            switch (nom) {
                case "sqrt": {
                    String expression = texte.trim();
                    return v -> {
                        double x = a.evalue(v);
                        if (x < 0)
                            throw new IllegalArgumentException("Racine carrée d'un nombre négatif dans « " + expression + " ».");
                        return Math.sqrt(x);
                    };
                }
                case "abs": return v -> Math.abs(a.evalue(v));
                case "floor": return v -> Math.floor(a.evalue(v));
                case "min": return v -> Math.min(a.evalue(v), b.evalue(v));
                default: return v -> Math.max(a.evalue(v), b.evalue(v));
            }
        }
    }
}
