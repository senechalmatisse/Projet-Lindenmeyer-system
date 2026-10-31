package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Export de la figure affichée en SVG (sans JavaFX) : la projection courante (vue 2D ou 3D), avec les mêmes
 * couleurs, épaisseurs et tranches de profondeur que le dessin à l'écran.
 * <p>
 * Les segments consécutifs (la fin de l'un est le début du suivant) sont fusionnés en un seul chemin
 * {@code M … L … L …} ; il y a un {@code <path>} par groupe (tranche, couleur, épaisseur). Les coordonnées ont deux
 * décimales et un point décimal ({@code Locale.ROOT}), quelle que soit la langue du système.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class ExportSvg {
    /** Au-delà, l'export est refusé (le fichier pèserait des dizaines de mégaoctets). */
    public static final int MAX_SEGMENTS = 200_000;
    private static final int NB_TRANCHES = 8;

    /** Les réglages de l'export (les mêmes que ceux du dessin à l'écran). */
    public static final class Options {
        /** Couleur du trait simple, au format #rrggbb. */
        public String couleur = "#a52a2a";
        /** true pour la vue 3D (projection et tranches de profondeur), false pour la vue 2D de face. */
        public boolean vue3D;
        /** true pour remplir la zone de dessin, false pour garder la taille réelle. */
        public boolean ajuster = true;
        /** true pour l'épaisseur et la couleur de chaque segment (palette {@link Trace#PALETTE}). */
        public boolean epaisseurCouleur;

        /** Des réglages par défaut : vue 2D ajustée, trait simple brun. */
        public Options() {
            super();
        }
    }

    private ExportSvg() {
    }

    /**
     * Écrit la figure en SVG.
     * @param trace la figure
     * @param vue l'orientation, le zoom et le déplacement
     * @param options la couleur, la vue et le cadrage
     * @param largeur la largeur de la zone de dessin (pixels)
     * @param hauteur la hauteur de la zone de dessin (pixels)
     * @return le document SVG
     * @throws IllegalArgumentException si la figure a plus de {@link #MAX_SEGMENTS} segments
     */
    public static String svg(Trace trace, Vue vue, Options options, double largeur, double hauteur) {
        if (trace.nbSegments > MAX_SEGMENTS)
            throw new IllegalArgumentException("Export SVG refusé : " + trace.nbSegments + " segments > " + MAX_SEGMENTS
                + " (réduisez le nombre d'itérations).");
        Projection projection = Projection.pour(trace, vue, largeur, hauteur, options.vue3D, options.ajuster);
        // Groupes de segments : clé = (tranche, épaisseur décroissante, couleur), dans l'ordre de tracé.
        Map<Long, List<Integer>> groupes = new TreeMap<>();
        double[] ecran = new double[4 * trace.nbSegments], a = new double[3], b = new double[3];
        double[] s = trace.segments;
        for (int i = 0; i < trace.nbSegments; i++) {
            int j = 6 * i;
            projection.projette(s[j], s[j + 1], s[j + 2], a);
            projection.projette(s[j + 3], s[j + 4], s[j + 5], b);
            ecran[4 * i] = a[0];
            ecran[4 * i + 1] = a[1];
            ecran[4 * i + 2] = b[0];
            ecran[4 * i + 3] = b[1];
            int tranche = tranche(options, (a[2] + b[2]) / 2);
            long demiPixels = options.epaisseurCouleur
                ? Math.round(2 * Math.max(0.5, 2.5 * trace.largeurEffective(i))) : 2;
            int couleur = options.epaisseurCouleur ? trace.couleurEffective(i) : 0;
            long cle = ((long) tranche * 1000 + (999 - Math.min(999, demiPixels))) * 16 + couleur;
            groupes.computeIfAbsent(cle, c -> new ArrayList<>()).add(i);
        }
        // Polygones : sommets projetés et tranche de leur centre.
        List<List<double[]>> polygonesParTranche = new ArrayList<>();
        for (int t = 0; t < NB_TRANCHES; t++)
            polygonesParTranche.add(new ArrayList<>());
        List<Integer> couleursPolygones = new ArrayList<>();
        for (int k = 0; k < trace.nbPolygones(); k++) {
            double[] polygone = trace.polygones.get(k), points = new double[2 * (polygone.length / 3) + 1];
            double profondeur = 0;
            for (int m = 0; m < polygone.length / 3; m++) {
                projection.projette(polygone[3 * m], polygone[3 * m + 1], polygone[3 * m + 2], a);
                points[2 * m] = a[0];
                points[2 * m + 1] = a[1];
                profondeur += a[2] / (polygone.length / 3);
            }
            points[points.length - 1] = k;
            polygonesParTranche.get(tranche(options, profondeur)).add(points);
            couleursPolygones.add(trace.couleurPolygone[k]);
        }

        StringBuilder svg = new StringBuilder();
        String l = nombre(largeur), h = nombre(hauteur);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(l).append(' ').append(h)
            .append("\" width=\"").append(l).append("\" height=\"").append(h).append("\">\n");
        svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n");
        int tranchePolygones = -1;
        for (Map.Entry<Long, List<Integer>> groupe : groupes.entrySet()) {
            long cle = groupe.getKey();
            int couleur = (int) (cle % 16), tranche = (int) (cle / 16 / 1000);
            double epaisseur = (999 - (cle / 16) % 1000) / 2.0;
            for (; tranchePolygones < tranche; tranchePolygones++)
                ecritPolygones(svg, polygonesParTranche.get(tranchePolygones + 1), trace, options, tranchePolygones + 1);
            svg.append("  <path d=\"");
            double xPrecedent = Double.NaN, yPrecedent = Double.NaN;
            for (int i : groupe.getValue()) {
                double x1 = ecran[4 * i], y1 = ecran[4 * i + 1], x2 = ecran[4 * i + 2], y2 = ecran[4 * i + 3];
                if (!(Math.abs(x1 - xPrecedent) < 1e-6 && Math.abs(y1 - yPrecedent) < 1e-6))
                    svg.append(svg.charAt(svg.length() - 1) == '"' ? "" : " ").append("M ").append(nombre(x1)).append(' ')
                        .append(nombre(y1));
                svg.append(" L ").append(nombre(x2)).append(' ').append(nombre(y2));
                xPrecedent = x2;
                yPrecedent = y2;
            }
            String teinte = options.epaisseurCouleur ? Trace.PALETTE.get(couleur) : options.couleur;
            svg.append("\" stroke=\"").append(ombre(teinte, options, tranche)).append("\" stroke-width=\"")
                .append(nombre(options.epaisseurCouleur ? epaisseur : 1)).append("\" fill=\"none\"")
                .append(options.epaisseurCouleur ? " stroke-linecap=\"round\" stroke-linejoin=\"round\"" : "")
                .append("/>\n");
        }
        for (; tranchePolygones < NB_TRANCHES - 1; tranchePolygones++)
            ecritPolygones(svg, polygonesParTranche.get(tranchePolygones + 1), trace, options, tranchePolygones + 1);
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static int tranche(Options options, double profondeur) {
        return options.vue3D ? Math.min(NB_TRANCHES - 1, (int) (profondeur * NB_TRANCHES)) : 0;
    }

    private static void ecritPolygones(StringBuilder svg, List<double[]> polygones, Trace trace, Options options, int tranche) {
        for (double[] points : polygones) {
            int k = (int) points[points.length - 1];
            svg.append("  <path d=\"");
            for (int m = 0; m < (points.length - 1) / 2; m++)
                svg.append(m == 0 ? "M " : " L ").append(nombre(points[2 * m])).append(' ').append(nombre(points[2 * m + 1]));
            String teinte = options.epaisseurCouleur ? Trace.PALETTE.get(trace.couleurPolygone[k]) : options.couleur;
            svg.append(" Z\" fill=\"").append(ombre(teinte, options, tranche)).append("\" stroke=\"none\"/>\n");
        }
    }

    /** Éclaircit la couleur des tranches lointaines en vue 3D (comme à l'écran). */
    private static String ombre(String couleur, Options options, int tranche) {
        if (!options.vue3D)
            return couleur.toLowerCase(Locale.ROOT);
        double blanc = 0.55 * (1 - (tranche + 0.5) / NB_TRANCHES);
        int rgb = Integer.parseInt(couleur.substring(1), 16);
        int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
        return couleur(r + (255 - r) * blanc, g + (255 - g) * blanc, b + (255 - b) * blanc);
    }

    /**
     * Écrit une couleur au format #rrggbb.
     * @param rouge la composante rouge (0 à 255)
     * @param vert la composante verte (0 à 255)
     * @param bleu la composante bleue (0 à 255)
     * @return la couleur, par exemple « #a52a2a »
     */
    public static String couleur(double rouge, double vert, double bleu) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", Math.round(rouge), Math.round(vert), Math.round(bleu));
    }

    private static String nombre(double x) {
        return String.format(Locale.ROOT, "%.2f", x);
    }
}
