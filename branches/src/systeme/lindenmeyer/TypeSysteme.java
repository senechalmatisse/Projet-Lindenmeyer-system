package systeme.lindenmeyer;

/**
 * Les types de systèmes de Lindenmayer proposés, avec leur nom affiché (aussi utilisé dans les fichiers
 * d'exemples et de sauvegarde) et le moyen de les construire.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public enum TypeSysteme {
    /** Une règle au plus par symbole, sans contexte ni poids. */
    DETERMINISTE("Déterministe", false),
    /** Plusieurs règles par symbole, tirées au hasard selon leur poids ; contextes acceptés. */
    STOCHASTIQUE("Stochastique", true),
    /** Règles {@code G<S>D=...} qui dépendent des voisins du symbole. */
    CONTEXTUEL("Contextuel", false),
    /** Symboles à paramètres numériques ({@code A(l)=F(l)[+(30)A(l*0.7)]}), conditions et poids facultatifs. */
    PARAMETRIQUE("Paramétrique", true);

    private final String nom;
    private final boolean aleatoire;

    TypeSysteme(String nom, boolean aleatoire) {
        this.nom = nom;
        this.aleatoire = aleatoire;
    }

    /**
     * Retourne le nom affiché du type (« Déterministe », « Stochastique »…).
     * @return le nom affiché du type (« Déterministe », « Stochastique »…)
     */
    public String nom() {
        return nom;
    }

    /**
     * Retourne true si ce type tire des règles au hasard (il utilise alors une graine).
     * @return true si ce type tire des règles au hasard (il utilise alors une graine)
     */
    public boolean estAleatoire() {
        return aleatoire;
    }

    /**
     * Construit un système de ce type.
     * @param axiomes le (ou les) axiome(s)
     * @param regles les règles
     * @param angle l'angle en degrés
     * @param iterations le nombre d'itérations
     * @param longueur la longueur d'un segment
     * @param graine la graine du générateur aléatoire (types aléatoires), ou null pour un tirage au hasard
     * @return le système
     * @throws IllegalArgumentException si une règle est invalide pour ce type de système
     */
    public SystemeLindenmeyer cree(String axiomes, String regles, double angle, int iterations, int longueur, Long graine) {
        switch (this) {
            case STOCHASTIQUE:
                return graine == null ? new SystemeStochastique(axiomes, regles, angle, iterations, longueur)
                    : new SystemeStochastique(axiomes, regles, angle, iterations, longueur, graine);
            case CONTEXTUEL:
                return new SystemeContextuel(axiomes, regles, angle, iterations, longueur);
            case PARAMETRIQUE:
                return graine == null ? new SystemeParametrique(axiomes, regles, angle, iterations, longueur)
                    : new SystemeParametrique(axiomes, regles, angle, iterations, longueur, graine);
            default:
                return new SystemeDeterministe(axiomes, regles, angle, iterations, longueur);
        }
    }

    /**
     * Retrouve un type d'après son nom affiché (sans tenir compte des majuscules ni des accents).
     * @param nom le nom (« Déterministe », « deterministe »…)
     * @return le type
     * @throws IllegalArgumentException si le nom ne correspond à aucun type
     */
    public static TypeSysteme depuisNom(String nom) {
        String cherche = sansAccents(nom == null ? "" : nom.trim());
        for (TypeSysteme type : values())
            if (sansAccents(type.nom).equalsIgnoreCase(cherche))
                return type;
        StringBuilder noms = new StringBuilder();
        for (TypeSysteme type : values())
            noms.append(noms.length() == 0 ? "" : ", ").append(type.nom);
        throw new IllegalArgumentException("Type de système inconnu : « " + nom + " » (types : " + noms + ").");
    }

    /**
     * Retrouve le type d'un système.
     * @param systeme le système
     * @return son type
     */
    public static TypeSysteme de(SystemeLindenmeyer systeme) {
        if (systeme instanceof SystemeStochastique)
            return STOCHASTIQUE;
        if (systeme instanceof SystemeContextuel)
            return CONTEXTUEL;
        if (systeme instanceof SystemeParametrique)
            return PARAMETRIQUE;
        return DETERMINISTE;
    }

    private static String sansAccents(String texte) {
        return java.text.Normalizer.normalize(texte, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * Retourne le nom affiché.
     * @return le nom affiché
     */
    @Override
    public String toString() {
        return nom;
    }
}
