package systeme.lindenmeyer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Export d'un {@link Maillage} vers Blender (OBJ) ou une imprimante 3D (STL binaire), sans JavaFX.
 * Les nombres sont toujours écrits avec un point décimal ({@code Locale.ROOT}) : sous une locale française,
 * une virgule rendrait le fichier illisible.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class ExportMaillage {
    private ExportMaillage() {
    }

    /**
     * Écrit le maillage au format OBJ (texte) : une ligne {@code v x y z} par sommet (y vers le haut, comme le
     * modèle), puis une ligne {@code f i j k} par triangle (indices à partir de 1).
     * @param maillage le maillage
     * @param sortie la destination (le texte est en ASCII)
     * @throws IOException si l'écriture échoue
     */
    public static void ecritObj(Maillage maillage, Writer sortie) throws IOException {
        sortie.write("# Systeme de Lindenmayer : " + maillage.nbSommets + " sommets et " + maillage.nbTriangles + " triangles\n");
        sortie.write("o figure\n");
        float[] v = maillage.sommets;
        for (int i = 0; i < maillage.nbSommets; i++)
            sortie.write(String.format(Locale.ROOT, "v %.6f %.6f %.6f\n", v[3 * i], v[3 * i + 1], v[3 * i + 2]));
        int[] t = maillage.triangles;
        for (int i = 0; i < maillage.nbTriangles; i++)
            sortie.write("f " + (t[3 * i] + 1) + " " + (t[3 * i + 1] + 1) + " " + (t[3 * i + 2] + 1) + "\n");
        sortie.flush();
    }

    /**
     * Écrit le maillage au format STL binaire : 80 octets d'en-tête, le nombre de triangles (entier sur 4 octets),
     * puis 50 octets par triangle (normale, trois sommets, 2 octets nuls), en petit-boutiste.
     * Les trancheurs d'impression 3D considèrent que z est vertical : le modèle (y vers le haut) est donc
     * tourné de 90° autour de x, (x, y, z) ↦ (x, −z, y), puis mis à l'échelle.
     * @param maillage le maillage
     * @param sortie la destination
     * @param millimetresParUnite l'échelle (millimètres par unité du modèle)
     * @throws IOException si l'écriture échoue
     */
    public static void ecritStl(Maillage maillage, OutputStream sortie, double millimetresParUnite) throws IOException {
        ByteBuffer tampon = ByteBuffer.allocate(84 + 50 * maillage.nbTriangles).order(ByteOrder.LITTLE_ENDIAN);
        byte[] entete = new byte[80];
        byte[] texte = "Systeme de Lindenmayer (STL binaire, millimetres)".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(texte, 0, entete, 0, texte.length);
        tampon.put(entete);
        tampon.putInt(maillage.nbTriangles);
        float[] v = maillage.sommets;
        int[] t = maillage.triangles;
        double[][] p = new double[3][3];
        for (int i = 0; i < maillage.nbTriangles; i++) {
            for (int a = 0; a < 3; a++) {
                int j = 3 * t[3 * i + a];
                p[a][0] = v[j] * millimetresParUnite;
                p[a][1] = -v[j + 2] * millimetresParUnite;
                p[a][2] = v[j + 1] * millimetresParUnite;
            }
            double[] n = normale(p[0], p[1], p[2]);
            tampon.putFloat((float) n[0]).putFloat((float) n[1]).putFloat((float) n[2]);
            for (double[] sommet : p)
                tampon.putFloat((float) sommet[0]).putFloat((float) sommet[1]).putFloat((float) sommet[2]);
            tampon.putShort((short) 0);
        }
        sortie.write(tampon.array());
        sortie.flush();
    }

    /**
     * Calcule la normale unitaire d'un triangle : normalise((v2 − v1) × (v3 − v1)).
     * @param a le premier sommet
     * @param b le deuxième sommet
     * @param c le troisième sommet
     * @return la normale (nulle si le triangle est dégénéré)
     */
    public static double[] normale(double[] a, double[] b, double[] c) {
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double norme = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return norme == 0 ? new double[3] : new double[] {nx / norme, ny / norme, nz / norme};
    }
}
