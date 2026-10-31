package systeme.lindenmeyer;

/**
 * Les paramètres de la vue (sans JavaFX) : orientation, zoom, déplacement et perspective.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Vue {
    /** Bornes du zoom. */
    public static final double ZOOM_MIN = 0.1, ZOOM_MAX = 50;

    /** Rotation autour de l'axe vertical (lacet), en degrés. */
    public double yaw;
    /** Rotation autour de l'axe horizontal (tangage), en degrés, entre −90 et 90. */
    public double pitch;
    /** Facteur de zoom (1 = figure ajustée). */
    public double zoom = 1;
    /** Déplacement de la figure à l'écran, en pixels. */
    public double panX, panY;
    /** Vrai pour une projection en perspective (sinon orthographique). */
    public boolean perspective;

    /** Crée une vue de face, sans zoom ni déplacement, orthographique. */
    public Vue() {
        super();
    }

    /** Revient à la vue de face, sans zoom ni déplacement (la perspective est conservée). */
    public void reinitialise() {
        yaw = 0;
        pitch = 0;
        zoom = 1;
        panX = 0;
        panY = 0;
    }

    /**
     * Fait tourner la vue (glisser de la souris) : le lacet suit le déplacement horizontal,
     * le tangage le déplacement vertical (borné à ±90°).
     * @param dx le déplacement horizontal de la souris, en pixels
     * @param dy le déplacement vertical de la souris, en pixels
     */
    public void tourne(double dx, double dy) {
        yaw += dx * 0.5;
        pitch = Math.max(-90, Math.min(90, pitch + dy * 0.5));
    }

    /**
     * Zoome en gardant immobile le point sous le curseur.
     * @param facteur le facteur de zoom demandé (borné pour rester dans [ZOOM_MIN, ZOOM_MAX])
     * @param mx l'abscisse du curseur par rapport au centre de la zone de dessin
     * @param my l'ordonnée du curseur par rapport au centre de la zone de dessin
     */
    public void zoome(double facteur, double mx, double my) {
        double nouveau = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * facteur));
        double rapport = nouveau / zoom;
        panX = mx - rapport * (mx - panX);
        panY = my - rapport * (my - panY);
        zoom = nouveau;
    }
}
