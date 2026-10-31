package systeme.affichage;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systeme.lindenmeyer.SystemeLindenmeyer;
import systeme.lindenmeyer.Trace;
import systeme.lindenmeyer.Vue;

/**
 * Un système de Lindenmayer du moteur, doté des méthodes de dessin de {@link GenerationGraphique}.
 * Le dessin reste celui d'avant la 3D : vue de face, taille réelle (réduite si la figure déborde),
 * un trait de 1 pixel.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public class SystemeGraphique implements GenerationGraphique {
    private final SystemeLindenmeyer systeme;

    /**
     * Ajoute le dessin à un système.
     * @param systeme le système à dessiner
     */
    public SystemeGraphique(SystemeLindenmeyer systeme) {
        this.systeme = systeme;
    }

    /**
     * Retourne le système dessiné.
     * @return le système dessiné
     */
    public SystemeLindenmeyer getSysteme() {
        return systeme;
    }

    @Override
    public String genereChaine(int nbIterations) {
        return systeme.genereChaine(nbIterations);
    }

    @Override
    public double getAngle() {
        return systeme.getAngle();
    }

    @Override
    public int getIteration() {
        return systeme.getIteration();
    }

    @Override
    public int getLongueur() {
        return systeme.getLongueur();
    }

    @Override
    public void generationSysteme(int nbIterations, double angle, int longueur, Color couleur, Canvas canvas) {
        String chaine = genereChaine(nbIterations);
        dessineSysteme(chaine, longueur, couleur, canvas, canvas.getGraphicsContext2D());
    }

    @Override
    public void dessineSysteme(String chaine, int longueur, Color couleur, Canvas canvas, GraphicsContext gc) {
        // L'interprétation a lieu avant d'effacer : en cas d'erreur, le canvas n'est pas modifié.
        Trace trace = systeme.interprete(chaine, longueur, systeme.getAngle());
        RenduCanvas.Options options = new RenduCanvas.Options();
        options.couleur = couleur;
        options.ajuster = false;
        new RenduCanvas().dessine(trace, new Vue(), options, gc, canvas.getWidth(), canvas.getHeight());
    }
}
