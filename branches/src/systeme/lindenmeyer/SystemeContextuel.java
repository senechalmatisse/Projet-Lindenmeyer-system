package systeme.lindenmeyer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * Cette classe représente un système de Lindenmeyer contextuel.
 * Un système de Lindenmeyer contextuel utilise des règles qui prennent en compte le contexte (les axiomes précédents)
 * lors de la génération de la chaîne de caractères du système.
 * 
 * @author Ali Azou, Matisse Senechal, Rafik Halit, Université de Caen Normandie, France
 */
public class SystemeContextuel extends SystemeLindenmeyer implements Generation {
    /** Un tableau des règles contextuelles qui permettent de définir l'évolution de chaque axiome en fonction de son contexte. */
    private String[][] reglesContextuelles;

    /**
     * Construit une nouvelle instance d'un système de Lindenmeyer contextuel.
     * @param axiome le (ou les) axiome(s) du système
     * @param regles la (ou les) règle(s) du système
     * @param angle l'angle (en degrés) pour tourner lors de l'affichage
     * @param iteration le nombre d'itérations
     * @param longueur la longueur du dessin
     * @param reglesContextuelles le tableau des règles contextuelles
     */
    public SystemeContextuel(String axiome, String regles, double angle, int iteration, int longueur, String[][] reglesContextuelles) {
        super(axiome, regles, angle, iteration, longueur);
        this.reglesContextuelles = reglesContextuelles;
    }
    public SystemeContextuel() {
        this("F X", "", 25, 5, 7, new String[][] {{"X", "F-[[X]+X]+F[+FX]-X"}, {"F", "FF"}});
    }

    /**
     * Initialise les règles contextuelles comme un LinkedHashMap avec
     * le contexte comme clé et la règle correspondante comme valeur.
     * @return un LinkedHashMap avec les règles contextuelles
     */
    private LinkedHashMap<String, String> initialiseReglesContextuelles() {
        LinkedHashMap<String, String> associeContextesRegles = new LinkedHashMap<>();
        for (String[] regle : reglesContextuelles)
            associeContextesRegles.put(regle[0], regle[1]);
        return associeContextesRegles;
    }

    /**
     * Transforme une chaîne de caractères en une liste de caractères.
     * @param chaine la chaîne à transformer
     * @return une liste de caractères
     */
    private ArrayList<Character> decomposeChaine(String chaine) {
        ArrayList<Character> chaineDecomposee = new ArrayList<>(chaine.length());
        for (char symbole : chaine.toCharArray())
            chaineDecomposee.add(symbole);
        return chaineDecomposee;
    }

    /**
     * Applique des règles contextuelles à une chaîne de caractères donnée.
     * @param chaine la chaîne de caractères à laquelle appliquer les règles.
     * @param reglesContextuelles un LinkedHashMap contenant les règles contextuelles à appliquer.
     * @return une chaîne de caractères après l'application des règles.
     */
    private String appliquerReglesContextuelles(String chaine, LinkedHashMap<String, String> reglesContextuelles) {
        String reglesAppliquees = "";
        for (char symbole : decomposeChaine(chaine)) {
            String regle = reglesContextuelles.get(String.valueOf(symbole));
            if (regle != null)
                reglesAppliquees += regle;
            else
                reglesAppliquees += symbole;
        }
        return reglesAppliquees;
    }

    @Override
    public void generationSysteme(int nbIterations, double angle, int longueur, Color couleur, Canvas canvas) {
        String chaineAxiomes = getAxiomes();
        LinkedHashMap<String, String> regles = initialiseReglesContextuelles();
        for (int iteration=0; iteration < nbIterations; iteration++)
            chaineAxiomes = appliquerReglesContextuelles(chaineAxiomes, regles);
        GraphicsContext gc = initialiseContexteGraphique(canvas, couleur);
        dessineSysteme(chaineAxiomes, longueur, couleur, canvas, gc);
    }
}