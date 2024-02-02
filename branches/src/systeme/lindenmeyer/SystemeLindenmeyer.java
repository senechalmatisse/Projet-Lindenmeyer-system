package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Une classe abstraite qui représente un système de Lindenmeyer, fournissant les fonctionnalités basiques pour la génération et l'affichage.
 * 
 * @author Ali Azou, Matisse Senechal, Rafik Halit, Université de Caen Normandie, France
 */
public abstract class SystemeLindenmeyer implements Generation {
    /**
     * Le nombre d'itérations.
     * La longueur du dessin.
     * L'angle du dessin (en degrés).
     * L'axiome(s) du système.
     * La (ou les) règle(s) du système.
     */
    private int iteration, longueur;
    private double angle;
    private String axiomes, regles;

    /**
     * Construit une nouvelle instance d'un système de Lindenmeyer.
     * @param axiomes le (ou les) axiome(s) du système
     * @param regles la (ou les) règle(s) du système
     * @param angle l'angle (en degrés) pour tourner lors de l'affichage
     * @param iteration le nombre d'itérations
     * @param longueur la longueur du dessin
    */
    public SystemeLindenmeyer(String axiomes, String regles, double angle, int iteration, int longueur) {
        this.axiomes = axiomes;
        this.regles = regles;
        this.angle = angle;
        this.iteration = iteration;
        this.longueur = longueur;
    }

    @Override
    public double getAngle() {
        return this.angle;
    }
    @Override
    public int getIteration() {
        return this.iteration;
    }
    /**
     * Retourne le (ou les) axiome(s) du L-Système.
     * @return le (ou les) axiome(s) du L-Système
     */
    public String getAxiomes() {
        return this.axiomes;
    }
    /**
     * Retourne la (ou les) règle(s) du L-Système.
     * @return la (ou les) règle(s) du L-Système
     */
    public String getRegles() {
        return this.regles;
    }
    @Override
    public int getLongueur() {
        return this.longueur;
    }

    /**
     * Supprime le premier axiome et  le signe '=' d'une règle.
     * @param chaine la chaîne de règle de production à modifier
     * @return la chaîne de règle de production modifiée, sans son premier axiome
     */
    protected String supprimeAxiomeDeRegle(String chaine) {
        return chaine.trim().substring(2);
    }

    /**
     * Transforme une chaîne de caractères en une liste de chaînes de caractères.
     * @param chaine la chaîne à transformer
     * @return une liste de chaînes de caractères
     */
    protected ArrayList<String> transformeChaine(String chaine) {
        return new ArrayList<String>(Arrays.asList(chaine.trim().split(" ")));
    }

    /**
     * Initialise le canvas en paramettant les paramètres spécifiés.
     * @param gc le contexte graphique pour dessiner le système
     * @param canvas le canvas sur lequel on va dessiner
     */
    private void initialiseCanvas(GraphicsContext gc, Canvas canvas) {
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Initialise le crayon du dessin.
     * @param gc le contexte graphique pour dessiner le système
     * @param couleur la couleur à utiliser pour le crayon
     */
    private void initialiseCrayon(GraphicsContext gc, Color couleur) {
        gc.setStroke(couleur);
        gc.setLineWidth(1);
    }

    /**
     * Initialise le contexte graphique du canvas avec les paramètres spécifiés.
     * @param canvas le canvas sur lequel on dessine
     * @param couleur la couleur du stylo à utiliser
     * @return le contexte graphique initialisé
     */
    protected GraphicsContext initialiseContexteGraphique(Canvas canvas, Color couleur) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        initialiseCanvas(gc, canvas);
        gc.translate(500, 700);
        initialiseCrayon(gc, couleur);
        return gc;
    }

    @Override
    public void dessineSysteme(String chaine, int d, Color couleur, Canvas canvas, GraphicsContext gc) {
        double delta = Math.toRadians(angle);
        double x = 500;
        double y = 700;
        double alpha = -Math.PI / 2;

        ArrayDeque<Double> anglesSysteme = new ArrayDeque<>();
        ArrayDeque<Double> abscisseSysteme = new ArrayDeque<>();
        ArrayDeque<Double> ordonneeSysteme = new ArrayDeque<>();
    
        char[] axiomes = chaine.toCharArray();
        for (char axiome : axiomes) {
            switch (axiome) {
                case 'F':
                    gc.strokeLine(x, y, x+d*Math.cos(alpha), y+d*Math.sin(alpha));
                    x += Math.cos(alpha) * d;
                    y += Math.sin(alpha) * d;
                    break;
                case 'f':
                    x += Math.cos(alpha) * d;
                    y += Math.sin(alpha) * d;
                    break;
                case '+':
                    alpha -= delta;
                    break;
                case '-':
                    alpha += delta;
                    break;
                case '[':
                    anglesSysteme.push(alpha);
                    abscisseSysteme.push(x);
                    ordonneeSysteme.push(y);
                    break;
                case ']':
                    alpha = anglesSysteme.pop();
                    x = abscisseSysteme.pop();
                    y = ordonneeSysteme.pop();
                    break;
            }
        }
    }
}
