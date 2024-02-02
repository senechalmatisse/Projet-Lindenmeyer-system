package systeme.affichage;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.canvas.GraphicsContext;
import systeme.lindenmeyer.*;

/**
 * Une classe qui sert à l'affichage du système de Lindenmeyer choisit.
 * 
 * @author Ali Azou, Matisse Senechal, Rafik Halit, Université de Caen Normandie, France
 */
public class AffichageSysteme extends Application {
	/**
     * Le nombre d'itérations.
     * La longueur du dessin.
     * L'angle du dessin (en degrés).
     * Le (ou les) axiome(s) du système.
     * La (ou les) règle(s) du système.
	 * La couleur du dessin.
	 * Les TextField pour changer les paramètres préconfigurés des systèmes.
	 * Le canvas où dessiner le système.
	 */
	private int iterations, longueur, angle;
	private String axiomes, regles;
	private Color couleur;
	private TextField iterationsRentrees, angleRentre, longueurRentree, axiomesRentres, reglesRentrees;
	private Label longueurLabel, angleLabel, iterationsLabel, axiomesLabel, reglesLabel, systemePreconfigure, couleurLabel;
	private Canvas canvas;

	/**
     * Retourne un entier si la chaine contient un entrée valide.
     * @param chaine chaine à transformer
     * @exception NumberFormatException indique que chaine n'a pas le format appoprié et que la conversion vers un type numérique est impossible
     * @return Un entier si la chaine contient un entrée valide
     */
    private boolean stringEntierValide(String chaine) {
        try {
            Integer.parseInt(chaine);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

	/**
	 * Vérifie si les paramètres en entrés sont valides pour le dessin.
	 * @param iterations Le nombre d'itérations.
	 * @param angle L'angle de rotation.
	 * @param longueur La longueur du dessin.
	 * @return true si les paramètres sont valides, sinon false.
     */
	private boolean paramEntreValide(int iterations, int angle, int longueur) {
    	return longueur > 0 && iterations > 0 && angle > 0;
    }

	/**
	 * Vérifie si chaque paramètres est null ou pas.
	 * @param iterationsLe Le nombre d'itérations rentré.
	 * @param angle L'angle de rotation rentré.
	 * @param longueur La longueur rentrée.
	 * @param axiomes les axiomes rentrés.
	 * @param regles Les règles rentrées.
	 * @return true if any parameter is null, false otherwise
     */
	private boolean paramVide(Integer iterations, Integer angle, Integer longueur, String axiomes, String regles) {
    	return iterations == null || angle == null || longueur == null || axiomes == null || regles == null;
	}

	/**
	 * Retourne un entier à partir d'une chaîne de caractères si l'entier est valide.
	 * @param valeurEntree La chaîne de caractères à convertir en entier.
	 * @return un entier si la chaîne de caractères est valide (c'est-à-dire un entier positif)
	 */
	private int recupValeurEntree(String valeurEntree) {
		if(stringEntierValide(valeurEntree))
			return Integer.parseInt(valeurEntree);
		if(valeurEntree.equals(""))
			return -2;
		return -1;
	}

	/**
	 * Gère la visibilité des TextField pour changer les paramètres.
	 * @param visible un booléen indiquant si les TextField doivent être visibles ou pas
     */
	private void afficheParamètres(boolean visible) {
    	iterationsRentrees.setVisible(visible);
    	angleRentre.setVisible(visible);
    	longueurRentree.setVisible(visible);
    	axiomesRentres.setVisible(visible);
    	reglesRentrees.setVisible(visible);
    	longueurLabel.setVisible(visible);
    	angleLabel.setVisible(visible);
    	iterationsLabel.setVisible(visible);
    	axiomesLabel.setVisible(visible);
    	reglesLabel.setVisible(visible);
	}

	/**
	 * Change les paramètres de la fenêtre et génère le système de Lindenmeyer correspodant.
	 * @param fenetre La fenêtre dont on doit changer le titre.
	 * @param systeme Le système de Lindenmeyer à générer.
	 * @param title Le nouveau titre de la fenêtre.
	 * @param x La coordonnée x du canvas pour la fenêtre.
	 * @param y La coordonnée y du canvas pour la fenêtre.
     */
	private void changeParametresFenetre(Stage fenetre, Generation systeme, String titre, double x, double y) {
    	fenetre.setTitle(titre);
    	canvas.setTranslateX(x);
    	canvas.setTranslateY(y);
    	systeme.generationSysteme(systeme.getIteration(), Math.toRadians(systeme.getAngle()), systeme.getLongueur(), couleur, canvas);

	}

	@Override
	public void start(Stage stage) {
		// Initialise les composants de la mise en page du GUI.
		Group conteneur = new Group();
		GridPane miseEnPage = new GridPane();
		miseEnPage.setAlignment(Pos.CENTER);
		miseEnPage.setHgap(10);
		miseEnPage.setVgap(10);
		miseEnPage.setPadding(new Insets(25, 25, 25, 25));
     	
		// Label gérant les interactions utilisateur.
		iterationsLabel = new Label("Nombre d'itérations :");
		iterationsRentrees = new TextField();
		iterationsRentrees.setPromptText("10");

		angleLabel = new Label("Angle de rotation :");
		angleRentre = new TextField();
		angleRentre.setPromptText("90");

		longueurLabel = new Label("Longueur :");
		longueurRentree = new TextField();
		longueurRentree.setPromptText("5");

		axiomesLabel = new Label("Axiome(s) :");
		axiomesRentres = new TextField();
		axiomesRentres.setPromptText("X Y");

		reglesLabel = new Label("Règle(s) :");
		reglesRentrees = new TextField();
		reglesRentrees.setPromptText("X=X+YF+ Y=-FX-Y");

		systemePreconfigure = new Label("Systèmes préconfigurés disponibles :");

		couleurLabel = new Label("Couleurs disponibles :");
		
		// Liste des couleurs disponibles pour le dessin
		ComboBox<String> listeCouleurs = new ComboBox<>();
		listeCouleurs.getItems().addAll("Marron", "Vert", "Bleu", "Rouge");
		listeCouleurs.getSelectionModel().select(0);

		// Initialise le canvas utilisé pour dessiner le L-système.
		canvas = new Canvas(2000, 2000);
		canvas.setWidth(2000);
		canvas.setHeight(2000);

		// Définit sa taille et sa position sur l'écran.
		canvas.setTranslateX(400);
		canvas.setTranslateY(100);
		conteneur.getChildren().add(canvas);

		// Initialise la liste déroulante des systèmes et l'ajoute à la mise en page.
		ComboBox<String> listeSystemes = new ComboBox<>();
		Button genereBouton = new Button("Générer système");
		Button nettoieBouton = new Button("Nettoyer");

		listeSystemes.getItems().addAll("Système personnalisé","Système détermnisite", "Système stochastique", "Système contextuel");
		listeSystemes.getSelectionModel().select(0);

		// Gère le fonctionnement des boutons 
		listeSystemes.setOnAction(event -> {
    		String systemeChoisi = listeSystemes.getValue().toString();
    		boolean isCustom = systemeChoisi.equals("Système personnalisé");
    		afficheParamètres(isCustom);
    		miseEnPage.getChildren().clear();
    		GridPane.setConstraints(systemePreconfigure, 0, 6);
    		GridPane.setConstraints(listeSystemes, 1, 6);
    		GridPane.setConstraints(couleurLabel, 0, 7);
    		GridPane.setConstraints(listeCouleurs, 1, 7);
    		GridPane.setConstraints(genereBouton, 0, 8);
    		GridPane.setConstraints(nettoieBouton, 1, 8);
    		if (isCustom) {
        		GridPane.setConstraints(iterationsLabel, 0, 1);
        		GridPane.setConstraints(iterationsRentrees, 1, 1);
        		GridPane.setConstraints(angleLabel, 0, 2);
        		GridPane.setConstraints(angleRentre, 1, 2);
        		GridPane.setConstraints(longueurLabel, 0, 3);
        		GridPane.setConstraints(longueurRentree, 1, 3);
        		GridPane.setConstraints(axiomesLabel, 0, 4);
        		GridPane.setConstraints(axiomesRentres, 1, 4);
        		GridPane.setConstraints(reglesLabel, 0, 5);
        		GridPane.setConstraints(reglesRentrees, 1, 5);
        		miseEnPage.getChildren().addAll(iterationsLabel, iterationsRentrees, angleLabel, angleRentre, longueurLabel, longueurRentree, axiomesLabel, axiomesRentres, reglesLabel, reglesRentrees);
    		}
    		miseEnPage.getChildren().addAll(systemePreconfigure, listeSystemes, couleurLabel, listeCouleurs, genereBouton, nettoieBouton);
		});
		genereBouton.setOnAction(event -> {
    		int indexCouleurChoisie = listeCouleurs.getSelectionModel().getSelectedIndex();
    		regles = reglesRentrees.getText();
    		axiomes = axiomesRentres.getText();
    		longueur = recupValeurEntree(longueurRentree.getText());
    		iterations = recupValeurEntree(iterationsRentrees.getText());
    		angle = recupValeurEntree(angleRentre.getText());

    		switch (indexCouleurChoisie) {
        		case 0:
            		couleur = Color.BROWN;
            		break;
        		case 1:
            		couleur = Color.GREEN;
            		break;
        		case 2:
            		couleur = Color.BLUE;
            		break;
        		case 3:
            		couleur = Color.RED;
            		break;
        		default:
            		couleur = Color.BLACK;
            		break;
    		}

    		Generation systeme = null;
    		String systemeChoisit = listeSystemes.getSelectionModel().getSelectedItem().toString();
    		switch (systemeChoisit){
        		case "Système stochastique":
            		systeme = new SystemeStochastique();
					changeParametresFenetre(stage, systeme, "Système stochastique", -100, -700);
            		break;
        		case "Système détermnisite":
            		systeme = new SystemeDeterministe();
					changeParametresFenetre(stage, systeme, "Système détermnisite", 100, -900);
					break;
        		case "Système contextuel":
            		systeme = new SystemeContextuel();
					changeParametresFenetre(stage, systeme, "Système contextuel", 0, -500);
            		break;
        		case "Système personnalisé":
            		if(!paramEntreValide(iterations, angle, longueur) || paramVide(iterations, angle, longueur, axiomes, regles)) {
                		Alert alert = new Alert(AlertType.INFORMATION);
                		alert.setTitle("Erreur génération système");
                		alert.setHeaderText(null);
                		alert.setContentText("L'un de vos paramètres contient une erreur.");
                		alert.showAndWait();
                		return;
            		}
            		systeme = new SystemeDeterministe(axiomes, regles, angle, iterations, longueur);
					changeParametresFenetre(stage, systeme, "Système personnalisé", 100, -700);
            		break;
    		}
    		systeme.generationSysteme(systeme.getIteration(), Math.toRadians(systeme.getAngle()),  systeme.getLongueur(), couleur, canvas);
		});
		nettoieBouton.setOnAction(event -> {
    		GraphicsContext gc = canvas.getGraphicsContext2D();
    		gc.clearRect(-1000, -1000, canvas.getWidth(), canvas.getHeight());
    		gc.setFill(Color.BLACK);
    		iterationsRentrees.clear();
    		angleRentre.clear();
    		longueurRentree.clear();
    		axiomesRentres.clear();
    		reglesRentrees.clear();
    		stage.setTitle("Système lindenmeyer");
		});

		// Mise en page des composants 
		GridPane.setConstraints(iterationsLabel, 0, 1);
		GridPane.setConstraints(iterationsRentrees, 1, 1);
		GridPane.setConstraints(angleLabel, 0, 2);
		GridPane.setConstraints(angleRentre, 1, 2);
		GridPane.setConstraints(longueurLabel, 0, 3);
		GridPane.setConstraints(longueurRentree, 1, 3);
		GridPane.setConstraints(axiomesLabel, 0, 4);
		GridPane.setConstraints(axiomesRentres, 1, 4);
		GridPane.setConstraints(reglesLabel, 0, 5);
		GridPane.setConstraints(reglesRentrees, 1, 5);
		GridPane.setConstraints(systemePreconfigure, 0, 6);
		GridPane.setConstraints(listeSystemes, 1, 6);
		GridPane.setConstraints(couleurLabel, 0, 7);
		GridPane.setConstraints(listeCouleurs, 1, 7);
		GridPane.setConstraints(genereBouton, 0, 8);
		GridPane.setConstraints(nettoieBouton, 1, 8);
		
		miseEnPage.getChildren().addAll(iterationsLabel, iterationsRentrees, angleLabel, angleRentre, longueurLabel, longueurRentree, couleurLabel, listeCouleurs, axiomesLabel, axiomesRentres, reglesLabel, reglesRentrees, systemePreconfigure, listeSystemes, genereBouton, nettoieBouton);
		conteneur.getChildren().add(miseEnPage);

		// Création de la scène et du stage
		Scene scene = new Scene(conteneur, 800, 800);
		stage.setWidth(2000);
		stage.setHeight(2000);
		stage.setScene(scene);
		stage.setTitle("Système lindenmeyer");
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}