package systeme.affichage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import systeme.lindenmeyer.*;

/**
 * Une classe qui sert à l'affichage du système de Lindenmayer choisi.
 * Les exemples du parcours (fichier {@code exemples.txt}) remplissent les champs, qui restent modifiables,
 * et sont dessinés aussitôt ; le type de système (déterministe, stochastique ou contextuel) se choisit séparément.
 * <p>
 * Après « Générer système », toutes les générations (de l'axiome à la dernière itération) restent
 * disponibles : le curseur du panneau « Chaîne générée » choisit celle qui est dessinée. La figure peut
 * être vue de face (2D), en 3D (rotation à la souris) ou en volumes ; le curseur d'angle la redessine sans
 * régénérer. Survoler un symbole de la chaîne surligne son segment, et inversement ; la tortue animée rejoue
 * le tracé.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public class AffichageSysteme extends Application {
    private static final String TITRE = "Système de Lindenmayer";
    private static final String[] NOMS_COULEURS = {"Marron", "Vert", "Bleu", "Rouge"};
    private static final Color[] COULEURS = {Color.BROWN, Color.GREEN, Color.BLUE, Color.RED};
    private static final String STYLE_ERREUR = "-fx-border-color: red; -fx-border-width: 1.5;";
    /** Bornes du curseur d'angle (degrés). */
    private static final double ANGLE_MAX = 180;

    /**
     * Les TextField pour changer les paramètres du système.
     * Les listes déroulantes pour choisir l'exemple, le type de système et la couleur.
     * La zone de dessin.
     */
    private TextField iterationsRentrees, angleRentre, longueurRentree, axiomesRentres, reglesRentrees, graineRentree;
    private ComboBox<Exemple> listeExemples;
    private ComboBox<TypeSysteme> listeTypes;
    private ComboBox<String> listeCouleurs;
    private TextFlow descriptionExemple;
    private Slider curseurAngle;
    private CheckBox caseAjuster, casePerspective, caseEpaisseurCouleur;
    private RadioButton choixVue2D, choixVue3D, choixVolumes;
    private MenuItem menuObj, menuStl, menuPng, menuSvg;
    private Exports exports;
    private Button nouveauTirageBouton;
    private ZoneDessin zoneDessin;
    private Stage stage;
    private PanneauChaine panneauChaine;
    private TableRegles tableRegles;
    private BarreEtat barreEtat;
    private Tooltip infobulleRegles;

    /** Le dernier système généré et ses générations, pour changer de génération ou redessiner sans régénérer. */
    private SystemeLindenmeyer systemeCourant;
    private SystemeLindenmeyer.Derivation derivation;
    /** La génération affichée et ses modules (null si elle n'est pas dessinable). */
    private int generationCourante;
    private Mot motCourant;
    private int longueurCourante;
    /** L'angle du dessin : celui du système généré, puis celui du curseur d'angle. */
    private double angleCourant;
    private boolean reinterpretationPlanifiee = false;
    /** Le dernier message d'erreur de génération affiché (effacé quand une génération dessinable est choisie). */
    private String messageGeneration = "";
    /** Vrai pendant que le programme règle lui-même un contrôle (les écouteurs ne doivent alors rien faire). */
    private boolean reglageCurseur = false, synchroAngle = false, remplissage = false, reglageVue = false;

    /** Crée l'application (la fenêtre est construite par {@link #start(Stage)}). */
    public AffichageSysteme() {
        super();
    }

    /**
     * Lit un entier dans un champ.
     * @param champ le champ à lire
     * @param nom le nom du paramètre, pour le message d'erreur
     * @param minimum la plus petite valeur acceptée
     * @return l'entier saisi
     * @throws IllegalArgumentException si le champ ne contient pas un entier supérieur ou égal à minimum
     */
    private int lisEntier(TextField champ, String nom, int minimum) {
        try {
            int valeur = Integer.parseInt(champ.getText().trim());
            if (valeur >= minimum)
                return valeur;
        } catch (NumberFormatException e) {
            // Le message d'erreur est levé ci-dessous.
        }
        throw new IllegalArgumentException(nom + " doit être un entier supérieur ou égal à " + minimum + ".");
    }

    /**
     * Lit un angle (en degrés), avec un point ou une virgule comme séparateur décimal.
     * @param texte le texte saisi
     * @return l'angle, ou null si le texte n'est pas un nombre
     */
    private static Double angleDe(String texte) {
        try {
            double valeur = Double.parseDouble(texte.trim().replace(',', '.'));
            return Double.isFinite(valeur) ? valeur : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Lit l'angle (en degrés) du champ « Angle ».
     * @return l'angle saisi
     * @throws IllegalArgumentException si le champ ne contient pas un nombre
     */
    private double lisAngle() {
        Double valeur = angleDe(angleRentre.getText());
        if (valeur == null)
            throw new IllegalArgumentException("L'angle doit être un nombre en degrés (par exemple 25.7).");
        return valeur;
    }

    /**
     * Retourne true si le type choisi utilise le hasard (et donc une graine).
     * @return true si le type choisi utilise le hasard (et donc une graine)
     */
    private boolean typeAleatoire() {
        return listeTypes.getValue().estAleatoire();
    }

    /**
     * Lit la graine du générateur aléatoire : un champ vide donne une graine au hasard.
     * @return la graine, ou null si le type choisi n'utilise pas le hasard
     * @throws IllegalArgumentException si le champ ne contient pas un entier
     */
    private Long lisGraine() {
        if (!typeAleatoire())
            return null;
        String texte = graineRentree.getText().trim();
        if (texte.isEmpty())
            return System.nanoTime();
        try {
            return Long.parseLong(texte);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La graine doit être un nombre entier (par exemple 42), "
                + "ou rester vide pour un tirage au hasard.");
        }
    }

    /**
     * Construit un système du type choisi.
     * @param graine la graine (types aléatoires), ou null pour un tirage au hasard
     * @throws IllegalArgumentException si une règle est invalide pour ce type de système
     */
    private SystemeLindenmeyer creeSysteme(String axiomes, String regles, double angle, int iterations, int longueur,
                                           Long graine) {
        return listeTypes.getValue().cree(axiomes, regles, angle, iterations, longueur, graine);
    }

    /**
     * Choisit un exemple : remplit les champs avec ses paramètres, affiche sa description et le dessine.
     * @param exemple l'exemple choisi (« Système personnalisé » : seule la description est effacée)
     */
    private void choisitExemple(Exemple exemple) {
        afficheDescription(exemple);
        if (exemple == null || exemple.type() == null)
            return;
        remplissage = true;
        listeTypes.setValue(exemple.type());
        axiomesRentres.setText(exemple.axiome());
        reglesRentrees.setText(exemple.regles());
        angleRentre.setText(Formats.nombre(exemple.angle()));
        iterationsRentrees.setText(String.valueOf(exemple.iterations()));
        longueurRentree.setText(String.valueOf(exemple.longueur()));
        graineRentree.clear();
        remplissage = false;
        genere();
    }

    /**
     * Affiche la description d'un exemple, avec ce qu'il faut observer et ce qu'on peut essayer.
     * @param exemple l'exemple (null ou sans description : la zone est masquée)
     */
    private void afficheDescription(Exemple exemple) {
        descriptionExemple.getChildren().clear();
        if (exemple != null && exemple.type() != null) {
            Text nom = new Text(exemple.nom());
            nom.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
            descriptionExemple.getChildren().add(nom);
            String[][] parties = {{"", exemple.description()}, {"À observer : ", exemple.observer()},
                {"À essayer : ", exemple.essayer()}};
            for (String[] partie : parties) {
                if (partie[1].isEmpty())
                    continue;
                if (!descriptionExemple.getChildren().isEmpty())
                    descriptionExemple.getChildren().add(new Text("\n"));
                if (!partie[0].isEmpty()) {
                    Text titre = new Text(partie[0]);
                    titre.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
                    descriptionExemple.getChildren().add(titre);
                }
                descriptionExemple.getChildren().add(new Text(partie[1]));
            }
        }
        boolean visible = !descriptionExemple.getChildren().isEmpty();
        descriptionExemple.setVisible(visible);
        descriptionExemple.setManaged(visible);
    }

    /**
     * Passe à l'exemple précédent ou suivant de la liste (et le dessine).
     * @param sens −1 pour le précédent, +1 pour le suivant
     */
    private void exempleVoisin(int sens) {
        int n = listeExemples.getItems().size();
        int i = listeExemples.getSelectionModel().getSelectedIndex() + sens;
        // L'entrée 0 (« Système personnalisé ») est sautée : on boucle sur les exemples 1 à n − 1.
        if (i < 1)
            i = n - 1;
        else if (i >= n)
            i = 1;
        listeExemples.getSelectionModel().select(i);
    }

    /**
     * Affiche un message d'erreur.
     * @param message le message à afficher
     */
    private void afficheErreur(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur génération système");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Applique la couleur choisie et redessine. */
    private void couleurChange() {
        zoneDessin.options.couleur = COULEURS[listeCouleurs.getSelectionModel().getSelectedIndex()];
        zoneDessin.maillagePerime();
    }

    /**
     * Choisit le mode de vue (réinitialise l'orientation, le zoom et le déplacement) et redessine.
     * @param mode 2D, 3D en traits ou volumes
     */
    private void choisitVue(ZoneDessin.Mode mode) {
        regleVue(mode);
        zoneDessin.redessine();
    }

    /**
     * Règle le mode de vue sans redessiner (réinitialise l'orientation, le zoom et le déplacement).
     * @param mode 2D, 3D en traits ou volumes
     */
    private void regleVue(ZoneDessin.Mode mode) {
        reglageVue = true;
        (mode == ZoneDessin.Mode.VUE_2D ? choixVue2D : mode == ZoneDessin.Mode.VUE_3D ? choixVue3D : choixVolumes)
            .setSelected(true);
        reglageVue = false;
        casePerspective.setDisable(mode != ZoneDessin.Mode.VUE_3D);
        zoneDessin.vue.reinitialise();
        zoneDessin.regleMode(mode);
    }

    /**
     * Retourne le mode de vue choisi par les boutons radio.
     * @return le mode de vue choisi par les boutons radio
     */
    private ZoneDessin.Mode vueChoisie() {
        return choixVolumes.isSelected() ? ZoneDessin.Mode.VOLUMES
            : choixVue3D.isSelected() ? ZoneDessin.Mode.VUE_3D : ZoneDessin.Mode.VUE_2D;
    }

    /**
     * Active ou désactive le mode « épaisseur et couleur » (sans redessiner) ; la liste des couleurs ne sert
     * qu'au trait simple.
     * @param actif true pour dessiner chaque segment avec sa largeur et sa couleur de palette
     */
    private void regleEpaisseurCouleur(boolean actif) {
        caseEpaisseurCouleur.setSelected(actif);
        zoneDessin.options.epaisseurCouleur = actif;
        listeCouleurs.setDisable(actif);
    }

    /** Met à jour les menus qui dépendent de la figure affichée (exports). */
    private void metAJourMenus() {
        Trace trace = zoneDessin.trace();
        boolean maillable = trace != null && !trace.estVide() && trace.nbSegments <= Maillage.MAX_SEGMENTS;
        menuObj.setDisable(!maillable);
        menuStl.setDisable(!maillable);
        menuPng.setDisable(trace == null || trace.estVide());
        menuSvg.setDisable(trace == null || trace.estVide());
    }

    /** Génère le système décrit par les champs et dessine sa dernière génération, ou affiche l'erreur à corriger. */
    private void genere() {
        try {
            int iterations = lisEntier(iterationsRentrees, "Le nombre d'itérations", 0);
            int longueur = lisEntier(longueurRentree, "La longueur", 1);
            double angle = lisAngle();
            if (axiomesRentres.getText().trim().isEmpty())
                throw new IllegalArgumentException("L'axiome est vide.");
            Long graine = lisGraine();
            long debut = System.nanoTime();
            SystemeLindenmeyer systeme = creeSysteme(axiomesRentres.getText(), reglesRentrees.getText(), angle,
                iterations, longueur, graine);
            // La provenance des règles (petites générations seulement) sert à colorer la chaîne.
            SystemeLindenmeyer.Derivation nouvelle = systeme.derive().avecProvenance(true);
            for (int i = 0; i < iterations; i++)
                nouvelle.avance();
            Mot mot = nouvelle.mot(iterations);
            // L'interprétation a lieu avant de remplacer la figure courante : en cas d'erreur, rien ne change.
            Trace trace = systeme.interprete(mot, longueur, angle);
            long duree = System.nanoTime() - debut;

            systemeCourant = systeme;
            derivation = nouvelle;
            longueurCourante = longueur;
            angleCourant = angle;
            generationCourante = iterations;
            motCourant = mot;
            if (graine != null)
                graineRentree.setText(String.valueOf(graine));
            stage.setTitle(TITRE + " - " + listeTypes.getValue().nom());
            barreEtat.message("");
            messageGeneration = "";
            regleCurseur(iterations);
            // Une figure qui sort du plan s'ouvre en vue 3D (ou en volumes si ce mode est choisi),
            // une figure plane en vue 2D (modifiable).
            zoneDessin.rayonBase = 0.1 * longueur;
            ZoneDessin.Mode mode = !trace.est3D() ? ZoneDessin.Mode.VUE_2D
                : vueChoisie() == ZoneDessin.Mode.VOLUMES ? ZoneDessin.Mode.VOLUMES : ZoneDessin.Mode.VUE_3D;
            // Une chaîne qui contient ! ou ' s'affiche avec ses épaisseurs et ses couleurs (modifiable).
            if (trace.largeursExplicites || trace.couleursExplicites)
                regleEpaisseurCouleur(true);
            regleVue(mode);
            zoneDessin.affiche(trace);
            metAJourInfos(duree, null);
        } catch (IllegalArgumentException e) {
            afficheErreur(e.getMessage());
        }
    }

    /** Place le curseur des générations sans redessiner. */
    private void regleCurseur(int valeur) {
        reglageCurseur = true;
        panneauChaine.regleCurseur(derivation.nbGenerations() - 1, valeur);
        reglageCurseur = false;
    }

    /**
     * Interprète et dessine la génération courante avec l'angle courant.
     * Une génération qui n'est plus conservée ou qui n'est pas dessinable est signalée dans la barre d'état.
     */
    private void dessineGeneration() {
        int k = generationCourante;
        if (!derivation.estStockee(k)) {
            motCourant = null;
            zoneDessin.affiche(null);
            metAJourInfos(-1, "La génération " + k + " (" + Formats.entier(derivation.longueur(k))
                + " symboles) n'est plus conservée en mémoire : choisissez une génération plus récente ou plus ancienne.");
            return;
        }
        Mot mot = derivation.mot(k);
        long debut = System.nanoTime();
        String erreur = null;
        try {
            Trace trace = systemeCourant.interprete(mot, longueurCourante, angleCourant);
            motCourant = mot;
            zoneDessin.affiche(trace);
        } catch (IllegalArgumentException e) {
            motCourant = null;
            zoneDessin.affiche(null);
            erreur = "Génération " + k + " non dessinable : " + e.getMessage();
        }
        metAJourInfos(System.nanoTime() - debut, erreur);
    }

    /**
     * Affiche une autre génération de la dérivation courante, sans régénérer.
     * @param k le numéro de la génération
     */
    private void choisitGeneration(int k) {
        if (derivation == null || k < 0 || k >= derivation.nbGenerations())
            return;
        generationCourante = k;
        dessineGeneration();
    }

    /** Bouton « génération suivante » : avance d'un pas, en calculant une nouvelle génération si besoin. */
    private void generationSuivante() {
        if (derivation == null)
            return;
        int derniere = derivation.nbGenerations() - 1;
        if (generationCourante < derniere) {
            panneauChaine.curseurIteration.setValue(generationCourante + 1);
            return;
        }
        try {
            derivation.avance();
        } catch (IllegalArgumentException e) {
            afficheErreur(e.getMessage());
            return;
        }
        regleCurseur(derniere + 1);
        choisitGeneration(derniere + 1);
    }

    /** Bouton « génération précédente ». */
    private void generationPrecedente() {
        if (derivation != null && generationCourante > 0)
            panneauChaine.curseurIteration.setValue(generationCourante - 1);
    }

    /**
     * L'angle a changé (curseur ou champ) : la chaîne courante est réinterprétée, sans régénérer.
     * @param angle le nouvel angle
     */
    private void angleChange(double angle) {
        if (remplissage || derivation == null)
            return;
        angleCourant = angle;
        if (reinterpretationPlanifiee)
            return;
        reinterpretationPlanifiee = true;
        Platform.runLater(() -> {
            reinterpretationPlanifiee = false;
            if (derivation != null)
                dessineGeneration();
        });
    }

    /**
     * Retourne le texte de chaque règle du système courant, dans l'ordre (pour la légende des couleurs).
     * @return le texte de chaque règle du système courant, dans l'ordre (pour la légende des couleurs)
     */
    private List<String> textesRegles() {
        List<String> textes = new ArrayList<>();
        if (systemeCourant instanceof SystemeParametrique)
            for (RegleParametrique regle : ((SystemeParametrique) systemeCourant).getReglesParametriques())
                textes.add(regle.toString());
        else if (systemeCourant != null)
            for (Regle regle : systemeCourant.getReglesAnalysees())
                textes.add(regle.toString());
        return textes;
    }

    /**
     * La souris survole un module de la chaîne : son segment est surligné sur le dessin (si c'est un F dessiné).
     * @param module le numéro du module, ou −1
     */
    private void moduleSurvole(int module) {
        Trace trace = zoneDessin.trace();
        if (module < 0 || trace == null || motCourant == null || !zoneDessin.survolPossible()
            || motCourant.symbole(module) != 'F') {
            zoneDessin.surligneSegment(-1);
            return;
        }
        int segment = Arrays.binarySearch(trace.indexSymbole, 0, trace.nbSegments, module);
        zoneDessin.surligneSegment(segment >= 0 ? segment : -1);
    }

    /**
     * La souris survole un segment du dessin : son symbole F est surligné dans la chaîne.
     * @param segment le numéro du segment, ou −1
     */
    private void segmentSurvole(int segment) {
        Trace trace = zoneDessin.trace();
        panneauChaine.surligneModule(segment < 0 || trace == null ? -1 : trace.indexSymbole[segment]);
    }

    /** Lance la tortue animée (en vue 3D en traits si la figure était affichée en volumes). */
    private void lanceAnimation() {
        if (zoneDessin.trace() == null)
            return;
        if (zoneDessin.enVolumes())
            choisitVue(ZoneDessin.Mode.VUE_3D);
        zoneDessin.vitesseAnimation = panneauChaine.vitesse();
        zoneDessin.lanceAnimation();
    }

    /** Copie toute la chaîne de la génération affichée dans le presse-papiers. */
    private void copieChaine() {
        if (derivation == null || !derivation.estStockee(generationCourante))
            return;
        ClipboardContent contenu = new ClipboardContent();
        contenu.putString(derivation.chaine(generationCourante));
        Clipboard.getSystemClipboard().setContent(contenu);
        barreEtat.message("Chaîne de la génération " + generationCourante + " copiée ("
            + Formats.entier(derivation.longueur(generationCourante)) + " symboles).");
    }

    /**
     * Rassemble les paramètres saisis (pour les enregistrer ou les copier).
     * @return les paramètres
     * @throws IllegalArgumentException si un nombre est mal saisi
     */
    private SauvegardeSysteme parametresSaisis() {
        String graine = graineRentree.getText().trim();
        Long valeurGraine = null;
        if (typeAleatoire() && !graine.isEmpty())
            valeurGraine = lisGraine();
        return new SauvegardeSysteme(listeTypes.getValue(), axiomesRentres.getText(), reglesRentrees.getText(),
            lisAngle(), lisEntier(iterationsRentrees, "Le nombre d'itérations", 0),
            lisEntier(longueurRentree, "La longueur", 1), valeurGraine);
    }

    /**
     * Remplit les champs avec un système chargé ou collé, sans le générer.
     * @param systeme les paramètres
     */
    private void remplitAvec(SauvegardeSysteme systeme) {
        remplissage = true;
        listeExemples.getSelectionModel().select(0);
        afficheDescription(null);
        listeTypes.setValue(systeme.type);
        axiomesRentres.setText(systeme.axiome);
        reglesRentrees.setText(systeme.regles);
        angleRentre.setText(Formats.nombre(systeme.angle));
        iterationsRentrees.setText(String.valueOf(systeme.iterations));
        longueurRentree.setText(String.valueOf(systeme.longueur));
        graineRentree.setText(systeme.graine == null ? "" : String.valueOf(systeme.graine));
        remplissage = false;
    }

    /** Menu « Enregistrer le système ». */
    private void enregistreSysteme() {
        try {
            exports.enregistre(parametresSaisis());
        } catch (IllegalArgumentException e) {
            afficheErreur(e.getMessage());
        }
    }

    /** Menu « Ouvrir un système ». */
    private void ouvreSysteme() {
        SauvegardeSysteme systeme = exports.ouvre();
        if (systeme != null)
            remplitAvec(systeme);
    }

    /** Menu « Copier le système » : une ligne « lsys1:… » dans le presse-papiers. */
    private void copieSysteme() {
        try {
            ClipboardContent contenu = new ClipboardContent();
            contenu.putString(parametresSaisis().versPressePapiers());
            Clipboard.getSystemClipboard().setContent(contenu);
            barreEtat.message("Système copié dans le presse-papiers : il peut être collé dans un message, puis "
                + "« Coller un système ».");
        } catch (IllegalArgumentException e) {
            afficheErreur(e.getMessage());
        }
    }

    /** Menu « Coller un système ». */
    private void colleSysteme() {
        try {
            remplitAvec(SauvegardeSysteme.depuisPressePapiers(Clipboard.getSystemClipboard().getString()));
            barreEtat.message("Système collé : cliquer sur « Générer système ».");
        } catch (IllegalArgumentException e) {
            afficheErreur(e.getMessage());
        }
    }

    /**
     * Met à jour le panneau de la chaîne, le libellé de la génération et la barre d'état.
     * @param duree la durée du calcul en nanosecondes (négative : non affichée)
     * @param erreur un message d'erreur à afficher dans la barre d'état, ou null
     */
    private void metAJourInfos(long duree, String erreur) {
        int k = generationCourante, n = derivation.nbGenerations() - 1;
        int longueur = derivation.longueur(k);
        boolean stockee = derivation.estStockee(k);
        panneauChaine.libelle.setText("Génération " + k + " / " + n + " | " + Formats.entier(longueur)
            + (longueur > 1 ? " symboles" : " symbole") + (stockee ? "" : " (non conservée)"));
        panneauChaine.afficheMot(stockee ? derivation.mot(k) : null, stockee ? derivation.provenance(k) : null,
            textesRegles());
        panneauChaine.precedent.setDisable(k == 0);
        panneauChaine.suivant.setDisable(false);

        StringBuilder resume = new StringBuilder("Génération " + k + "/" + n + " | " + Formats.entier(longueur)
            + (longueur > 1 ? " symboles" : " symbole"));
        Trace trace = zoneDessin.trace();
        if (motCourant != null && trace != null)
            resume.append(" | ").append(Formats.entier(trace.nbSegments))
                .append(trace.nbSegments > 1 ? " segments" : " segment");
        if (duree >= 0)
            resume.append(" | ").append(Formats.duree(duree));
        barreEtat.resume(resume.toString());
        metAJourMenus();
        barreEtat.avertissements(motCourant == null ? List.of() : Diagnostic.analyse(systemeCourant, motCourant, k));
        if (trace != null && trace.nbSegments > ZoneDessin.MAX_SEGMENTS_SURVOL && motCourant != null)
            barreEtat.message("Survol de la chaîne et du dessin désactivé au-delà de "
                + Formats.entier(ZoneDessin.MAX_SEGMENTS_SURVOL) + " segments.");
        if (erreur != null) {
            barreEtat.message(erreur);
            messageGeneration = erreur;
        } else if (!messageGeneration.isEmpty() && barreEtat.message().equals(messageGeneration)) {
            barreEtat.message("");
            messageGeneration = "";
        }
    }

    /** Bouton « Nouveau tirage » : oublie la graine et génère à nouveau. */
    private void nouveauTirage() {
        graineRentree.clear();
        genere();
    }

    /**
     * Validation pendant la frappe : met le champ des règles en rouge (avec le message en infobulle)
     * si une règle est mal formée ou refusée par le type choisi, et remplit le tableau des règles comprises.
     */
    private void valideRegles() {
        String texte = reglesRentrees.getText();
        if (listeTypes.getValue() == TypeSysteme.PARAMETRIQUE)
            tableRegles.afficheReglesParametriques(texte);
        else
            tableRegles.afficheReglesClassiques(texte);
        try {
            creeSysteme(listeTypes.getValue() == TypeSysteme.PARAMETRIQUE ? "F" : "", texte, 0, 0, 1, 0L);
            reglesRentrees.setStyle("");
            reglesRentrees.setTooltip(infobulleRegles);
        } catch (IllegalArgumentException e) {
            reglesRentrees.setStyle(STYLE_ERREUR);
            reglesRentrees.setTooltip(new Tooltip(e.getMessage()));
        }
    }

    /** Le type de système a changé : active la graine pour les types aléatoires et revalide les règles. */
    private void typeChange() {
        boolean aleatoire = typeAleatoire();
        graineRentree.setDisable(!aleatoire);
        nouveauTirageBouton.setDisable(!aleatoire);
        boolean parametrique = listeTypes.getValue() == TypeSysteme.PARAMETRIQUE;
        reglesRentrees.setPromptText(parametrique ? "A(l)=F(l)[+(30)A(l*0.7)][-(30)A(l*0.7)]" : "X=X+YF+ Y=-FX-Y");
        axiomesRentres.setPromptText(parametrique ? "A(1)" : "X Y");
        infobulleRegles = new Tooltip(parametrique
            ? "Règles séparées par « ; » : S(paramètres):condition~poids=successeur (condition et poids facultatifs)"
            : "Règles séparées par des espaces, au format [G<]S[>D][(poids)]=remplacement");
        valideRegles();
    }

    /** Efface le dessin, les générations, la vue et vide les champs. */
    private void nettoie() {
        zoneDessin.efface();
        systemeCourant = null;
        derivation = null;
        motCourant = null;
        generationCourante = 0;
        panneauChaine.reinitialise();
        barreEtat.efface();
        remplissage = true;
        listeExemples.getSelectionModel().select(0);
        afficheDescription(null);
        iterationsRentrees.clear();
        angleRentre.clear();
        longueurRentree.clear();
        axiomesRentres.clear();
        reglesRentrees.clear();
        graineRentree.clear();
        remplissage = false;
        choisitVue(ZoneDessin.Mode.VUE_2D);
        metAJourMenus();
        stage.setTitle(TITRE);
    }

    /**
     * Retourne un panneau repliable.
     * @return un panneau repliable
     */
    private static TitledPane panneau(String titre, Node contenu, boolean deplie) {
        TitledPane panneau = new TitledPane(titre, contenu);
        panneau.setExpanded(deplie);
        panneau.setAnimated(false);
        return panneau;
    }

    /** Crée le curseur d'angle, synchronisé dans les deux sens avec le champ « Angle ». */
    private void creeCurseurAngle() {
        curseurAngle = new Slider(0, ANGLE_MAX, 90);
        curseurAngle.setMajorTickUnit(45);
        curseurAngle.setMinorTickCount(2);
        curseurAngle.setBlockIncrement(0.5);
        curseurAngle.setTooltip(new Tooltip("Change l'angle et redessine la chaîne affichée, sans régénérer"));
        curseurAngle.valueProperty().addListener((observable, avant, apres) -> {
            if (synchroAngle)
                return;
            double angle = Math.round(apres.doubleValue() * 2) / 2.0;
            synchroAngle = true;
            angleRentre.setText(Formats.nombre(angle));
            synchroAngle = false;
            angleChange(angle);
        });
        angleRentre.textProperty().addListener((observable, avant, apres) -> {
            if (synchroAngle)
                return;
            Double angle = angleDe(apres);
            if (angle == null)
                return;
            synchroAngle = true;
            curseurAngle.setValue(Math.max(0, Math.min(ANGLE_MAX, angle)));
            synchroAngle = false;
            angleChange(angle);
        });
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Champs de saisie des paramètres.
        iterationsRentrees = new TextField();
        iterationsRentrees.setPromptText("10");
        angleRentre = new TextField();
        angleRentre.setPromptText("90");
        angleRentre.setPrefColumnCount(5);
        longueurRentree = new TextField();
        longueurRentree.setPromptText("5");
        longueurRentree.setTooltip(new Tooltip("Longueur d'un segment, en pixels. Quand « Ajuster à la fenêtre » est coché, "
            + "elle n'a pas d'effet sur la taille affichée : la figure est agrandie ou réduite pour remplir la zone de dessin."));
        axiomesRentres = new TextField();
        axiomesRentres.setPromptText("X Y");
        reglesRentrees = new TextField();
        reglesRentrees.setPromptText("X=X+YF+ Y=-FX-Y");
        infobulleRegles = new Tooltip("Règles séparées par des espaces, au format [G<]S[>D][(poids)]=remplacement");
        reglesRentrees.setTooltip(infobulleRegles);
        graineRentree = new TextField();
        graineRentree.setPromptText("aléatoire");
        graineRentree.setTooltip(new Tooltip("Graine du tirage au hasard (systèmes stochastiques) : la même graine "
            + "redonne la même figure. Vide : une graine est tirée au hasard puis affichée ici."));
        creeCurseurAngle();
        HBox ligneAngle = new HBox(6, angleRentre, curseurAngle);
        ligneAngle.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(curseurAngle, Priority.ALWAYS);

        // Exemples du parcours (fichier exemples.txt, ou exemples intégrés s'il est absent ou invalide).
        Exemples.Catalogue catalogue = Exemples.charge(AffichageSysteme.class.getResourceAsStream("exemples.txt"));
        listeExemples = new ComboBox<>();
        listeExemples.getItems().add(new Exemple("Système personnalisé", null, "", "", 0, 0, 1, "", "", ""));
        listeExemples.getItems().addAll(catalogue.exemples);
        listeExemples.getSelectionModel().select(0);
        listeExemples.setOnAction(event -> {
            if (!remplissage)
                choisitExemple(listeExemples.getValue());
        });
        listeExemples.setMaxWidth(Double.MAX_VALUE);
        Button exemplePrecedent = new Button(), exempleSuivant = new Button();
        exemplePrecedent.setGraphic(Icones.precedent());
        exemplePrecedent.setTooltip(new Tooltip("Exemple précédent"));
        exemplePrecedent.setOnAction(event -> exempleVoisin(-1));
        exempleSuivant.setGraphic(Icones.suivant());
        exempleSuivant.setTooltip(new Tooltip("Exemple suivant"));
        exempleSuivant.setOnAction(event -> exempleVoisin(1));
        HBox ligneExemples = new HBox(6, listeExemples, exemplePrecedent, exempleSuivant);
        ligneExemples.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(listeExemples, Priority.ALWAYS);
        descriptionExemple = new TextFlow();
        descriptionExemple.setPadding(new Insets(6, 8, 6, 8));
        descriptionExemple.setStyle("-fx-background-color: #f6f3e8; -fx-background-radius: 4;");
        descriptionExemple.setPrefWidth(250);

        listeTypes = new ComboBox<>();
        listeTypes.getItems().addAll(TypeSysteme.values());
        listeTypes.getSelectionModel().select(0);

        listeCouleurs = new ComboBox<>();
        listeCouleurs.getItems().addAll(NOMS_COULEURS);
        listeCouleurs.getSelectionModel().select(0);
        listeCouleurs.setOnAction(event -> couleurChange());

        // Zone de dessin et options d'affichage.
        zoneDessin = new ZoneDessin();
        caseAjuster = new CheckBox("Ajuster à la fenêtre");
        caseAjuster.setSelected(true);
        caseAjuster.setTooltip(new Tooltip("Agrandit ou réduit la figure pour qu'elle remplisse la zone de dessin. "
            + "Décoché : taille réelle (longueur en pixels), réduite seulement si elle déborde."));
        caseAjuster.setOnAction(event -> {
            zoneDessin.options.ajuster = caseAjuster.isSelected();
            zoneDessin.redessine();
        });
        ToggleGroup groupeVue = new ToggleGroup();
        choixVue2D = new RadioButton("2D");
        choixVue3D = new RadioButton("3D");
        choixVolumes = new RadioButton("Volumes");
        choixVue2D.setToggleGroup(groupeVue);
        choixVue3D.setToggleGroup(groupeVue);
        choixVolumes.setToggleGroup(groupeVue);
        choixVue2D.setSelected(true);
        choixVue2D.setTooltip(new Tooltip("Vue de face. Glisser : déplacer ; molette : zoom ; double-clic : vue initiale"));
        choixVue3D.setTooltip(new Tooltip("Traits en 3D. Glisser : tourner ; bouton droit ou Maj + glisser : déplacer ; "
            + "molette : zoom ; double-clic : vue de face"));
        if (ZoneDessin.volumesDisponibles()) {
            choixVolumes.setTooltip(new Tooltip("Branches en volume, éclairées (scène 3D), jusqu'à "
                + Formats.entier(Maillage.MAX_SEGMENTS) + " segments. Mêmes gestes que la vue 3D."));
        } else {
            choixVolumes.setDisable(true);
            choixVolumes.setTooltip(new Tooltip("La scène 3D n'est pas disponible sur cette machine : utilisez la vue 3D."));
        }
        groupeVue.selectedToggleProperty().addListener(observable -> {
            if (!reglageVue)
                choisitVue(vueChoisie());
        });
        casePerspective = new CheckBox("Perspective");
        casePerspective.setDisable(true);
        casePerspective.setOnAction(event -> {
            zoneDessin.vue.perspective = casePerspective.isSelected();
            zoneDessin.redessine();
        });
        FlowPane ligneVue = new FlowPane(12, 4, choixVue2D, choixVue3D, choixVolumes);
        ligneVue.setPrefWrapLength(240);
        caseEpaisseurCouleur = new CheckBox("Épaisseur et couleur");
        caseEpaisseurCouleur.setTooltip(new Tooltip("Trait épais et brun à la base, fin et vert aux extrémités : "
            + "selon les symboles ! et ' s'il y en a, sinon selon la profondeur des branches. "
            + "Décoché : trait simple de la couleur choisie."));
        caseEpaisseurCouleur.setOnAction(event -> {
            regleEpaisseurCouleur(caseEpaisseurCouleur.isSelected());
            zoneDessin.maillagePerime();
        });
        FlowPane ligneAffichage = new FlowPane(12, 4, caseAjuster, casePerspective, caseEpaisseurCouleur);
        ligneAffichage.setPrefWrapLength(240);

        Button genereBouton = new Button("Générer système");
        genereBouton.setDefaultButton(true);
        genereBouton.setOnAction(event -> genere());
        Button nettoieBouton = new Button("Nettoyer");
        nettoieBouton.setOnAction(event -> nettoie());
        nouveauTirageBouton = new Button("Nouveau tirage");
        nouveauTirageBouton.setTooltip(new Tooltip("Oublie la graine et génère une nouvelle figure au hasard"));
        nouveauTirageBouton.setOnAction(event -> nouveauTirage());
        nouveauTirageBouton.setMinWidth(Button.USE_PREF_SIZE);
        HBox ligneGraine = new HBox(6, graineRentree, nouveauTirageBouton);
        HBox.setHgrow(graineRentree, Priority.ALWAYS);

        // Formulaire du système.
        GridPane formulaire = new GridPane();
        formulaire.setHgap(10);
        formulaire.setVgap(8);
        formulaire.setPadding(new Insets(10));
        ColumnConstraints colonneEtiquettes = new ColumnConstraints();
        ColumnConstraints colonneChamps = new ColumnConstraints(230, 250, Double.MAX_VALUE);
        colonneChamps.setHgrow(Priority.ALWAYS);
        formulaire.getColumnConstraints().addAll(colonneEtiquettes, colonneChamps);

        Label[] etiquettes = {new Label("Exemples :"), new Label("Type de système :"), new Label("Axiome(s) :"),
            new Label("Règle(s) :"), new Label("Angle de rotation (°) :"), new Label("Nombre d'itérations :"),
            new Label("Longueur d'un segment :"), new Label("Graine :"), new Label("Couleur :"), new Label("Vue :"),
            new Label("Affichage :")};
        Node[] champs = {ligneExemples, listeTypes, axiomesRentres, reglesRentrees, ligneAngle,
            iterationsRentrees, longueurRentree, ligneGraine, listeCouleurs, ligneVue, ligneAffichage};
        for (int ligne = 0; ligne < champs.length; ligne++) {
            if (champs[ligne] instanceof ComboBox)
                ((ComboBox<?>) champs[ligne]).setMaxWidth(Double.MAX_VALUE);
            // La description de l'exemple s'insère sous la ligne des exemples.
            int rang = ligne == 0 ? 0 : ligne + 1;
            formulaire.add(etiquettes[ligne], 0, rang);
            formulaire.add(champs[ligne], 1, rang);
        }
        formulaire.add(descriptionExemple, 0, 1, 2, 1);
        afficheDescription(null);
        Button aideBouton = new Button("?");
        aideBouton.setTooltip(new Tooltip("Aide : axiome, règles, itérations, ordres de la tortue"));
        aideBouton.setOnAction(event -> FenetreAide.ouvre(stage));
        HBox boutons = new HBox(10, genereBouton, nettoieBouton, aideBouton);
        boutons.setAlignment(Pos.CENTER_LEFT);
        formulaire.add(boutons, 0, champs.length + 1, 2, 1);

        // Chaîne générée, règles comprises et aide.
        panneauChaine = new PanneauChaine();
        panneauChaine.setPadding(new Insets(8));
        panneauChaine.curseurIteration.valueProperty().addListener((observable, avant, apres) -> {
            int k = (int) Math.round(apres.doubleValue());
            if (!reglageCurseur && k != generationCourante)
                choisitGeneration(k);
        });
        panneauChaine.precedent.setOnAction(event -> generationPrecedente());
        panneauChaine.suivant.setOnAction(event -> generationSuivante());
        panneauChaine.survolModule = this::moduleSurvole;
        panneauChaine.caseProvenance.setOnAction(event -> {
            if (derivation != null)
                metAJourInfos(-1, null);
        });
        panneauChaine.copier.setOnAction(event -> copieChaine());
        panneauChaine.lecture.setOnAction(event -> lanceAnimation());
        panneauChaine.pause.setOnAction(event -> zoneDessin.pauseAnimation());
        panneauChaine.debut.setOnAction(event -> zoneDessin.debutAnimation());
        panneauChaine.vitesse.valueProperty().addListener(observable ->
            zoneDessin.vitesseAnimation = panneauChaine.vitesse());
        zoneDessin.survolSegment = this::segmentSurvole;
        zoneDessin.avancementAnimation = module -> panneauChaine.surligneModule(module);

        tableRegles = new TableRegles();
        reglesRentrees.textProperty().addListener(observable -> valideRegles());
        listeTypes.valueProperty().addListener(observable -> typeChange());
        typeChange();

        VBox panneaux = new VBox(
            panneau("Système", formulaire, true),
            panneau("Chaîne générée", panneauChaine, true),
            panneau("Règles comprises", tableRegles, false));
        ScrollPane defilement = new ScrollPane(panneaux);
        defilement.setFitToWidth(true);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        defilement.setPrefViewportWidth(460);
        defilement.setMinWidth(360);

        barreEtat = new BarreEtat();
        if (!catalogue.messages.isEmpty())
            barreEtat.message(String.join(" ; ", catalogue.messages));
        zoneDessin.messages = barreEtat::message;
        exports = new Exports(stage, barreEtat);

        // Menus.
        MenuItem ouvrir = new MenuItem("Ouvrir un système…");
        ouvrir.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        ouvrir.setOnAction(event -> ouvreSysteme());
        MenuItem enregistrer = new MenuItem("Enregistrer le système…");
        enregistrer.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));
        enregistrer.setOnAction(event -> enregistreSysteme());
        MenuItem copierSysteme = new MenuItem("Copier le système (une ligne)");
        copierSysteme.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+C"));
        copierSysteme.setOnAction(event -> copieSysteme());
        MenuItem collerSysteme = new MenuItem("Coller un système");
        collerSysteme.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+V"));
        collerSysteme.setOnAction(event -> colleSysteme());
        menuPng = new MenuItem("Exporter en PNG (image)…");
        menuPng.setOnAction(event -> exports.exportePng(zoneDessin.enVolumes() ? zoneDessin.scene3D().sousScene
            : zoneDessin.canvas));
        menuSvg = new MenuItem("Exporter en SVG (dessin)…");
        menuSvg.setOnAction(event -> exports.exporteSvg(zoneDessin.trace(), zoneDessin.vue, zoneDessin.options,
            zoneDessin.getWidth(), zoneDessin.getHeight()));
        menuObj = new MenuItem("Exporter en OBJ (Blender)…");
        menuObj.setOnAction(event -> exports.exporteObj(zoneDessin.trace(), zoneDessin.optionsMaillage()));
        menuStl = new MenuItem("Exporter en STL (impression 3D)…");
        menuStl.setOnAction(event -> exports.exporteStl(zoneDessin.trace(), zoneDessin.optionsMaillage()));
        MenuItem quitter = new MenuItem("Quitter");
        quitter.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        quitter.setOnAction(event -> stage.close());
        Menu fichier = new Menu("Fichier");
        fichier.getItems().addAll(ouvrir, enregistrer, new SeparatorMenuItem(), copierSysteme, collerSysteme,
            new SeparatorMenuItem(), menuPng, menuSvg, menuObj, menuStl, new SeparatorMenuItem(), quitter);
        MenuItem aideMenu = new MenuItem("Aide…");
        aideMenu.setAccelerator(KeyCombination.keyCombination("F1"));
        aideMenu.setOnAction(event -> FenetreAide.ouvre(stage));
        Menu menuAide = new Menu("Aide");
        menuAide.getItems().add(aideMenu);
        MenuBar barreMenus = new MenuBar(fichier, menuAide);
        metAJourMenus();

        BorderPane racine = new BorderPane();
        racine.setTop(barreMenus);
        racine.setLeft(defilement);
        racine.setCenter(zoneDessin);
        racine.setBottom(barreEtat);

        // Création de la scène et du stage
        stage.setScene(new Scene(racine, 1200, 800));
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setTitle(TITRE);
        stage.show();
    }

    /**
     * Lance l'application.
     * @param args les arguments de la ligne de commande (transmis à JavaFX)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
