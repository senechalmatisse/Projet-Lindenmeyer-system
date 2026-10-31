package systeme.affichage;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import systeme.lindenmeyer.ExportMaillage;
import systeme.lindenmeyer.ExportSvg;
import systeme.lindenmeyer.Maillage;
import systeme.lindenmeyer.SauvegardeSysteme;
import systeme.lindenmeyer.Trace;
import systeme.lindenmeyer.Vue;

/**
 * Les échanges avec des fichiers (choix du fichier, dialogues, messages) : image PNG, dessin SVG, maillages
 * OBJ et STL, sauvegarde des systèmes ({@code .lsys}).
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class Exports {
    private final Stage stage;
    private final BarreEtat barreEtat;
    private File dossier;

    Exports(Stage stage, BarreEtat barreEtat) {
        this.stage = stage;
        this.barreEtat = barreEtat;
    }

    /**
     * Demande un fichier où enregistrer.
     * @param titre le titre du dialogue
     * @param description la description du filtre (« Maillage OBJ »)
     * @param extension l'extension, sans point (« obj »)
     * @return le fichier choisi (avec l'extension), ou null si l'utilisateur annule
     */
    File choisitFichier(String titre, String description, String extension) {
        FileChooser choix = new FileChooser();
        choix.setTitle(titre);
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + extension));
        choix.setInitialFileName("figure." + extension);
        if (dossier != null && dossier.isDirectory())
            choix.setInitialDirectory(dossier);
        File fichier = choix.showSaveDialog(stage);
        if (fichier == null)
            return null;
        dossier = fichier.getParentFile();
        if (!fichier.getName().toLowerCase().endsWith("." + extension))
            fichier = new File(fichier.getParentFile(), fichier.getName() + "." + extension);
        return fichier;
    }

    /**
     * Demande un fichier à ouvrir.
     * @param titre le titre du dialogue
     * @param description la description du filtre
     * @param extension l'extension, sans point
     * @return le fichier choisi, ou null si l'utilisateur annule
     */
    File choisitFichierAOuvrir(String titre, String description, String extension) {
        FileChooser choix = new FileChooser();
        choix.setTitle(titre);
        choix.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + extension));
        if (dossier != null && dossier.isDirectory())
            choix.setInitialDirectory(dossier);
        File fichier = choix.showOpenDialog(stage);
        if (fichier != null)
            dossier = fichier.getParentFile();
        return fichier;
    }

    /**
     * Affiche une erreur d'export.
     * @param message le message
     */
    void erreur(String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message);
        alerte.setTitle("Erreur");
        alerte.setHeaderText(null);
        alerte.initOwner(stage);
        alerte.showAndWait();
    }

    /**
     * Exporte la figure au format OBJ (pour Blender, par exemple).
     * @param trace la figure
     * @param reglages les réglages du maillage (rayon, épaisseur et couleur)
     */
    void exporteObj(Trace trace, Maillage.Options reglages) {
        Maillage maillage = maillage(trace, reglages);
        if (maillage == null)
            return;
        File fichier = choisitFichier("Exporter la figure (OBJ)", "Maillage OBJ", "obj");
        if (fichier == null)
            return;
        try (Writer sortie = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(fichier)),
                StandardCharsets.US_ASCII)) {
            ExportMaillage.ecritObj(maillage, sortie);
            barreEtat.message("Figure exportée : " + fichier.getName() + " (" + Formats.entier(maillage.nbTriangles)
                + " triangles).");
        } catch (IOException e) {
            erreur("Impossible d'écrire « " + fichier + " » : " + e.getMessage());
        }
    }

    /**
     * Exporte la figure au format STL binaire (impression 3D), après avoir demandé l'échelle.
     * @param trace la figure
     * @param reglages les réglages du maillage (rayon, épaisseur et couleur)
     */
    void exporteStl(Trace trace, Maillage.Options reglages) {
        if (maillage(trace, reglages) == null)
            return;
        // Dialogue : échelle en millimètres par unité, fermeture des prismes et rayon minimal imprimable.
        Dialog<ButtonType> dialogue = new Dialog<>();
        dialogue.initOwner(stage);
        dialogue.setTitle("Exporter la figure (STL)");
        dialogue.setHeaderText("Impression 3D : dimensions de la figure");
        TextField echelle = new TextField("1");
        CheckBox impression = new CheckBox("Fermer les extrémités des branches et imposer un rayon d'au moins 1 mm");
        impression.setSelected(true);
        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(10);
        grille.setPadding(new Insets(10));
        grille.addRow(0, new Label("Échelle (millimètres par unité de longueur) :"), echelle);
        grille.add(impression, 0, 1, 2, 1);
        grille.add(new Label(String.format("Taille avec l'échelle 1 : %.0f × %.0f × %.0f mm (x, y, z).",
            trace.etendueX(), trace.etendueY(), trace.etendueZ())), 0, 2, 2, 1);
        dialogue.getDialogPane().setContent(grille);
        dialogue.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> reponse = dialogue.showAndWait();
        if (!reponse.isPresent() || reponse.get() != ButtonType.OK)
            return;
        double millimetres;
        try {
            millimetres = Double.parseDouble(echelle.getText().trim().replace(',', '.'));
            if (!(millimetres > 0) || Double.isInfinite(millimetres))
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            erreur("L'échelle doit être un nombre strictement positif (par exemple 0.5).");
            return;
        }
        reglages.capuchons = impression.isSelected();
        reglages.rayonMin = impression.isSelected() ? 1 / millimetres : 0;
        Maillage maillage = maillage(trace, reglages);
        File fichier = choisitFichier("Exporter la figure (STL)", "Maillage STL", "stl");
        if (fichier == null)
            return;
        try (OutputStream sortie = new BufferedOutputStream(new FileOutputStream(fichier))) {
            ExportMaillage.ecritStl(maillage, sortie, millimetres);
            barreEtat.message("Figure exportée : " + fichier.getName() + " (" + Formats.entier(maillage.nbTriangles)
                + " triangles).");
        } catch (IOException e) {
            erreur("Impossible d'écrire « " + fichier + " » : " + e.getMessage());
        }
    }

    /**
     * Retourne le maillage de la trace, ou null (avec un message) s'il ne peut pas être construit.
     * @return le maillage de la trace, ou null (avec un message) s'il ne peut pas être construit
     */
    private Maillage maillage(Trace trace, Maillage.Options reglages) {
        if (trace == null || trace.estVide()) {
            erreur("Rien à exporter : générez d'abord une figure.");
            return null;
        }
        try {
            return Maillage.depuis(trace, reglages);
        } catch (IllegalArgumentException e) {
            erreur(e.getMessage() + " Réduisez le nombre d'itérations pour exporter.");
            return null;
        }
    }

    /**
     * Enregistre une image PNG de ce qui est affiché (fond blanc), sans le module {@code javafx.swing} :
     * les pixels sont recopiés un par un dans une image AWT.
     * @param noeud le canvas ou la scène 3D affichés
     */
    void exportePng(Node noeud) {
        File fichier = choisitFichier("Exporter l'image (PNG)", "Image PNG", "png");
        if (fichier == null)
            return;
        SnapshotParameters parametres = new SnapshotParameters();
        parametres.setFill(Color.WHITE);
        WritableImage capture = noeud.snapshot(parametres, null);
        int l = (int) capture.getWidth(), h = (int) capture.getHeight();
        BufferedImage image = new BufferedImage(l, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixels = capture.getPixelReader();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < l; x++)
                image.setRGB(x, y, pixels.getArgb(x, y));
        try {
            if (!ImageIO.write(image, "png", fichier))
                throw new IOException("format PNG indisponible");
            barreEtat.message("Image exportée : " + fichier.getName() + " (" + l + " × " + h + " pixels).");
        } catch (IOException e) {
            erreur("Impossible d'écrire « " + fichier + " » : " + e.getMessage());
        }
    }

    /**
     * Enregistre la figure en SVG, telle qu'elle est projetée à l'écran.
     * @param trace la figure
     * @param vue l'orientation, le zoom et le déplacement
     * @param options la couleur, la vue et le cadrage du dessin
     * @param largeur la largeur de la zone de dessin
     * @param hauteur la hauteur de la zone de dessin
     */
    void exporteSvg(Trace trace, Vue vue, RenduCanvas.Options options, double largeur, double hauteur) {
        if (trace == null || trace.estVide()) {
            erreur("Rien à exporter : générez d'abord une figure.");
            return;
        }
        ExportSvg.Options reglages = new ExportSvg.Options();
        reglages.couleur = ExportSvg.couleur(255 * options.couleur.getRed(), 255 * options.couleur.getGreen(),
            255 * options.couleur.getBlue());
        reglages.vue3D = options.vue3D;
        reglages.ajuster = options.ajuster;
        reglages.epaisseurCouleur = options.epaisseurCouleur;
        String svg;
        try {
            svg = ExportSvg.svg(trace, vue, reglages, largeur, hauteur);
        } catch (IllegalArgumentException e) {
            barreEtat.message(e.getMessage());
            erreur(e.getMessage());
            return;
        }
        File fichier = choisitFichier("Exporter le dessin (SVG)", "Dessin SVG", "svg");
        if (fichier == null)
            return;
        try (Writer sortie = new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8)) {
            sortie.write(svg);
            barreEtat.message("Dessin exporté : " + fichier.getName() + ".");
        } catch (IOException e) {
            erreur("Impossible d'écrire « " + fichier + " » : " + e.getMessage());
        }
    }

    /**
     * Enregistre un système dans un fichier {@code .lsys} (UTF-8).
     * @param systeme les paramètres du système
     */
    void enregistre(SauvegardeSysteme systeme) {
        File fichier = choisitFichier("Enregistrer le système", "Système de Lindenmayer", "lsys");
        if (fichier == null)
            return;
        try (Writer sortie = new OutputStreamWriter(new FileOutputStream(fichier), StandardCharsets.UTF_8)) {
            systeme.ecrit(sortie);
            barreEtat.message("Système enregistré : " + fichier.getName() + ".");
        } catch (IOException e) {
            erreur("Impossible d'écrire « " + fichier + " » : " + e.getMessage());
        }
    }

    /**
     * Lit un système dans un fichier {@code .lsys} choisi par l'utilisateur.
     * @return le système, ou null (annulation ou erreur, déjà signalée)
     */
    SauvegardeSysteme ouvre() {
        File fichier = choisitFichierAOuvrir("Ouvrir un système", "Système de Lindenmayer", "lsys");
        if (fichier == null)
            return null;
        try (Reader entree = new InputStreamReader(new FileInputStream(fichier), StandardCharsets.UTF_8)) {
            SauvegardeSysteme systeme = SauvegardeSysteme.lis(entree);
            barreEtat.message("Système chargé : " + fichier.getName() + " (cliquer sur « Générer système »).");
            return systeme;
        } catch (IOException e) {
            erreur("Impossible de lire « " + fichier + " » : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            erreur("Le fichier « " + fichier.getName() + " » n'est pas un système valide : " + e.getMessage());
        }
        return null;
    }
}
