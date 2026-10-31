package systeme.affichage;

import java.util.List;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import systeme.lindenmeyer.Mot;

/**
 * Le panneau « Chaîne générée » : un curseur pour choisir la génération affichée, deux boutons pour reculer ou
 * avancer d'un pas, la chaîne de la génération choisie (au plus 5 000 caractères), et les commandes de la
 * tortue animée.
 * <ul>
 * <li>Survoler un symbole de la chaîne le surligne ; si c'est un {@code F}, son segment est surligné sur le dessin
 *     (et inversement, voir {@link #surligneModule(int)}).</li>
 * <li>« Colorer selon la règle » colore chaque morceau de la chaîne selon la règle qui l'a produit, avec une légende.</li>
 * </ul>
 * La logique (dérivation, dessin) est dans {@link AffichageSysteme} ; ce panneau affiche et signale les survols.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class PanneauChaine extends VBox {
    /** Au-delà, la chaîne affichée est tronquée (un très long texte rend l'affichage très lent). */
    static final int MAX_CARACTERES_AFFICHES = 5000;
    /** Couleurs des règles (provenance), utilisées dans l'ordre et de façon cyclique. */
    static final Color[] COULEURS_REGLES = {Color.web("#1565c0"), Color.web("#c62828"), Color.web("#2e7d32"),
        Color.web("#6a1b9a"), Color.web("#ef6c00"), Color.web("#00838f")};
    /** Au-delà de ce nombre de générations, les numéros ne sont plus écrits sous le curseur. */
    private static final int MAX_ETIQUETTES = 20;
    private static final Font POLICE = Font.font("Monospaced", 12);

    final Slider curseurIteration = new Slider(0, 0, 0);
    final Button precedent = new Button();
    final Button suivant = new Button();
    final Label libelle = new Label();
    final CheckBox caseProvenance = new CheckBox("Colorer selon la règle");
    final Button copier = new Button("Copier la chaîne");
    final Button lecture = new Button(), pause = new Button(), debut = new Button();
    /** Vitesse de la tortue animée : 10^valeur segments par seconde (de 1 à 10 000). */
    final Slider vitesse = new Slider(0, 4, 2);
    private final Label libelleVitesse = new Label();
    /** Appelé quand la souris survole un module de la chaîne (−1 : aucun). */
    IntConsumer survolModule = module -> { };

    private final TextFlow flux = new TextFlow();
    private final Path surlignage = new Path();
    /** Une ligne par règle (une VBox : un long texte se replie au lieu d'élargir le panneau). */
    private final VBox legende = new VBox(2);
    private Mot.Texte texte;
    private int moduleSurligne = -1;

    PanneauChaine() {
        super(6);
        curseurIteration.setMajorTickUnit(1);
        curseurIteration.setMinorTickCount(0);
        curseurIteration.setSnapToTicks(true);
        curseurIteration.setBlockIncrement(1);
        curseurIteration.setShowTickMarks(true);
        curseurIteration.setShowTickLabels(true);
        curseurIteration.setTooltip(new Tooltip("Génération affichée (0 = l'axiome)"));
        precedent.setGraphic(Icones.precedent());
        precedent.setTooltip(new Tooltip("Génération précédente"));
        suivant.setGraphic(Icones.suivant());
        suivant.setTooltip(new Tooltip("Génération suivante (calculée si besoin)"));
        HBox navigation = new HBox(6, precedent, curseurIteration, suivant);
        navigation.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(curseurIteration, Priority.ALWAYS);

        // Le texte et, derrière lui, le surlignage du module survolé.
        surlignage.setFill(Color.rgb(255, 193, 7, 0.55));
        surlignage.setStroke(null);
        surlignage.setManaged(false);
        Pane fond = new Pane(surlignage);
        fond.setMouseTransparent(true);
        StackPane superposition = new StackPane(fond, flux);
        superposition.setAlignment(Pos.TOP_LEFT);
        superposition.setPadding(new Insets(4));
        superposition.setStyle("-fx-background-color: white;");
        ScrollPane defilement = new ScrollPane(superposition);
        defilement.setFitToWidth(true);
        defilement.setPrefViewportHeight(112);
        // Sans cela, la largeur préférée serait celle du texte sur une seule ligne (des dizaines de milliers de pixels).
        defilement.setPrefViewportWidth(300);
        defilement.setMinWidth(0);
        defilement.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        flux.setOnMouseMoved(evenement -> survole(evenement.getX(), evenement.getY()));
        flux.setOnMouseExited(evenement -> survolModule.accept(-1));

        caseProvenance.setTooltip(new Tooltip("Colore chaque morceau de la chaîne selon la règle qui l'a produit "
            + "(générations d'au plus 20 000 symboles)"));
        copier.setTooltip(new Tooltip("Copie toute la chaîne de la génération affichée dans le presse-papiers"));
        HBox options = new HBox(10, caseProvenance, copier);
        options.setAlignment(Pos.CENTER_LEFT);

        lecture.setGraphic(Icones.suivant());
        lecture.setTooltip(new Tooltip("Tortue animée : rejoue le tracé, segment par segment"));
        pause.setGraphic(Icones.pause());
        pause.setTooltip(new Tooltip("Pause"));
        debut.setGraphic(Icones.debut());
        debut.setTooltip(new Tooltip("Retour au début du tracé"));
        vitesse.setTooltip(new Tooltip("Vitesse de la tortue (échelle logarithmique)"));
        vitesse.valueProperty().addListener(observable -> libelleVitesse.setText(Formats.entier(vitesse()) + " segments/s"));
        libelleVitesse.setText(Formats.entier(vitesse()) + " segments/s");
        libelleVitesse.setMinWidth(Label.USE_PREF_SIZE);
        HBox animation = new HBox(6, new Label("Tortue :"), lecture, pause, debut, vitesse, libelleVitesse);
        animation.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(vitesse, Priority.ALWAYS);

        getChildren().addAll(navigation, libelle, defilement, legende, options, animation);
        reinitialise();
    }

    /**
     * Retourne la vitesse de la tortue animée, en segments par seconde.
     * @return la vitesse de la tortue animée, en segments par seconde
     */
    long vitesse() {
        return Math.round(Math.pow(10, vitesse.getValue()));
    }

    /**
     * Règle le curseur sans déclencher de dessin (l'appelant protège l'écouteur).
     * @param derniere le numéro de la dernière génération calculée
     * @param valeur la génération à sélectionner
     */
    void regleCurseur(int derniere, int valeur) {
        curseurIteration.setMax(Math.max(0, derniere));
        curseurIteration.setValue(valeur);
        curseurIteration.setShowTickLabels(derniere <= MAX_ETIQUETTES);
        curseurIteration.setShowTickMarks(derniere <= 5 * MAX_ETIQUETTES);
        curseurIteration.setDisable(derniere == 0);
    }

    /**
     * Affiche une génération (au plus {@link #MAX_CARACTERES_AFFICHES} caractères).
     * @param mot la génération (null : aucune)
     * @param provenance les triplets (début, fin, règle) de la génération, ou null
     * @param regles le texte de chaque règle, pour la légende
     */
    void afficheMot(Mot mot, int[] provenance, List<String> regles) {
        flux.getChildren().clear();
        legende.getChildren().clear();
        surlignage.getElements().clear();
        moduleSurligne = -1;
        texte = null;
        if (mot == null)
            return;
        texte = mot.texte(MAX_CARACTERES_AFFICHES);
        String chaine = texte.texte;
        boolean couleurs = caseProvenance.isSelected() && provenance != null;
        boolean[] utilisees = new boolean[regles.size()];
        int position = 0;
        if (couleurs) {
            for (int t = 0; t + 2 < provenance.length && provenance[t] < texte.nbModules; t += 3) {
                int debutTexte = texte.debuts[provenance[t]];
                int finTexte = provenance[t + 1] - 1 < texte.nbModules ? texte.fin(provenance[t + 1] - 1) : chaine.length();
                if (debutTexte > position)
                    flux.getChildren().add(morceau(chaine.substring(position, debutTexte), Color.BLACK));
                int regle = provenance[t + 2];
                if (regle >= 0 && regle < utilisees.length)
                    utilisees[regle] = true;
                flux.getChildren().add(morceau(chaine.substring(debutTexte, finTexte),
                    COULEURS_REGLES[Math.floorMod(regle, COULEURS_REGLES.length)]));
                position = finTexte;
            }
        }
        if (position < chaine.length())
            flux.getChildren().add(morceau(chaine.substring(position), Color.BLACK));
        if (texte.nbModules < mot.taille())
            flux.getChildren().add(morceau("… (" + Formats.entier(mot.taille()) + " symboles au total)", Color.GRAY));
        if (couleurs) {
            for (int r = 0; r < regles.size(); r++)
                if (utilisees[r]) {
                    Rectangle carre = new Rectangle(10, 10, COULEURS_REGLES[r % COULEURS_REGLES.length]);
                    Label entree = new Label("règle " + (r + 1) + " : " + regles.get(r), carre);
                    entree.setTextFill(COULEURS_REGLES[r % COULEURS_REGLES.length]);
                    entree.setWrapText(true);
                    legende.getChildren().add(entree);
                }
        } else if (caseProvenance.isSelected() && provenance == null && !regles.isEmpty()) {
            Label remarque = new Label("Pas de couleurs pour l'axiome ni pour une génération de plus de 20 000 symboles.");
            remarque.setWrapText(true);
            legende.getChildren().add(remarque);
        }
    }

    private static Text morceau(String contenu, Color couleur) {
        Text t = new Text(contenu);
        t.setFont(POLICE);
        t.setFill(couleur);
        return t;
    }

    /**
     * Surligne un module de la chaîne (sans prévenir l'écouteur de survol).
     * @param module le numéro du module, ou −1 pour effacer ; un module au-delà du texte affiché est ignoré
     */
    void surligneModule(int module) {
        if (module == moduleSurligne)
            return;
        moduleSurligne = module;
        if (texte == null || module < 0 || module >= texte.nbModules) {
            surlignage.getElements().clear();
            return;
        }
        surlignage.getElements().setAll(flux.rangeShape(texte.debuts[module], texte.fin(module)));
    }

    private void survole(double x, double y) {
        if (texte == null)
            return;
        HitInfo touche = flux.hitTest(new Point2D(x, y));
        int caractere = touche.getCharIndex();
        int module = caractere >= 0 && caractere < texte.texte.length() ? texte.module(caractere) : -1;
        surligneModule(module);
        survolModule.accept(module);
    }

    /** Remet le panneau dans son état initial (aucune génération). */
    void reinitialise() {
        regleCurseur(0, 0);
        libelle.setText("Aucune génération : cliquer sur « Générer système ».");
        afficheMot(null, null, List.of());
        precedent.setDisable(true);
        suivant.setDisable(true);
    }
}
