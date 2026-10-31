package systeme.affichage;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import systeme.lindenmeyer.Projection;
import systeme.lindenmeyer.Trace;
import systeme.lindenmeyer.Vue;

/**
 * Dessine une {@link Trace} sur un canvas JavaFX, en vue 2D (de face) ou en vue 3D (projection manuelle,
 * voir {@link Projection}).
 * <ul>
 * <li>Vue 2D, trait simple : la figure est tracée en (x, −y) avec une transformation du contexte graphique,
 * un segment à la fois, comme avant l'introduction de la 3D (rendu identique).</li>
 * <li>Vue 3D : les coordonnées sont projetées en pixels ; les segments sont répartis en 8 tranches de
 * profondeur, tracées de la plus lointaine (plus pâle) à la plus proche.</li>
 * <li>Épaisseur et couleur : chaque segment a sa largeur (2,5 pixels × largeur relative, au moins 0,5 pixel)
 * et sa couleur de palette (symboles {@code !} et {@code '}, ou mode automatique selon la profondeur).</li>
 * </ul>
 * Dans les deux derniers cas, les segments sont regroupés par (tranche, épaisseur, couleur) et chaque groupe
 * est tracé avec un seul chemin, pour ne pas changer d'état de dessin à chaque segment.
 * Les polygones (feuilles) sont remplis, tranche par tranche en vue 3D, juste avant les segments de leur tranche.
 * Une instance garde ses tableaux de travail d'un dessin à l'autre (un rendu par canvas).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class RenduCanvas {
    /** Au-delà, pendant un glisser de souris, on ne trace qu'un segment sur k (k = ⌈n / 50 000⌉). */
    public static final int MAX_SEGMENTS_APERCU = 50_000;
    /** Palette du mode « épaisseur et couleur », du brun (tronc, indice 0) au vert clair (extrémités, indice 7). */
    public static final Color[] PALETTE = Trace.PALETTE.stream().map(Color::web).toArray(Color[]::new);
    /** Épaisseur à l'écran d'un trait de largeur relative 1, et épaisseur minimale (pixels). */
    public static final double PIXELS_PAR_LARGEUR = 2.5, PIXELS_MIN = 0.5;
    /** Nombre de tranches de profondeur en vue 3D. */
    private static final int NB_TRANCHES = 8;
    /** Épaisseurs arrondies au demi-pixel, jusqu'à 50 pixels. */
    private static final int NB_EPAISSEURS = 101;

    /** Les réglages d'un dessin. */
    public static final class Options {
        /** La couleur des traits (trait simple). */
        public Color couleur = Color.BROWN;
        /** true pour la vue 3D (projection manuelle), false pour la vue 2D de face. */
        public boolean vue3D;
        /** true pour remplir la zone de dessin, false pour garder la taille réelle. */
        public boolean ajuster = true;
        /** true pour l'épaisseur et la couleur de chaque segment, false pour un trait simple d'une seule couleur. */
        public boolean epaisseurCouleur;
        /** true pendant un glisser de souris : dessin allégé au-delà de {@link #MAX_SEGMENTS_APERCU} segments. */
        public boolean apercu;

        /** Des réglages par défaut : vue 2D ajustée, trait simple brun. */
        public Options() {
            super();
        }
    }

    /** Crée un rendu (ses tableaux de travail sont réutilisés d'un dessin à l'autre). */
    public RenduCanvas() {
        super();
    }

    /** Coordonnées à l'écran (x1, y1, x2, y2) de chaque segment, groupe de dessin et ordre de tracé. */
    private float[] ecran = new float[0];
    private int[] groupes = new int[0];
    private int[] ordre = new int[0];

    /**
     * Dessine une trace en vue 2D, à sa taille réelle (réduite si elle déborde), comme l'ancien
     * {@code dessineSysteme}.
     * @param trace la figure
     * @param vue le zoom et le déplacement (l'orientation est ignorée en 2D)
     * @param couleur la couleur des traits
     * @param canvas le canvas
     */
    public static void dessine(Trace trace, Vue vue, Color couleur, Canvas canvas) {
        Options options = new Options();
        options.couleur = couleur;
        options.ajuster = false;
        new RenduCanvas().dessine(trace, vue, options, canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Efface le canvas puis dessine la trace.
     * @param trace la figure (null : le canvas est seulement effacé)
     * @param vue l'orientation, le zoom et le déplacement
     * @param options la couleur, le mode de vue et le cadrage
     * @param gc le contexte graphique du canvas
     * @param largeur la largeur du canvas
     * @param hauteur la hauteur du canvas
     */
    public void dessine(Trace trace, Vue vue, Options options, GraphicsContext gc, double largeur, double hauteur) {
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, largeur, hauteur);
        gc.setStroke(options.couleur);
        gc.setLineWidth(1);
        gc.setLineCap(StrokeLineCap.SQUARE);
        gc.setLineJoin(StrokeLineJoin.MITER);
        if (trace == null || trace.estVide())
            return;
        if (!options.vue3D && !options.epaisseurCouleur)
            dessine2D(trace, vue, options, gc, largeur, hauteur);
        else
            dessineProjete(trace, vue, options, gc, largeur, hauteur);
    }

    /**
     * Retourne 1, ou k pour ne tracer qu'un segment sur k pendant un glisser.
     * @return 1, ou k pour ne tracer qu'un segment sur k pendant un glisser
     */
    private static int pas(Trace trace, Options options) {
        return options.apercu && trace.nbSegments > MAX_SEGMENTS_APERCU
            ? (trace.nbSegments + MAX_SEGMENTS_APERCU - 1) / MAX_SEGMENTS_APERCU : 1;
    }

    /**
     * Épaisseur à l'écran d'un segment.
     * @param trace la figure
     * @param i le numéro du segment
     * @return l'épaisseur en pixels (au moins {@link #PIXELS_MIN})
     */
    public static double epaisseur(Trace trace, int i) {
        return Math.max(PIXELS_MIN, PIXELS_PAR_LARGEUR * trace.largeurEffective(i));
    }

    private void dessine2D(Trace trace, Vue vue, Options options, GraphicsContext gc, double largeur, double hauteur) {
        // La figure est centrée dans le canvas, à l'échelle choisie (voir Projection.echelle2D) ;
        // le point (x, y, z) est dessiné en (x, −y) grâce à une transformation, avec un trait de 1 pixel.
        double echelle = Projection.pour(trace, vue, largeur, hauteur, false, options.ajuster).echelle();
        gc.setTransform(echelle, 0, 0, echelle, largeur / 2 + vue.panX - echelle * trace.centreX(),
            hauteur / 2 + vue.panY + echelle * trace.centreY());
        gc.setLineWidth(1 / echelle);
        double[] s = trace.segments;
        int pas = pas(trace, options);
        for (int i = 0; i < trace.nbSegments; i += pas) {
            int j = 6 * i;
            gc.strokeLine(s[j], -s[j + 1], s[j + 3], -s[j + 4]);
        }
        gc.setFill(options.couleur);
        for (double[] polygone : trace.polygones) {
            int n = polygone.length / 3;
            double[] xs = new double[n], ys = new double[n];
            for (int k = 0; k < n; k++) {
                xs[k] = polygone[3 * k];
                ys[k] = -polygone[3 * k + 1];
            }
            gc.fillPolygon(xs, ys, n);
        }
    }

    /**
     * Remplit les polygones d'une tranche de profondeur.
     * @param tranchesPolygones la tranche de chaque polygone (vue 3D), ou null en vue 2D
     */
    private static void remplitPolygones(Trace trace, Options options, GraphicsContext gc, double[][] xs, double[][] ys,
                                         int[] tranchesPolygones, int tranche) {
        for (int k = 0; k < xs.length; k++) {
            if (tranchesPolygones != null && tranchesPolygones[k] != tranche)
                continue;
            Color base = options.epaisseurCouleur ? PALETTE[trace.couleurPolygone[k]] : options.couleur;
            if (options.vue3D)
                base = base.interpolate(Color.WHITE, 0.55 * (1 - (tranche + 0.5) / NB_TRANCHES));
            gc.setFill(base);
            gc.fillPolygon(xs[k], ys[k], xs[k].length);
        }
    }

    /** Projette les segments en pixels et calcule leur groupe de dessin (tranche de profondeur, épaisseur, couleur). */
    private void projette(Trace trace, Vue vue, Options options, double largeur, double hauteur, int pas) {
        Projection projection = Projection.pour(trace, vue, largeur, hauteur, options.vue3D, options.ajuster);
        int n = trace.nbSegments;
        if (groupes.length < n) {
            ecran = new float[4 * n];
            groupes = new int[n];
            ordre = new int[n];
        }
        double[] s = trace.segments, a = new double[3], b = new double[3];
        for (int i = 0; i < n; i += pas) {
            int j = 6 * i;
            projection.projette(s[j], s[j + 1], s[j + 2], a);
            projection.projette(s[j + 3], s[j + 4], s[j + 5], b);
            ecran[4 * i] = (float) a[0];
            ecran[4 * i + 1] = (float) a[1];
            ecran[4 * i + 2] = (float) b[0];
            ecran[4 * i + 3] = (float) b[1];
            int tranche = options.vue3D ? Math.min(NB_TRANCHES - 1, (int) ((a[2] + b[2]) / 2 * NB_TRANCHES)) : 0;
            int epaisseur = 2, couleur = 0;
            if (options.epaisseurCouleur) {
                epaisseur = (int) Math.min(NB_EPAISSEURS - 1, Math.round(2 * epaisseur(trace, i)));
                couleur = trace.couleurEffective(i);
            }
            // Ordre de tracé : les tranches lointaines d'abord, puis les traits épais avant les fins.
            groupes[i] = (tranche * NB_EPAISSEURS + (NB_EPAISSEURS - 1 - epaisseur)) * Trace.TAILLE_PALETTE + couleur;
        }
    }

    private void dessineProjete(Trace trace, Vue vue, Options options, GraphicsContext gc, double largeur, double hauteur) {
        int n = trace.nbSegments, pas = pas(trace, options);
        projette(trace, vue, options, largeur, hauteur, pas);
        // Tri par groupe (tri par dénombrement : les groupes sont peu nombreux).
        int nbGroupes = NB_TRANCHES * NB_EPAISSEURS * Trace.TAILLE_PALETTE;
        int[] debuts = new int[nbGroupes + 1];
        for (int i = 0; i < n; i += pas)
            debuts[groupes[i] + 1]++;
        for (int g = 0; g < nbGroupes; g++)
            debuts[g + 1] += debuts[g];
        int[] position = debuts.clone();
        for (int i = 0; i < n; i += pas)
            ordre[position[groupes[i]]++] = i;

        // Polygones : sommets projetés et tranche de profondeur (celle de leur centre).
        Projection projection = Projection.pour(trace, vue, largeur, hauteur, options.vue3D, options.ajuster);
        int nbPolygones = trace.nbPolygones();
        double[][] xs = new double[nbPolygones][], ys = new double[nbPolygones][];
        int[] tranchesPolygones = options.vue3D ? new int[nbPolygones] : null;
        double[] p = new double[3];
        for (int k = 0; k < nbPolygones; k++) {
            double[] polygone = trace.polygones.get(k);
            int m = polygone.length / 3;
            xs[k] = new double[m];
            ys[k] = new double[m];
            double profondeur = 0;
            for (int j = 0; j < m; j++) {
                projection.projette(polygone[3 * j], polygone[3 * j + 1], polygone[3 * j + 2], p);
                xs[k][j] = p[0];
                ys[k][j] = p[1];
                profondeur += p[2] / m;
            }
            if (tranchesPolygones != null)
                tranchesPolygones[k] = Math.min(NB_TRANCHES - 1, (int) (profondeur * NB_TRANCHES));
        }

        // Coordonnées déjà en pixels : pas de transformation.
        gc.setTransform(1, 0, 0, 1, 0, 0);
        if (options.epaisseurCouleur) {
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
        }
        int polygonesRemplis = -1;
        for (int g = 0; g < nbGroupes; g++) {
            if (debuts[g] == debuts[g + 1])
                continue;
            int couleur = g % Trace.TAILLE_PALETTE;
            int epaisseur = NB_EPAISSEURS - 1 - (g / Trace.TAILLE_PALETTE) % NB_EPAISSEURS;
            int tranche = g / (Trace.TAILLE_PALETTE * NB_EPAISSEURS);
            // Les polygones des tranches lointaines passent avant les segments de cette tranche.
            for (; polygonesRemplis < tranche; polygonesRemplis++)
                if (nbPolygones > 0)
                    remplitPolygones(trace, options, gc, xs, ys, tranchesPolygones, polygonesRemplis + 1);
            Color base = options.epaisseurCouleur ? PALETTE[couleur] : options.couleur;
            if (options.vue3D)
                base = base.interpolate(Color.WHITE, 0.55 * (1 - (tranche + 0.5) / NB_TRANCHES));
            gc.setStroke(base);
            gc.setLineWidth(options.epaisseurCouleur ? epaisseur / 2.0 : 1);
            gc.beginPath();
            for (int k = debuts[g]; k < debuts[g + 1]; k++) {
                int i = ordre[k];
                gc.moveTo(ecran[4 * i], ecran[4 * i + 1]);
                gc.lineTo(ecran[4 * i + 2], ecran[4 * i + 3]);
            }
            gc.stroke();
        }
        // Polygones des tranches restantes (et tous les polygones en vue 2D s'il n'y a aucun segment).
        for (; polygonesRemplis < (options.vue3D ? NB_TRANCHES - 1 : 0); polygonesRemplis++)
            if (nbPolygones > 0)
                remplitPolygones(trace, options, gc, xs, ys, tranchesPolygones, polygonesRemplis + 1);
    }

    /**
     * Calcule les coordonnées à l'écran de tous les segments (pour trouver le segment sous la souris).
     * @param trace la figure
     * @param projection la projection du dessin
     * @return 4 nombres par segment : x1, y1, x2, y2 (pixels)
     */
    static float[] coordonneesEcran(Trace trace, Projection projection) {
        float[] coordonnees = new float[4 * trace.nbSegments];
        double[] s = trace.segments, a = new double[3];
        for (int i = 0; i < trace.nbSegments; i++) {
            projection.projette(s[6 * i], s[6 * i + 1], s[6 * i + 2], a);
            coordonnees[4 * i] = (float) a[0];
            coordonnees[4 * i + 1] = (float) a[1];
            projection.projette(s[6 * i + 3], s[6 * i + 4], s[6 * i + 5], a);
            coordonnees[4 * i + 2] = (float) a[0];
            coordonnees[4 * i + 3] = (float) a[1];
        }
        return coordonnees;
    }

    /**
     * Retourne la couleur d'un segment ou d'un polygone, comme dans le dessin complet.
     * @return la couleur d'un segment ou d'un polygone, comme dans le dessin complet
     */
    private static Color teinte(Options options, int indiceCouleur, double profondeur) {
        Color base = options.epaisseurCouleur ? PALETTE[indiceCouleur] : options.couleur;
        if (options.vue3D) {
            int tranche = Math.min(NB_TRANCHES - 1, (int) (profondeur * NB_TRANCHES));
            base = base.interpolate(Color.WHITE, 0.55 * (1 - (tranche + 0.5) / NB_TRANCHES));
        }
        return base;
    }

    /**
     * Dessine un seul segment (tortue animée), avec la couleur et l'épaisseur du dessin complet.
     * @param trace la figure
     * @param i le numéro du segment
     * @param projection la projection (calculée une fois pour toute l'animation)
     * @param options la couleur et le mode de vue
     * @param gc le contexte graphique (sans transformation)
     */
    static void dessineSegment(Trace trace, int i, Projection projection, Options options, GraphicsContext gc) {
        double[] s = trace.segments, a = new double[3], b = new double[3];
        projection.projette(s[6 * i], s[6 * i + 1], s[6 * i + 2], a);
        projection.projette(s[6 * i + 3], s[6 * i + 4], s[6 * i + 5], b);
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setStroke(teinte(options, trace.couleurEffective(i), (a[2] + b[2]) / 2));
        gc.setLineWidth(options.epaisseurCouleur ? Math.round(2 * epaisseur(trace, i)) / 2.0 : 1);
        gc.setLineCap(options.epaisseurCouleur ? StrokeLineCap.ROUND : StrokeLineCap.SQUARE);
        gc.strokeLine(a[0], a[1], b[0], b[1]);
    }

    /**
     * Remplit un seul polygone (tortue animée).
     * @param trace la figure
     * @param k le numéro du polygone
     * @param projection la projection
     * @param options la couleur et le mode de vue
     * @param gc le contexte graphique (sans transformation)
     */
    static void remplitPolygone(Trace trace, int k, Projection projection, Options options, GraphicsContext gc) {
        double[] polygone = trace.polygones.get(k), p = new double[3];
        int m = polygone.length / 3;
        double[] xs = new double[m], ys = new double[m];
        double profondeur = 0;
        for (int j = 0; j < m; j++) {
            projection.projette(polygone[3 * j], polygone[3 * j + 1], polygone[3 * j + 2], p);
            xs[j] = p[0];
            ys[j] = p[1];
            profondeur += p[2] / m;
        }
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.setFill(teinte(options, trace.couleurPolygone[k], profondeur));
        gc.fillPolygon(xs, ys, m);
    }
}
