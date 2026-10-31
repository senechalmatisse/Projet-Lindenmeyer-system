package systeme.affichage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import systeme.lindenmeyer.Maillage;
import systeme.lindenmeyer.Projection;
import systeme.lindenmeyer.Trace;
import systeme.lindenmeyer.Vue;

/**
 * La zone de dessin : un canvas (vues 2D et 3D en traits), un calque transparent au-dessus (segment survolé,
 * tortue animée) et, si la machine le permet, une scène JavaFX 3D (vue « volumes ») ; le canvas ou la scène est
 * visible, jamais les deux. Elle suit la taille de la fenêtre et réagit à la souris :
 * <ul>
 * <li>vue 3D ou volumes : glisser fait tourner la figure ; glisser avec le bouton droit (ou Maj) la déplace ;</li>
 * <li>vue 2D : glisser déplace la figure ;</li>
 * <li>molette : zoom centré sur le curseur ; double-clic : retour à la vue de face, sans zoom ;</li>
 * <li>survol (jusqu'à 100 000 segments, hors volumes) : le segment le plus proche (à moins de 6 pixels)
 *     est surligné et signalé à {@link #survolSegment}.</li>
 * </ul>
 * Pendant un glisser, une figure de plus de 50 000 segments est dessinée de façon allégée,
 * puis entièrement au relâchement.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class ZoneDessin extends Pane {
    /** Les modes de vue. */
    enum Mode { VUE_2D, VUE_3D, VOLUMES }

    /** Au-delà de ce nombre de segments, le maillage est construit en arrière-plan. */
    private static final int SEGMENTS_MAILLAGE_SYNCHRONE = 10_000;
    /** Au-delà, le survol est désactivé ; au-delà de 20 000, une grille accélère la recherche. */
    static final int MAX_SEGMENTS_SURVOL = 100_000;
    private static final int SEGMENTS_SANS_GRILLE = 20_000, TAILLE_GRILLE = 64;
    private static final double DISTANCE_SURVOL = 6;

    final Canvas canvas = new Canvas();
    /** Le calque transparent : segment surligné et tortue animée. */
    final Canvas calque = new Canvas();
    /** L'orientation, le zoom et le déplacement de la figure. */
    final Vue vue = new Vue();
    /** La couleur, le mode de vue et le cadrage. */
    final RenduCanvas.Options options = new RenduCanvas.Options();
    /** Rayon d'un segment du tronc dans la vue « volumes » (0,1 × la longueur d'un pas). */
    double rayonBase = 1;
    /** Vitesse de la tortue animée, en segments par seconde. */
    double vitesseAnimation = 100;
    /** Reçoit les messages à afficher dans la barre d'état (maillage désactivé, en construction…). */
    Consumer<String> messages = message -> { };
    /** Appelé quand la souris survole un segment (son numéro) ou n'en survole plus (−1). */
    IntConsumer survolSegment = segment -> { };
    /** Appelé à chaque pas de la tortue animée (numéro du module du dernier segment tracé, −1 à la fin). */
    IntConsumer avancementAnimation = module -> { };

    private final RenduCanvas rendu = new RenduCanvas();
    private RenduScene3D scene3D;
    private Mode mode = Mode.VUE_2D;
    private Trace trace;
    private Maillage maillage;
    /** Numéro de la dernière demande de maillage (un maillage calculé pour une figure périmée est ignoré). */
    private long demandeMaillage = 0;
    private boolean redessinPlanifie = false;
    private double sourisX, sourisY;
    private boolean glisse = false;

    /** Coordonnées à l'écran des segments et grille de recherche (calculées au premier survol après un dessin). */
    private float[] ecran;
    private List<List<Integer>> grille;
    private int segmentSurligne = -1;

    /** Tortue animée : en cours, en pause (tracé partiel) ou arrêtée (figure complète). */
    private enum Animation { ARRETEE, EN_COURS, EN_PAUSE }
    private Animation animation = Animation.ARRETEE;
    private final AnimationTimer minuteur = new AnimationTimer() {
        @Override
        public void handle(long maintenant) {
            pasAnimation(maintenant);
        }
    };
    private int nbAffiches, polygonesAffiches;
    private long instantPrecedent;
    private double reserve;
    private Projection projectionAnimation;

    ZoneDessin() {
        calque.setMouseTransparent(true);
        getChildren().addAll(canvas, calque);
        setStyle("-fx-background-color: white;");
        setMinSize(0, 0);
        // Les canvas suivent la taille de la zone ; la figure est recentrée à chaque changement.
        for (Canvas c : new Canvas[] {canvas, calque}) {
            c.widthProperty().bind(widthProperty());
            c.heightProperty().bind(heightProperty());
        }
        canvas.widthProperty().addListener(observable -> planifieRedessin());
        canvas.heightProperty().addListener(observable -> planifieRedessin());

        setCursor(Cursor.OPEN_HAND);
        setOnMousePressed(this::presse);
        setOnMouseDragged(this::glisse);
        setOnMouseReleased(this::relache);
        setOnMouseClicked(this::clique);
        setOnScroll(this::molette);
        setOnMouseMoved(evenement -> survole(evenement.getX(), evenement.getY()));
        setOnMouseExited(evenement -> {
            if (segmentSurligne >= 0) {
                surligneSegment(-1);
                survolSegment.accept(-1);
            }
        });
    }

    /**
     * Retourne true si la vue « volumes » est possible sur cette machine.
     * @return true si la vue « volumes » est possible sur cette machine
     */
    static boolean volumesDisponibles() {
        return RenduScene3D.estDisponible();
    }

    /**
     * Change de mode de vue (sans changer l'orientation ni le zoom, sans redessiner).
     * @param nouveau le mode
     */
    void regleMode(Mode nouveau) {
        if (nouveau == Mode.VOLUMES && !volumesDisponibles())
            nouveau = Mode.VUE_3D;
        mode = nouveau;
        options.vue3D = mode != Mode.VUE_2D;
        if (mode == Mode.VOLUMES && scene3D == null) {
            scene3D = new RenduScene3D();
            scene3D.sousScene.widthProperty().bind(widthProperty());
            scene3D.sousScene.heightProperty().bind(heightProperty());
            getChildren().add(1, scene3D.sousScene);
        }
        maillage = null;
    }

    /**
     * Retourne le mode de vue.
     * @return le mode de vue
     */
    Mode mode() {
        return mode;
    }

    /**
     * Affiche une nouvelle figure (sans changer la vue).
     * @param trace la figure, ou null pour effacer
     */
    void affiche(Trace trace) {
        this.trace = trace;
        maillage = null;
        redessine();
    }

    /**
     * Retourne la figure affichée, ou null.
     * @return la figure affichée, ou null
     */
    Trace trace() {
        return trace;
    }

    /** Efface la figure et revient à la vue initiale. */
    void efface() {
        vue.reinitialise();
        affiche(null);
    }

    /** Le maillage doit être reconstruit (épaisseur, couleur ou rayon changés), puis la figure redessinée. */
    void maillagePerime() {
        maillage = null;
        redessine();
    }

    /**
     * Retourne true si la figure est affichée en volumes (sinon : canvas).
     * @return true si la figure est affichée en volumes (sinon : canvas)
     */
    boolean enVolumes() {
        return mode == Mode.VOLUMES && trace != null && !trace.estVide() && trace.nbSegments <= Maillage.MAX_SEGMENTS;
    }

    /**
     * Retourne la scène 3D (pour une capture d'écran), ou null.
     * @return la scène 3D (pour une capture d'écran), ou null
     */
    RenduScene3D scene3D() {
        return scene3D;
    }

    /** Redessine immédiatement (la tortue animée s'arrête et la figure complète est dessinée). */
    void redessine() {
        arreteAnimation();
        ecran = null;
        grille = null;
        efface(calque);
        segmentSurligne = -1;
        boolean volumes = enVolumes();
        if (mode == Mode.VOLUMES && trace != null && trace.nbSegments > Maillage.MAX_SEGMENTS)
            messages.accept("Maillage 3D désactivé : " + Formats.entier(trace.nbSegments) + " segments > "
                + Formats.entier(Maillage.MAX_SEGMENTS) + " (vue 3D en traits).");
        canvas.setVisible(!volumes);
        calque.setVisible(!volumes);
        if (scene3D != null)
            scene3D.sousScene.setVisible(volumes);
        if (!volumes) {
            rendu.dessine(trace, vue, options, canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight());
            return;
        }
        if (maillage == null)
            construitMaillage();
        scene3D.appliqueVue(vue, getHeight());
    }

    private static void efface(Canvas c) {
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, c.getWidth(), c.getHeight());
    }

    /**
     * Retourne les réglages du maillage pour la figure courante (prismes fermés : pas de trou aux jonctions).
     * @return les réglages du maillage pour la figure courante (prismes fermés : pas de trou aux jonctions)
     */
    Maillage.Options optionsMaillage() {
        Maillage.Options reglages = new Maillage.Options();
        reglages.rayonBase = rayonBase;
        reglages.epaisseurCouleur = options.epaisseurCouleur;
        reglages.capuchons = true;
        return reglages;
    }

    /** Construit le maillage de la figure courante (en arrière-plan au-delà de 10 000 segments). */
    private void construitMaillage() {
        final Trace figure = trace;
        final Maillage.Options reglages = optionsMaillage();
        final long demande = ++demandeMaillage;
        if (figure.nbSegments <= SEGMENTS_MAILLAGE_SYNCHRONE) {
            installeMaillage(Maillage.depuis(figure, reglages), figure);
            return;
        }
        scene3D.affiche(null, null, null);
        messages.accept("Construction du maillage 3D (" + Formats.entier(figure.nbSegments) + " segments)…");
        Task<Maillage> tache = new Task<Maillage>() {
            @Override
            protected Maillage call() {
                return Maillage.depuis(figure, reglages);
            }
        };
        tache.setOnSucceeded(evenement -> {
            if (demande == demandeMaillage && figure == trace && mode == Mode.VOLUMES) {
                installeMaillage(tache.getValue(), figure);
                messages.accept("");
            }
        });
        Thread fil = new Thread(tache, "maillage");
        fil.setDaemon(true);
        fil.start();
    }

    private void installeMaillage(Maillage nouveau, Trace figure) {
        maillage = nouveau;
        scene3D.affiche(maillage, figure, options.epaisseurCouleur ? null : options.couleur);
        scene3D.appliqueVue(vue, getHeight());
    }

    /** Demande un redessin, en regroupant les demandes successives (redimensionnement, souris). */
    void planifieRedessin() {
        if (redessinPlanifie)
            return;
        redessinPlanifie = true;
        Platform.runLater(() -> {
            redessinPlanifie = false;
            redessine();
        });
    }

    // ----- Survol -----

    /**
     * Retourne true si le survol est possible (figure affichée sur le canvas, pas trop grande, pas d'animation).
     * @return true si le survol est possible (figure affichée sur le canvas, pas trop grande, pas d'animation)
     */
    boolean survolPossible() {
        return trace != null && trace.nbSegments > 0 && trace.nbSegments <= MAX_SEGMENTS_SURVOL && !enVolumes()
            && animation == Animation.ARRETEE;
    }

    private void survole(double x, double y) {
        if (!survolPossible() || options.apercu)
            return;
        int segment = segmentProche(x, y);
        if (segment != segmentSurligne) {
            surligneSegment(segment);
            survolSegment.accept(segment);
        }
    }

    /**
     * Retourne le numéro du segment le plus proche du point (à moins de 6 pixels), ou −1.
     * @return le numéro du segment le plus proche du point (à moins de 6 pixels), ou −1
     */
    private int segmentProche(double x, double y) {
        if (ecran == null)
            ecran = RenduCanvas.coordonneesEcran(trace, projection());
        int n = trace.nbSegments, meilleur = -1;
        double meilleureDistance = DISTANCE_SURVOL;
        if (n <= SEGMENTS_SANS_GRILLE) {
            for (int i = 0; i < n; i++) {
                double d = distance(x, y, i);
                if (d <= meilleureDistance) {
                    meilleureDistance = d;
                    meilleur = i;
                }
            }
            return meilleur;
        }
        if (grille == null)
            construitGrille();
        int cx = numeroCase(x, getWidth()), cy = numeroCase(y, getHeight());
        if (cx < 0 || cy < 0)
            return -1;
        for (int i : grille.get(cy * TAILLE_GRILLE + cx)) {
            double d = distance(x, y, i);
            if (d <= meilleureDistance) {
                meilleureDistance = d;
                meilleur = i;
            }
        }
        return meilleur;
    }

    private static int numeroCase(double v, double taille) {
        int c = (int) Math.floor(v / Math.max(1, taille) * TAILLE_GRILLE);
        return c < 0 || c >= TAILLE_GRILLE ? -1 : c;
    }

    /** Grille uniforme 64 × 64 : chaque case liste les segments dont la boîte (élargie de 6 pixels) la touche. */
    private void construitGrille() {
        grille = new ArrayList<>(TAILLE_GRILLE * TAILLE_GRILLE);
        for (int c = 0; c < TAILLE_GRILLE * TAILLE_GRILLE; c++)
            grille.add(new ArrayList<>());
        double l = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        for (int i = 0; i < trace.nbSegments; i++) {
            double x1 = Math.min(ecran[4 * i], ecran[4 * i + 2]) - DISTANCE_SURVOL;
            double x2 = Math.max(ecran[4 * i], ecran[4 * i + 2]) + DISTANCE_SURVOL;
            double y1 = Math.min(ecran[4 * i + 1], ecran[4 * i + 3]) - DISTANCE_SURVOL;
            double y2 = Math.max(ecran[4 * i + 1], ecran[4 * i + 3]) + DISTANCE_SURVOL;
            int cx1 = Math.max(0, (int) Math.floor(x1 / l * TAILLE_GRILLE));
            int cx2 = Math.min(TAILLE_GRILLE - 1, (int) Math.floor(x2 / l * TAILLE_GRILLE));
            int cy1 = Math.max(0, (int) Math.floor(y1 / h * TAILLE_GRILLE));
            int cy2 = Math.min(TAILLE_GRILLE - 1, (int) Math.floor(y2 / h * TAILLE_GRILLE));
            for (int cy = cy1; cy <= cy2; cy++)
                for (int cx = cx1; cx <= cx2; cx++)
                    grille.get(cy * TAILLE_GRILLE + cx).add(i);
        }
    }

    /**
     * Retourne la distance (pixels) du point au segment i.
     * @return la distance (pixels) du point au segment i
     */
    private double distance(double x, double y, int i) {
        double x1 = ecran[4 * i], y1 = ecran[4 * i + 1], x2 = ecran[4 * i + 2], y2 = ecran[4 * i + 3];
        double dx = x2 - x1, dy = y2 - y1, longueur2 = dx * dx + dy * dy;
        double t = longueur2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - x1) * dx + (y - y1) * dy) / longueur2));
        return Math.hypot(x - (x1 + t * dx), y - (y1 + t * dy));
    }

    private Projection projection() {
        return Projection.pour(trace, vue, canvas.getWidth(), canvas.getHeight(), options.vue3D, options.ajuster);
    }

    /**
     * Surligne un segment sur le calque (en rouge, 3 pixels), sans redessiner la figure.
     * @param segment le numéro du segment, ou −1 pour effacer
     */
    void surligneSegment(int segment) {
        segmentSurligne = segment;
        efface(calque);
        if (segment < 0 || trace == null || segment >= trace.nbSegments || enVolumes() || animation != Animation.ARRETEE)
            return;
        if (ecran == null)
            ecran = RenduCanvas.coordonneesEcran(trace, projection());
        GraphicsContext gc = calque.getGraphicsContext2D();
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);
        gc.strokeLine(ecran[4 * segment], ecran[4 * segment + 1], ecran[4 * segment + 2], ecran[4 * segment + 3]);
    }

    // ----- Tortue animée -----

    /**
     * Retourne true si la tortue animée est en cours.
     * @return true si la tortue animée est en cours
     */
    boolean animationEnCours() {
        return animation == Animation.EN_COURS;
    }

    /**
     * Lance (ou reprend) la tortue animée : seuls les nouveaux segments sont dessinés à chaque image, avec un
     * cadrage calculé une fois sur la figure complète. Elle s'arrête à la fin du tracé.
     */
    void lanceAnimation() {
        if (trace == null || trace.estVide() || enVolumes() || animation == Animation.EN_COURS)
            return;
        if (animation == Animation.ARRETEE) {
            nbAffiches = 0;
            polygonesAffiches = 0;
            efface(canvas);
        }
        efface(calque);
        segmentSurligne = -1;
        projectionAnimation = projection();
        animation = Animation.EN_COURS;
        instantPrecedent = 0;
        reserve = 0;
        minuteur.start();
    }

    /** Met la tortue animée en pause (le tracé partiel reste affiché). */
    void pauseAnimation() {
        if (animation == Animation.EN_COURS) {
            minuteur.stop();
            animation = Animation.EN_PAUSE;
        }
    }

    /** Revient au début du tracé : la zone de dessin est vidée, prête pour la tortue animée. */
    void debutAnimation() {
        if (trace == null || trace.estVide() || enVolumes())
            return;
        minuteur.stop();
        animation = Animation.EN_PAUSE;
        nbAffiches = 0;
        polygonesAffiches = 0;
        projectionAnimation = projection();
        efface(canvas);
        efface(calque);
        avancementAnimation.accept(-1);
    }

    private void arreteAnimation() {
        if (animation != Animation.ARRETEE) {
            minuteur.stop();
            animation = Animation.ARRETEE;
            avancementAnimation.accept(-1);
        }
    }

    private void pasAnimation(long maintenant) {
        if (instantPrecedent == 0) {
            instantPrecedent = maintenant;
            return;
        }
        reserve += vitesseAnimation * (maintenant - instantPrecedent) / 1e9;
        instantPrecedent = maintenant;
        int nombre = (int) Math.min(reserve, Integer.MAX_VALUE);
        if (nombre == 0)
            return;
        reserve -= nombre;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        int fin = (int) Math.min(trace.nbSegments, (long) nbAffiches + nombre);
        for (int i = nbAffiches; i < fin; i++) {
            // Les polygones fermés avant ce segment sont remplis d'abord.
            while (polygonesAffiches < trace.nbPolygones()
                   && trace.indexPolygone[polygonesAffiches] < trace.indexSymbole[i])
                RenduCanvas.remplitPolygone(trace, polygonesAffiches++, projectionAnimation, options, gc);
            RenduCanvas.dessineSegment(trace, i, projectionAnimation, options, gc);
        }
        nbAffiches = fin;
        if (nbAffiches >= trace.nbSegments) {
            // Fin du tracé : dessin complet (polygones restants, ordre de profondeur).
            redessine();
            return;
        }
        dessineTortue(fin - 1);
        avancementAnimation.accept(trace.indexSymbole[fin - 1]);
    }

    /** Dessine la tortue (un petit triangle) au bout du segment i, tournée dans le sens du tracé. */
    private void dessineTortue(int i) {
        double[] s = trace.segments, a = new double[3], b = new double[3];
        projectionAnimation.projette(s[6 * i], s[6 * i + 1], s[6 * i + 2], a);
        projectionAnimation.projette(s[6 * i + 3], s[6 * i + 4], s[6 * i + 5], b);
        double dx = b[0] - a[0], dy = b[1] - a[1], norme = Math.hypot(dx, dy);
        if (norme == 0) {
            dx = 0;
            dy = -1;
        } else {
            dx /= norme;
            dy /= norme;
        }
        efface(calque);
        GraphicsContext gc = calque.getGraphicsContext2D();
        gc.setFill(Color.web("#1b5e20"));
        gc.fillPolygon(new double[] {b[0] + 8 * dx, b[0] - 5 * dx - 5 * dy, b[0] - 5 * dx + 5 * dy},
            new double[] {b[1] + 8 * dy, b[1] - 5 * dy + 5 * dx, b[1] - 5 * dy - 5 * dx}, 3);
    }

    // ----- Souris -----

    private void presse(MouseEvent e) {
        sourisX = e.getX();
        sourisY = e.getY();
        glisse = false;
    }

    private void glisse(MouseEvent e) {
        double dx = e.getX() - sourisX, dy = e.getY() - sourisY;
        sourisX = e.getX();
        sourisY = e.getY();
        if (trace == null)
            return;
        glisse = true;
        setCursor(Cursor.CLOSED_HAND);
        boolean deplace = mode == Mode.VUE_2D || e.getButton() == MouseButton.SECONDARY || e.isShiftDown();
        if (deplace) {
            vue.panX += dx;
            vue.panY += dy;
        } else {
            vue.tourne(dx, dy);
        }
        options.apercu = true;
        planifieRedessin();
    }

    private void relache(MouseEvent e) {
        setCursor(Cursor.OPEN_HAND);
        if (options.apercu) {
            options.apercu = false;
            planifieRedessin();
        }
    }

    private void clique(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !glisse) {
            vue.reinitialise();
            redessine();
        }
    }

    private void molette(ScrollEvent e) {
        if (trace == null || e.getDeltaY() == 0)
            return;
        vue.zoome(Math.pow(1.1, e.getDeltaY() / 40), e.getX() - getWidth() / 2, e.getY() - getHeight() / 2);
        planifieRedessin();
    }
}
