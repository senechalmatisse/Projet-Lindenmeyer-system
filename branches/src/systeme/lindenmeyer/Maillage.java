package systeme.lindenmeyer;

/**
 * Un maillage de triangles construit à partir d'une {@link Trace} (sans JavaFX) : chaque segment devient un
 * prisme à k facettes, chaque polygone un éventail de triangles. Il sert à la scène 3D et aux exports OBJ / STL.
 * <p>
 * Pour un segment (P, Q) de rayon r :
 * <pre>
 * w = normalise(Q − P)
 * a = (0,1,0) ; si |w·a| &gt; 0,99 alors a = (1,0,0)
 * u = normalise(w × a) ; v = w × u                  (u, v, w forment un repère direct)
 * pour i = 0..k−1 : t = 2π·i/k ; o = cos t·u + sin t·v
 *     bas[i] = P + r·o ; haut[i] = Q + r·o
 * pour i = 0..k−1, j = (i+1) mod k :
 *     triangle (bas[i], bas[j], haut[i]) ; triangle (bas[j], haut[j], haut[i])
 * </pre>
 * Les triangles sont orientés vers l'extérieur (sens direct vu de dehors). Par segment : 2k sommets et 2k triangles,
 * plus 2(k − 2) triangles si les extrémités sont fermées (capuchons, pour l'impression 3D).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Maillage {
    /** Au-delà de ce nombre de segments, on ne construit pas de maillage (≈ 600 000 triangles à k = 6). */
    public static final int MAX_SEGMENTS = 50_000;

    /** Les réglages du maillage. */
    public static final class Options {
        /** Nombre de facettes de chaque prisme (3 à 8). */
        public int facettes = 6;
        /** Rayon d'un segment du tronc (profondeur 0, largeur 1), en unités du modèle. */
        public double rayonBase = 1;
        /** Sans épaisseur : le rayon est multiplié par ce facteur à chaque niveau de branche. */
        public double facteurRayon = 0.8;
        /** true : rayon selon la largeur de la tortue (symboles {@code !} ou mode automatique) et couleur de palette. */
        public boolean epaisseurCouleur;
        /** true : ferme les deux extrémités de chaque prisme (impression 3D). */
        public boolean capuchons;
        /** Rayon minimal (unités du modèle), par exemple pour qu'une branche reste imprimable ; 0 : aucun. */
        public double rayonMin;

        /** Des réglages par défaut : 6 facettes, rayon 1, sans capuchons. */
        public Options() {
            super();
        }
    }

    /** Coordonnées des sommets (x, y, z répétés), dans le repère du modèle. */
    public float[] sommets;
    /** Indices des sommets de chaque triangle (3 par triangle). */
    public int[] triangles;
    /** Indice de couleur de chaque triangle (0 sans le mode épaisseur et couleur). */
    public byte[] couleurTriangle;
    /** Pour chaque triangle, 1 s'il appartient au flanc d'un prisme (à lisser), 0 sinon (capuchon, polygone). */
    public byte[] flanc;
    /** Nombre de sommets et de triangles. */
    public int nbSommets, nbTriangles;
    /** Nombre de triangles des prismes : ils viennent en premier, suivis de ceux des polygones. */
    public int nbTrianglesPrismes;

    private Maillage(int sommetsMax, int trianglesMax) {
        sommets = new float[3 * sommetsMax];
        triangles = new int[3 * trianglesMax];
        couleurTriangle = new byte[trianglesMax];
        flanc = new byte[trianglesMax];
    }

    /**
     * Construit le maillage d'une trace.
     * @param trace la figure
     * @param options les réglages
     * @return le maillage
     * @throws IllegalArgumentException si la trace a plus de {@link #MAX_SEGMENTS} segments
     */
    public static Maillage depuis(Trace trace, Options options) {
        if (trace.nbSegments > MAX_SEGMENTS)
            throw new IllegalArgumentException("Maillage 3D désactivé : " + trace.nbSegments + " segments > "
                + MAX_SEGMENTS + ".");
        int k = Math.max(3, Math.min(8, options.facettes));
        int parSegment = 2 * k + (options.capuchons ? 2 * (k - 2) : 0);
        int sommetsMax = 2 * k * trace.nbSegments, trianglesMax = parSegment * trace.nbSegments;
        for (double[] polygone : trace.polygones) {
            sommetsMax += polygone.length / 3;
            trianglesMax += polygone.length / 3 - 2;
        }
        Maillage maillage = new Maillage(sommetsMax, trianglesMax);
        double[] s = trace.segments;
        double rayonMinimal = Math.max(options.rayonMin, 0.15 * options.rayonBase);
        for (int i = 0; i < trace.nbSegments; i++) {
            double rayon = options.epaisseurCouleur ? options.rayonBase * trace.largeurEffective(i)
                : options.rayonBase * Math.pow(options.facteurRayon, trace.profondeur[i]);
            rayon = Math.max(rayon, options.epaisseurCouleur ? options.rayonMin : rayonMinimal);
            int couleur = options.epaisseurCouleur ? trace.couleurEffective(i) : 0;
            maillage.prisme(s, 6 * i, rayon, k, options.capuchons, couleur);
        }
        maillage.nbTrianglesPrismes = maillage.nbTriangles;
        for (int j = 0; j < trace.nbPolygones(); j++)
            maillage.eventail(trace.polygones.get(j), options.epaisseurCouleur ? trace.couleurPolygone[j] : 0);
        return maillage;
    }

    private int sommet(double x, double y, double z) {
        sommets[3 * nbSommets] = (float) x;
        sommets[3 * nbSommets + 1] = (float) y;
        sommets[3 * nbSommets + 2] = (float) z;
        return nbSommets++;
    }

    private void triangle(int a, int b, int c, int couleur, boolean deFlanc) {
        triangles[3 * nbTriangles] = a;
        triangles[3 * nbTriangles + 1] = b;
        triangles[3 * nbTriangles + 2] = c;
        couleurTriangle[nbTriangles] = (byte) couleur;
        flanc[nbTriangles] = (byte) (deFlanc ? 1 : 0);
        nbTriangles++;
    }

    /** Ajoute le prisme du segment qui commence à s[j] (un segment de longueur nulle est ignoré). */
    private void prisme(double[] s, int j, double r, int k, boolean capuchons, int couleur) {
        double px = s[j], py = s[j + 1], pz = s[j + 2];
        double wx = s[j + 3] - px, wy = s[j + 4] - py, wz = s[j + 5] - pz;
        double longueur = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (longueur == 0)
            return;
        wx /= longueur;
        wy /= longueur;
        wz /= longueur;
        // a = (0,1,0), ou (1,0,0) si le segment est presque vertical ; u = w × a normalisé ; v = w × u.
        double ax = 0, ay = 1, az = 0;
        if (Math.abs(wy) > 0.99) {
            ax = 1;
            ay = 0;
        }
        double ux = wy * az - wz * ay, uy = wz * ax - wx * az, uz = wx * ay - wy * ax;
        double nu = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= nu;
        uy /= nu;
        uz /= nu;
        double vx = wy * uz - wz * uy, vy = wz * ux - wx * uz, vz = wx * uy - wy * ux;
        int bas = nbSommets, haut = nbSommets + k;
        double qx = s[j + 3], qy = s[j + 4], qz = s[j + 5];
        for (int i = 0; i < k; i++) {
            double t = 2 * Math.PI * i / k, c = Math.cos(t), si = Math.sin(t);
            double ox = r * (c * ux + si * vx), oy = r * (c * uy + si * vy), oz = r * (c * uz + si * vz);
            sommet(px + ox, py + oy, pz + oz);
            int b = 3 * (haut + i);
            sommets[b] = (float) (qx + ox);
            sommets[b + 1] = (float) (qy + oy);
            sommets[b + 2] = (float) (qz + oz);
        }
        nbSommets += k;
        for (int i = 0; i < k; i++) {
            int suivant = (i + 1) % k;
            triangle(bas + i, bas + suivant, haut + i, couleur, true);
            triangle(bas + suivant, haut + suivant, haut + i, couleur, true);
        }
        if (capuchons)
            for (int i = 1; i < k - 1; i++) {
                triangle(bas, bas + i + 1, bas + i, couleur, false);
                triangle(haut, haut + i, haut + i + 1, couleur, false);
            }
    }

    /** Ajoute un polygone, découpé en éventail depuis son premier sommet (polygone supposé convexe). */
    private void eventail(double[] polygone, int couleur) {
        int premier = nbSommets;
        for (int j = 0; j < polygone.length; j += 3)
            sommet(polygone[j], polygone[j + 1], polygone[j + 2]);
        for (int i = 1; i < polygone.length / 3 - 1; i++)
            triangle(premier, premier + i, premier + i + 1, couleur, false);
    }
}
