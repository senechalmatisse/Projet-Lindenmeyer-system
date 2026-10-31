package systeme.lindenmeyer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Les paramètres d'un système, à enregistrer dans un fichier {@code .lsys} ou à partager sur une ligne
 * (presse-papiers). Sans JavaFX.
 * <p>
 * Fichier : une {@link Properties} en UTF-8, clés {@code type}, {@code axiome}, {@code regles}, {@code angle},
 * {@code iterations}, {@code longueur} et, facultative, {@code graine}. {@code Properties} protège {@code =},
 * {@code :} et les caractères spéciaux : aucune règle n'est altérée.
 * <p>
 * Une ligne : {@code lsys1:} suivi du même texte encodé en Base64 (variante pour les URL, sans remplissage).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class SauvegardeSysteme {
    /** Le début d'un système copié sur une ligne. */
    public static final String PREFIXE = "lsys1:";

    /** Le type de système. */
    public final TypeSysteme type;
    /** L'axiome et les règles, tels qu'ils ont été saisis. */
    public final String axiome, regles;
    /** L'angle en degrés. */
    public final double angle;
    /** Le nombre d'itérations et la longueur d'un segment. */
    public final int iterations, longueur;
    /** La graine du tirage au hasard, ou null. */
    public final Long graine;

    /**
     * Rassemble les paramètres d'un système.
     * @param type le type
     * @param axiome l'axiome
     * @param regles les règles
     * @param angle l'angle en degrés
     * @param iterations le nombre d'itérations
     * @param longueur la longueur d'un segment
     * @param graine la graine, ou null
     */
    public SauvegardeSysteme(TypeSysteme type, String axiome, String regles, double angle, int iterations, int longueur,
                             Long graine) {
        this.type = type;
        this.axiome = axiome == null ? "" : axiome;
        this.regles = regles == null ? "" : regles;
        this.angle = angle;
        this.iterations = iterations;
        this.longueur = longueur;
        this.graine = graine;
    }

    private Properties proprietes() {
        Properties p = new Properties();
        p.setProperty("type", type.nom());
        p.setProperty("axiome", axiome);
        p.setProperty("regles", regles);
        p.setProperty("angle", Mot.nombre(angle));
        p.setProperty("iterations", String.valueOf(iterations));
        p.setProperty("longueur", String.valueOf(longueur));
        if (graine != null)
            p.setProperty("graine", String.valueOf(graine));
        return p;
    }

    /**
     * Écrit le système (fichier {@code .lsys}) ; le flux doit être en UTF-8.
     * @param sortie la destination
     * @throws IOException si l'écriture échoue
     */
    public void ecrit(Writer sortie) throws IOException {
        proprietes().store(sortie, "Systeme de Lindenmayer");
        sortie.flush();
    }

    /**
     * Lit un système (fichier {@code .lsys}) ; le flux doit être en UTF-8.
     * @param entree la source
     * @return le système
     * @throws IOException si la lecture échoue
     * @throws IllegalArgumentException si une clé manque ou si une valeur est illisible
     */
    public static SauvegardeSysteme lis(Reader entree) throws IOException {
        Properties p = new Properties();
        p.load(entree);
        return depuis(p);
    }

    /**
     * Code le système sur une seule ligne, pour le presse-papiers.
     * @return {@code lsys1:} suivi du texte en Base64
     */
    public String versPressePapiers() {
        StringWriter texte = new StringWriter();
        try {
            ecrit(texte);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // Les lignes de commentaire (dont la date) ne servent à rien sur une ligne.
        StringBuilder utile = new StringBuilder();
        for (String ligne : texte.toString().split("\\R"))
            if (!ligne.startsWith("#"))
                utile.append(ligne).append('\n');
        return PREFIXE + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(utile.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Décode un système copié sur une ligne.
     * @param ligne le texte du presse-papiers
     * @return le système
     * @throws IllegalArgumentException si le texte n'est pas un système copié, ou s'il est tronqué
     */
    public static SauvegardeSysteme depuisPressePapiers(String ligne) {
        String texte = ligne == null ? "" : ligne.trim();
        if (!texte.startsWith(PREFIXE))
            throw new IllegalArgumentException("Le presse-papiers ne contient pas de système : le texte doit commencer par « "
                + PREFIXE + " » (bouton « Copier le système »).");
        byte[] octets;
        try {
            octets = Base64.getUrlDecoder().decode(texte.substring(PREFIXE.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Le système copié est illisible (texte tronqué ou modifié ?).", e);
        }
        try {
            return lis(new StringReader(new String(octets, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Le système copié est incomplet (texte tronqué ?) : " + e.getMessage(), e);
        }
    }

    private static SauvegardeSysteme depuis(Properties p) {
        for (String cle : new String[] {"type", "axiome", "regles", "angle", "iterations", "longueur"})
            if (p.getProperty(cle) == null)
                throw new IllegalArgumentException("la clé « " + cle + " » manque.");
        TypeSysteme type = TypeSysteme.depuisNom(p.getProperty("type"));
        try {
            double angle = Double.parseDouble(p.getProperty("angle").trim());
            int iterations = Integer.parseInt(p.getProperty("iterations").trim());
            int longueur = Integer.parseInt(p.getProperty("longueur").trim());
            String graine = p.getProperty("graine");
            return new SauvegardeSysteme(type, p.getProperty("axiome"), p.getProperty("regles"), angle, iterations,
                longueur, graine == null || graine.trim().isEmpty() ? null : Long.valueOf(graine.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("un nombre est illisible (" + e.getMessage() + ").", e);
        }
    }
}
