package systeme.lindenmeyer;

/**
 * Passage du repère du modèle (x à droite, y en haut, z vers l'observateur) aux pixels de la zone de dessin,
 * sans JavaFX (donc testable seul).
 * <p>
 * <b>Vue 2D</b> (de face) : la figure est cadrée sur sa boîte englobante ({@link #echelle2D}).
 * <p>
 * <b>Vue 3D</b> : la figure, centrée en C (centre de sa boîte englobante), tourne de φ (lacet) puis de θ (tangage) :
 * <pre>
 * p  = P − C
 * x1 =  p.x·cos φ + p.z·sin φ
 * z1 = −p.x·sin φ + p.z·cos φ
 * y2 =  p.y·cos θ − z1·sin θ
 * z2 =  p.y·sin θ + z1·cos θ              (z2 &gt; 0 : plus près de l'observateur)
 * k  = perspective ? D / (D − z2) : 1     (D = 3·R, R = rayon de la sphère englobante)
 * X  = cx + e·zoom·k·x1 + panX
 * Y  = cy − e·zoom·k·y2 + panY            (l'axe y de l'écran descend)
 * e  = (min(largeur, hauteur) − 2·MARGE) / (2·R·kmax)     (kmax = 1,5 en perspective, 1 sinon)
 * </pre>
 * Le cadrage par la sphère englobante ne dépend pas de la rotation : la figure ne « pulse » pas quand on la tourne.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Projection {
    /** Marge (en pixels) laissée autour de la figure dessinée. */
    public static final double MARGE = 20;
    /** Facteur de perspective maximal (point le plus proche, z2 = R, avec D = 3·R). */
    public static final double K_MAX_PERSPECTIVE = 1.5;

    private final boolean vue3D, perspective;
    private final double centreEcranX, centreEcranY, panX, panY;
    private final double cx, cy, cz;
    private final double echelle;
    private final double cosYaw, sinYaw, cosPitch, sinPitch;
    private final double rayon, distance;

    private Projection(Trace trace, Vue vue, double largeur, double hauteur, boolean vue3D, boolean ajuster) {
        this.vue3D = vue3D;
        this.perspective = vue3D && vue.perspective;
        this.centreEcranX = largeur / 2;
        this.centreEcranY = hauteur / 2;
        this.panX = vue.panX;
        this.panY = vue.panY;
        this.cx = trace.centreX();
        this.cy = trace.centreY();
        this.cz = trace.centreZ();
        double yaw = Math.toRadians(vue.yaw), pitch = Math.toRadians(vue.pitch);
        this.cosYaw = vue3D ? Math.cos(yaw) : 1;
        this.sinYaw = vue3D ? Math.sin(yaw) : 0;
        this.cosPitch = vue3D ? Math.cos(pitch) : 1;
        this.sinPitch = vue3D ? Math.sin(pitch) : 0;
        this.rayon = vue3D ? trace.rayon() : 0;
        this.distance = 3 * rayon;
        double e;
        if (vue3D) {
            double kmax = perspective ? K_MAX_PERSPECTIVE : 1;
            e = rayon > 0 ? Math.max(1, Math.min(largeur, hauteur) - 2 * MARGE) / (2 * rayon * kmax) : 1;
            if (!ajuster)
                e = Math.min(1, e);
        } else {
            e = echelle2D(trace.etendueX(), trace.etendueY(), largeur, hauteur, ajuster);
        }
        this.echelle = e * vue.zoom;
    }

    /**
     * Prépare la projection d'une trace.
     * @param trace la figure (sa boîte englobante et son rayon fixent le cadrage)
     * @param vue l'orientation, le zoom, le déplacement et la perspective
     * @param largeur la largeur de la zone de dessin (pixels)
     * @param hauteur la hauteur de la zone de dessin (pixels)
     * @param vue3D false pour la vue 2D de face (cadrage sur la boîte englobante, sans rotation)
     * @param ajuster true pour remplir la zone de dessin, false pour garder la taille réelle (réduite si besoin)
     * @return la projection
     */
    public static Projection pour(Trace trace, Vue vue, double largeur, double hauteur, boolean vue3D, boolean ajuster) {
        return new Projection(trace, vue, largeur, hauteur, vue3D, ajuster);
    }

    /**
     * Projette un point du modèle.
     * @param x abscisse du point
     * @param y ordonnée du point
     * @param z cote du point
     * @param sortie reçoit X et Y (pixels) puis la profondeur t entre 0 (le plus loin) et 1 (le plus près) ;
     *        en vue 2D, t vaut 0,5
     */
    public void projette(double x, double y, double z, double[] sortie) {
        double px = x - cx, py = y - cy, pz = z - cz;
        if (!vue3D) {
            sortie[0] = centreEcranX + panX + echelle * px;
            sortie[1] = centreEcranY + panY - echelle * py;
            sortie[2] = 0.5;
            return;
        }
        double x1 = px * cosYaw + pz * sinYaw;
        double z1 = -px * sinYaw + pz * cosYaw;
        double y2 = py * cosPitch - z1 * sinPitch;
        double z2 = py * sinPitch + z1 * cosPitch;
        double k = perspective && distance > 0 ? distance / (distance - z2) : 1;
        sortie[0] = centreEcranX + echelle * k * x1 + panX;
        sortie[1] = centreEcranY - echelle * k * y2 + panY;
        sortie[2] = rayon > 0 ? Math.max(0, Math.min(1, (z2 + rayon) / (2 * rayon))) : 0.5;
    }

    /**
     * Retourne le nombre de pixels par unité du modèle (zoom compris, sans la perspective).
     * @return le nombre de pixels par unité du modèle (zoom compris, sans la perspective)
     */
    public double echelle() {
        return echelle;
    }

    /**
     * Retourne true pour une projection 3D.
     * @return true pour une projection 3D
     */
    public boolean est3D() {
        return vue3D;
    }

    /**
     * Fait tourner un point déjà centré (lacet φ puis tangage θ), sans cadrage.
     * @param px abscisse du point
     * @param py ordonnée du point
     * @param pz cote du point
     * @param yawDegres le lacet φ, en degrés
     * @param pitchDegres le tangage θ, en degrés
     * @return {x1, y2, z2} : abscisse et ordonnée à l'écran (y vers le haut), profondeur (positive vers l'observateur)
     */
    public static double[] tourne(double px, double py, double pz, double yawDegres, double pitchDegres) {
        double phi = Math.toRadians(yawDegres), theta = Math.toRadians(pitchDegres);
        double x1 = px * Math.cos(phi) + pz * Math.sin(phi);
        double z1 = -px * Math.sin(phi) + pz * Math.cos(phi);
        double y2 = py * Math.cos(theta) - z1 * Math.sin(theta);
        double z2 = py * Math.sin(theta) + z1 * Math.cos(theta);
        return new double[] {x1, y2, z2};
    }

    /**
     * Calcule l'échelle d'une figure plane pour qu'elle tienne dans la zone de dessin.
     * <ul>
     * <li>{@code ajuster} vrai : la figure est agrandie ou réduite pour remplir la zone (marges comprises) ;</li>
     * <li>{@code ajuster} faux : taille réelle (1 unité = 1 pixel), réduite seulement si elle déborde.</li>
     * </ul>
     * Une figure dégénérée (deux étendues nulles : un point) garde l'échelle 1.
     *
     * @param etendueX la largeur de la figure (unités du modèle)
     * @param etendueY la hauteur de la figure (unités du modèle)
     * @param largeur la largeur de la zone de dessin (pixels)
     * @param hauteur la hauteur de la zone de dessin (pixels)
     * @param ajuster true pour ajuster la figure à la zone de dessin
     * @return le nombre de pixels par unité du modèle
     */
    public static double echelle2D(double etendueX, double etendueY, double largeur, double hauteur, boolean ajuster) {
        double echelle = ajuster ? Double.POSITIVE_INFINITY : 1;
        if (etendueX > 0)
            echelle = Math.min(echelle, Math.max(1, largeur - 2 * MARGE) / etendueX);
        if (etendueY > 0)
            echelle = Math.min(echelle, Math.max(1, hauteur - 2 * MARGE) / etendueY);
        return Double.isInfinite(echelle) ? 1 : echelle;
    }
}
