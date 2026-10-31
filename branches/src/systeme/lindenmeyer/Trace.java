package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * La figure produite par la tortue : des segments 3D et leurs métadonnées, calculés une seule fois
 * puis lus par tous les rendus (Canvas 2D, vue 3D, export, survol).
 * <p>
 * Repère du modèle : x vers la droite, y vers le haut, z vers l'observateur (repère direct).
 * À l'écran, en vue de face, le point (x, y, z) est dessiné en (x, −y).
 * <p>
 * Les champs sont publics pour que les rendus parcourent les tableaux sans copie ;
 * seuls les {@link #nbSegments} premiers éléments de chaque tableau sont significatifs.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Trace {
    /** Nombre de couleurs de la palette (indices 0 à 7, du tronc aux extrémités). */
    public static final int TAILLE_PALETTE = 8;
    /** La palette du mode « épaisseur et couleur », du brun (tronc, indice 0) au vert clair (extrémités, indice 7). */
    public static final List<String> PALETTE = List.of("#5B3A29", "#6B4423", "#7A5230", "#5E7A2A", "#3F8F2F", "#2E8B2E",
        "#55AA3A", "#7CC85A");
    /** Largeur relative minimale (le symbole {@code !} multiplie la largeur par 0,7 sans descendre en dessous). */
    public static final double LARGEUR_MIN = 0.15;

    /** 6 nombres par segment : x1, y1, z1, x2, y2, z2. */
    public double[] segments = new double[6 * 256];
    /** Pour chaque segment, la position dans la chaîne du symbole {@code F} qui l'a tracé (strictement croissante). */
    public int[] indexSymbole = new int[256];
    /** Pour chaque segment, le nombre de crochets ouverts quand il a été tracé (saturé à 127). */
    public byte[] profondeur = new byte[256];
    /** Pour chaque segment, la largeur relative de la tortue (1 au départ, × 0,7 à chaque {@code !}). */
    public float[] largeur = new float[256];
    /** Pour chaque segment, l'indice de couleur de la tortue (0 au départ, + 1 à chaque {@code '}). */
    public byte[] indiceCouleur = new byte[256];
    /** true si la chaîne contient {@code !} : les largeurs explicites l'emportent sur le mode automatique. */
    public boolean largeursExplicites;
    /** true si la chaîne contient {@code '} : les couleurs explicites l'emportent sur le mode automatique. */
    public boolean couleursExplicites;
    /** Les polygones remplis (feuilles) : pour chacun, ses sommets (x, y, z répétés), dans l'ordre. */
    public final List<double[]> polygones = new ArrayList<>();
    /** Pour chaque polygone, l'indice de couleur de la tortue à son ouverture. */
    public int[] couleurPolygone = new int[16];
    /** Pour chaque polygone, la position dans la chaîne de l'accolade fermante qui l'a terminé. */
    public int[] indexPolygone = new int[16];
    /** Le nombre de segments. */
    public int nbSegments;
    /** La profondeur maximale atteinte par un segment. */
    public int profondeurMax;
    /** La boîte englobante (infinie tant que la trace est vide). */
    public double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
                  minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY,
                  minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

    private double rayon = -1;

    /** Crée une trace vide (la tortue y ajoute ses segments et ses polygones). */
    public Trace() {
        super();
    }

    /**
     * Ajoute un segment.
     * @param p le début (x, y, z)
     * @param q la fin (x, y, z)
     * @param indexSymbole la position du symbole {@code F} dans la chaîne
     * @param profondeur le nombre de crochets ouverts
     */
    public void ajoute(double[] p, double[] q, int indexSymbole, int profondeur) {
        ajoute(p, q, indexSymbole, profondeur, 1, 0);
    }

    /**
     * Ajoute un segment avec sa largeur et sa couleur.
     * @param p le début (x, y, z)
     * @param q la fin (x, y, z)
     * @param indexSymbole la position du symbole {@code F} dans la chaîne
     * @param profondeur le nombre de crochets ouverts
     * @param largeur la largeur relative de la tortue
     * @param couleur l'indice de couleur de la tortue
     */
    public void ajoute(double[] p, double[] q, int indexSymbole, int profondeur, double largeur, int couleur) {
        if (nbSegments == this.indexSymbole.length) {
            int capacite = 2 * nbSegments;
            segments = Arrays.copyOf(segments, 6 * capacite);
            this.indexSymbole = Arrays.copyOf(this.indexSymbole, capacite);
            this.profondeur = Arrays.copyOf(this.profondeur, capacite);
            this.largeur = Arrays.copyOf(this.largeur, capacite);
            this.indiceCouleur = Arrays.copyOf(this.indiceCouleur, capacite);
        }
        this.largeur[nbSegments] = (float) largeur;
        this.indiceCouleur[nbSegments] = (byte) Math.max(0, Math.min(TAILLE_PALETTE - 1, couleur));
        int j = 6 * nbSegments;
        segments[j] = p[0];
        segments[j + 1] = p[1];
        segments[j + 2] = p[2];
        segments[j + 3] = q[0];
        segments[j + 4] = q[1];
        segments[j + 5] = q[2];
        this.indexSymbole[nbSegments] = indexSymbole;
        int saturee = Math.min(profondeur, Byte.MAX_VALUE);
        this.profondeur[nbSegments] = (byte) saturee;
        profondeurMax = Math.max(profondeurMax, saturee);
        nbSegments++;
        englobe(p);
        englobe(q);
    }

    /**
     * Agrandit la boîte englobante pour contenir un point.
     * @param p le point (x, y, z)
     */
    void englobe(double[] p) {
        minX = Math.min(minX, p[0]);
        maxX = Math.max(maxX, p[0]);
        minY = Math.min(minY, p[1]);
        maxY = Math.max(maxY, p[1]);
        minZ = Math.min(minZ, p[2]);
        maxZ = Math.max(maxZ, p[2]);
        rayon = -1;
    }

    /**
     * Largeur du mode automatique (« épaisseur selon la profondeur ») : 0,75 à la puissance profondeur,
     * sans descendre sous {@link #LARGEUR_MIN}.
     * @param profondeur le nombre de crochets ouverts
     * @return la largeur relative
     */
    public static double largeurSelonProfondeur(int profondeur) {
        return Math.max(LARGEUR_MIN, Math.pow(0.75, profondeur));
    }

    /**
     * Couleur du mode automatique : la profondeur est répartie sur toute la palette.
     * @param profondeur le nombre de crochets ouverts
     * @param profondeurMax la profondeur maximale de la figure
     * @return l'indice de couleur (0 = tronc, {@link #TAILLE_PALETTE} − 1 = extrémités)
     */
    public static int couleurSelonProfondeur(int profondeur, int profondeurMax) {
        return profondeurMax <= 0 ? 0 : (int) Math.round((double) profondeur / profondeurMax * (TAILLE_PALETTE - 1));
    }

    /**
     * La largeur à utiliser pour un segment : celle des symboles {@code !} s'il y en a, sinon celle du mode automatique.
     * @param i le numéro du segment
     * @return la largeur relative
     */
    public double largeurEffective(int i) {
        return largeursExplicites ? largeur[i] : largeurSelonProfondeur(profondeur[i]);
    }

    /**
     * La couleur à utiliser pour un segment : celle des symboles {@code '} s'il y en a, sinon celle du mode automatique.
     * @param i le numéro du segment
     * @return l'indice de couleur
     */
    public int couleurEffective(int i) {
        return couleursExplicites ? indiceCouleur[i] : couleurSelonProfondeur(profondeur[i], profondeurMax);
    }

    /**
     * Ajoute un polygone rempli.
     * @param sommets les sommets (x, y, z répétés, au moins trois sommets)
     * @param couleur l'indice de couleur
     * @param indexFermeture la position de l'accolade fermante dans la chaîne
     */
    public void ajoutePolygone(double[] sommets, int couleur, int indexFermeture) {
        int k = polygones.size();
        if (k == couleurPolygone.length) {
            couleurPolygone = Arrays.copyOf(couleurPolygone, 2 * k);
            indexPolygone = Arrays.copyOf(indexPolygone, 2 * k);
        }
        polygones.add(sommets);
        couleurPolygone[k] = Math.max(0, Math.min(TAILLE_PALETTE - 1, couleur));
        indexPolygone[k] = indexFermeture;
        double[] p = new double[3];
        for (int j = 0; j < sommets.length; j += 3) {
            p[0] = sommets[j];
            p[1] = sommets[j + 1];
            p[2] = sommets[j + 2];
            englobe(p);
        }
    }

    /**
     * Retourne le nombre de polygones.
     * @return le nombre de polygones
     */
    public int nbPolygones() {
        return polygones.size();
    }

    /**
     * Retourne true si la trace ne contient rien à dessiner (ni segment, ni polygone).
     * @return true si la trace ne contient rien à dessiner (ni segment, ni polygone)
     */
    public boolean estVide() {
        return nbSegments == 0 && polygones.isEmpty();
    }

    /**
     * Retourne la largeur de la boîte englobante (0 si la trace est vide).
     * @return la largeur de la boîte englobante (0 si la trace est vide)
     */
    public double etendueX() {
        return estVide() ? 0 : maxX - minX;
    }

    /**
     * Retourne la hauteur de la boîte englobante (0 si la trace est vide).
     * @return la hauteur de la boîte englobante (0 si la trace est vide)
     */
    public double etendueY() {
        return estVide() ? 0 : maxY - minY;
    }

    /**
     * Retourne la profondeur de la boîte englobante (0 si la trace est vide).
     * @return la profondeur de la boîte englobante (0 si la trace est vide)
     */
    public double etendueZ() {
        return estVide() ? 0 : maxZ - minZ;
    }

    /**
     * Retourne l'abscisse du centre de la boîte englobante.
     * @return l'abscisse du centre de la boîte englobante
     */
    public double centreX() {
        return estVide() ? 0 : (minX + maxX) / 2;
    }

    /**
     * Retourne l'ordonnée du centre de la boîte englobante.
     * @return l'ordonnée du centre de la boîte englobante
     */
    public double centreY() {
        return estVide() ? 0 : (minY + maxY) / 2;
    }

    /**
     * Retourne la cote du centre de la boîte englobante.
     * @return la cote du centre de la boîte englobante
     */
    public double centreZ() {
        return estVide() ? 0 : (minZ + maxZ) / 2;
    }

    /**
     * Indique si la figure sort du plan de l'écran : son étendue en z dépasse 10⁻⁹ fois sa plus grande étendue.
     * Une figure qui reste plane s'ouvre en 2D, même si sa chaîne contient des symboles 3D.
     * @return true si la figure est en 3D
     */
    public boolean est3D() {
        double plusGrande = Math.max(etendueX(), Math.max(etendueY(), etendueZ()));
        return plusGrande > 0 && etendueZ() > 1e-9 * plusGrande;
    }

    /**
     * Calcule (une fois) le rayon de la sphère englobante centrée au centre de la boîte englobante :
     * le cadrage de la vue 3D en dépend, pas de la rotation, donc la figure ne « pulse » pas quand on la tourne.
     * @return le rayon (0 si la trace est vide ou réduite à un point)
     */
    public double rayon() {
        if (rayon < 0) {
            double cx = centreX(), cy = centreY(), cz = centreZ(), max2 = 0;
            for (int j = 0; j < 6 * nbSegments; j += 3) {
                double dx = segments[j] - cx, dy = segments[j + 1] - cy, dz = segments[j + 2] - cz;
                max2 = Math.max(max2, dx * dx + dy * dy + dz * dz);
            }
            for (double[] sommets : polygones)
                for (int j = 0; j < sommets.length; j += 3) {
                    double dx = sommets[j] - cx, dy = sommets[j + 1] - cy, dz = sommets[j + 2] - cz;
                    max2 = Math.max(max2, dx * dx + dy * dy + dz * dz);
                }
            rayon = Math.sqrt(max2);
        }
        return rayon;
    }
}
