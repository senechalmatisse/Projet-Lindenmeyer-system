package systeme.lindenmeyer;

/**
 * Un exemple du parcours : les paramètres d'un système et les textes qui l'accompagnent
 * (description, « à observer », « à essayer »).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Exemple {
    private final String nom, axiome, regles, description, observer, essayer;
    private final TypeSysteme type;
    private final double angle;
    private final int iterations, longueur;

    /**
     * Crée un exemple.
     * @param nom le nom affiché dans la liste
     * @param type le type de système
     * @param axiome l'axiome
     * @param regles les règles (peut être vide)
     * @param angle l'angle en degrés
     * @param iterations le nombre d'itérations
     * @param longueur la longueur d'un segment
     * @param description ce que montre l'exemple (peut être vide)
     * @param observer ce qu'il faut observer (peut être vide)
     * @param essayer ce qu'on peut essayer (peut être vide)
     */
    public Exemple(String nom, TypeSysteme type, String axiome, String regles, double angle, int iterations, int longueur,
                   String description, String observer, String essayer) {
        this.nom = nom;
        this.type = type;
        this.axiome = axiome;
        this.regles = regles == null ? "" : regles;
        this.angle = angle;
        this.iterations = iterations;
        this.longueur = longueur;
        this.description = description == null ? "" : description;
        this.observer = observer == null ? "" : observer;
        this.essayer = essayer == null ? "" : essayer;
    }

    /**
     * Construit le système décrit par l'exemple.
     * @param graine la graine (types aléatoires), ou null pour un tirage au hasard
     * @return le système
     * @throws IllegalArgumentException si une règle est invalide
     */
    public SystemeLindenmeyer creeSysteme(Long graine) {
        return type.cree(axiome, regles, angle, iterations, longueur, graine);
    }

    /**
     * Retourne le nom affiché.
     * @return le nom affiché
     */
    public String nom() { return nom; }
    /**
     * Retourne le type de système.
     * @return le type de système
     */
    public TypeSysteme type() { return type; }
    /**
     * Retourne l'axiome.
     * @return l'axiome
     */
    public String axiome() { return axiome; }
    /**
     * Retourne les règles.
     * @return les règles
     */
    public String regles() { return regles; }
    /**
     * Retourne l'angle en degrés.
     * @return l'angle en degrés
     */
    public double angle() { return angle; }
    /**
     * Retourne le nombre d'itérations.
     * @return le nombre d'itérations
     */
    public int iterations() { return iterations; }
    /**
     * Retourne la longueur d'un segment.
     * @return la longueur d'un segment
     */
    public int longueur() { return longueur; }
    /**
     * Retourne ce que montre l'exemple (vide si absent).
     * @return ce que montre l'exemple (vide si absent)
     */
    public String description() { return description; }
    /**
     * Retourne ce qu'il faut observer (vide si absent).
     * @return ce qu'il faut observer (vide si absent)
     */
    public String observer() { return observer; }
    /**
     * Retourne ce qu'on peut essayer (vide si absent).
     * @return ce qu'on peut essayer (vide si absent)
     */
    public String essayer() { return essayer; }

    /**
     * Retourne le nom affiché.
     * @return le nom affiché
     */
    @Override
    public String toString() {
        return nom;
    }
}
