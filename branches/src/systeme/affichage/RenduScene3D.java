package systeme.affichage;

import java.util.Arrays;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import systeme.lindenmeyer.Maillage;
import systeme.lindenmeyer.Trace;
import systeme.lindenmeyer.Vue;

/**
 * Rendu volumétrique d'une figure dans une scène JavaFX 3D : les branches sont des prismes éclairés,
 * la profondeur est gérée par un tampon de profondeur, la caméra tourne autour de la figure.
 * <p>
 * Le modèle (y vers le haut, z vers l'observateur) est converti dans le repère de JavaFX (y vers le bas,
 * z vers l'écran) par (x, y, z) ↦ (x, −y, −z), une rotation de 180° autour de x qui conserve le sens des faces.
 * L'orientation de la {@link Vue} (lacet φ, tangage θ) devient une rotation de θ autour de x et de −φ autour
 * de y : la figure est vue sous le même angle que dans la vue 3D du canvas.
 * <p>
 * Les maillages sont répartis en paquets d'au plus 60 000 sommets ({@link MeshView} séparés), un paquet par
 * couleur ; jamais un objet par segment.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class RenduScene3D {
    /** Nombre maximal de sommets par {@link MeshView} (prudence vis-à-vis des index sur 16 bits). */
    static final int MAX_SOMMETS_PAR_VUE = 60_000;
    /** Demi-angle du champ vertical de la caméra (le champ par défaut de JavaFX est de 30°). */
    private static final double DEMI_CHAMP = Math.toRadians(15);

    final SubScene sousScene;
    private final Group maillages = new Group();
    private final Rotate rotationX = new Rotate(0, Rotate.X_AXIS), rotationY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate centre = new Translate();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final PointLight lumiere = new PointLight(Color.WHITE);
    private double rayon = 1;

    RenduScene3D() {
        Group modele = new Group(maillages);
        modele.getTransforms().addAll(rotationX, rotationY, centre);
        Group racine = new Group(modele, new AmbientLight(Color.gray(0.4)), lumiere, camera);
        sousScene = new SubScene(racine, 1, 1, true, SceneAntialiasing.BALANCED);
        sousScene.setFill(Color.WHITE);
        sousScene.setCamera(camera);
    }

    /**
     * Retourne true si la machine sait afficher une scène 3D (sinon on reste sur la vue 3D du canvas).
     * @return true si la machine sait afficher une scène 3D (sinon on reste sur la vue 3D du canvas)
     */
    static boolean estDisponible() {
        return Platform.isSupported(ConditionalFeature.SCENE3D);
    }

    /**
     * Remplace la figure affichée (les anciens maillages sont retirés, pas accumulés).
     * @param maillage le maillage (null : scène vide)
     * @param trace la figure (son centre et son rayon fixent le cadrage)
     * @param couleur la couleur de tout le maillage, ou null pour la palette (une couleur par indice)
     */
    void affiche(Maillage maillage, Trace trace, Color couleur) {
        maillages.getChildren().clear();
        if (maillage == null || trace == null)
            return;
        rayon = Math.max(trace.rayon(), 1e-9);
        // Dans le repère de JavaFX, le centre (cx, cy, cz) du modèle devient (cx, −cy, −cz).
        centre.setX(-trace.centreX());
        centre.setY(trace.centreY());
        centre.setZ(trace.centreZ());
        int nbCouleurs = couleur == null ? Trace.TAILLE_PALETTE : 1;
        for (int c = 0; c < nbCouleurs; c++) {
            Color teinte = couleur == null ? RenduCanvas.PALETTE[c] : couleur;
            ajoutePaquets(maillage, 0, maillage.nbTrianglesPrismes, couleur == null ? c : -1, teinte, true);
            ajoutePaquets(maillage, maillage.nbTrianglesPrismes, maillage.nbTriangles, couleur == null ? c : -1,
                teinte, false);
        }
    }

    /**
     * Ajoute les triangles [debut, fin) de la couleur demandée, en paquets d'au plus {@link #MAX_SOMMETS_PAR_VUE} sommets.
     * @param indiceCouleur l'indice de couleur à garder, ou −1 pour tous les triangles
     * @param prismes true pour les prismes (fermés : faces arrière cachées), false pour les polygones (deux faces)
     */
    private void ajoutePaquets(Maillage maillage, int debut, int fin, int indiceCouleur, Color teinte, boolean prismes) {
        int[] local = new int[maillage.nbSommets];
        Arrays.fill(local, -1);
        float[] points = new float[3 * MAX_SOMMETS_PAR_VUE];
        int[] faces = new int[6 * MAX_SOMMETS_PAR_VUE];
        int[] utilises = new int[MAX_SOMMETS_PAR_VUE];
        int[] lissage = new int[faces.length / 6];
        int nbPoints = 0, nbFaces = 0;
        PhongMaterial matiere = new PhongMaterial(teinte);
        matiere.setSpecularColor(Color.gray(0.75));
        for (int t = debut; t < fin; t++) {
            if (indiceCouleur >= 0 && maillage.couleurTriangle[t] != indiceCouleur)
                continue;
            if (nbPoints + 3 > MAX_SOMMETS_PAR_VUE || 6 * (nbFaces + 1) > faces.length) {
                ajouteVue(points, nbPoints, faces, lissage, nbFaces, matiere, prismes);
                for (int i = 0; i < nbPoints; i++)
                    local[utilises[i]] = -1;
                nbPoints = 0;
                nbFaces = 0;
            }
            for (int a = 0; a < 3; a++) {
                int global = maillage.triangles[3 * t + a];
                if (local[global] < 0) {
                    local[global] = nbPoints;
                    utilises[nbPoints] = global;
                    // Repère de JavaFX : (x, y, z) ↦ (x, −y, −z).
                    points[3 * nbPoints] = maillage.sommets[3 * global];
                    points[3 * nbPoints + 1] = -maillage.sommets[3 * global + 1];
                    points[3 * nbPoints + 2] = -maillage.sommets[3 * global + 2];
                    nbPoints++;
                }
                faces[6 * nbFaces + 2 * a] = local[global];
                faces[6 * nbFaces + 2 * a + 1] = 0;
            }
            lissage[nbFaces] = maillage.flanc[t];
            nbFaces++;
        }
        ajouteVue(points, nbPoints, faces, lissage, nbFaces, matiere, prismes);
    }

    private void ajouteVue(float[] points, int nbPoints, int[] faces, int[] lissage, int nbFaces, PhongMaterial matiere,
                           boolean prismes) {
        if (nbFaces == 0)
            return;
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(points, 0, 3 * nbPoints);
        mesh.getTexCoords().setAll(0, 0);
        mesh.getFaces().setAll(faces, 0, 6 * nbFaces);
        // Les flancs des prismes sont lissés (ils paraissent arrondis) ; les capuchons et les polygones restent plats.
        mesh.getFaceSmoothingGroups().setAll(lissage, 0, nbFaces);
        MeshView vue = new MeshView(mesh);
        vue.setMaterial(matiere);
        vue.setCullFace(prismes ? CullFace.BACK : CullFace.NONE);
        maillages.getChildren().add(vue);
    }

    /**
     * Oriente la figure et place la caméra selon la vue (mêmes gestes que la vue 3D du canvas).
     * @param vue l'orientation, le zoom (distance de la caméra) et le déplacement
     * @param hauteur la hauteur de la zone de dessin (pour convertir le déplacement en unités du modèle)
     */
    void appliqueVue(Vue vue, double hauteur) {
        rotationX.setAngle(vue.pitch);
        rotationY.setAngle(-vue.yaw);
        // Recul tel que la sphère englobante tienne dans le champ vertical, avec 10 % de marge.
        double distance = 1.1 * rayon / Math.sin(DEMI_CHAMP) / vue.zoom;
        double unitesParPixel = 2 * distance * Math.tan(DEMI_CHAMP) / Math.max(1, hauteur);
        camera.setTranslateX(-vue.panX * unitesParPixel);
        camera.setTranslateY(-vue.panY * unitesParPixel);
        camera.setTranslateZ(-distance);
        camera.setNearClip(Math.max(1e-3, distance / 1000));
        camera.setFarClip(distance + 20 * rayon);
        // La lumière accompagne la caméra, un peu au-dessus et à gauche.
        lumiere.setTranslateX(camera.getTranslateX() - rayon);
        lumiere.setTranslateY(camera.getTranslateY() - rayon);
        lumiere.setTranslateZ(-distance);
    }
}
