package systeme.lindenmeyer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Une classe représentant un système de Lindemayer stochastique.
 * Un système stochastique est similaire à un système déterministe sauf qu'il inclus la possibilité
 * de choisir au hasard une des règles durant le processus de réécriture.
 * 
 * @author Ali Azou, Matisse Senechal, Rafik Halit, Université de Caen Normandie, France
 */
public class SystemeStochastique extends SystemeLindenmeyer implements Generation {
  /** Le générateur de nombre aléatoire utilisé pour générer des valeurs aléatoires pour choisir les règles. */
  private Random rand;

  /**
   * Construit une instance d'un système de Lindemayer stochastique.
   * @param axiomes le (ou les) axiome(s) du système
   * @param regles la (ou les) règle(s) du système
   * @param angle l'angle (en degrés) pour tourner lors de l'affichage
   * @param iteration le nombre d'itérations
   * @param longueur la longueur du dessin
   */
	public SystemeStochastique(String axiomes, String regles, double angle, int iteration, int longueur) {
		super(axiomes, regles, angle, iteration, longueur);
    this.rand = new Random();
	}
  public SystemeStochastique() {
	  this("F", "F=F[+F]F[-F]F F=F[+F]F F=F[-F]F", 25.7, 20, 7);
    this.rand = new Random();
  }

  @Override
  public void generationSysteme(int nbIterations, double angle, int longueur, Color couleur, Canvas canvas) {
    ArrayList<String> listeAxiomes = new ArrayList<>(Arrays.asList(supprimeEspaceDeChaine(getAxiomes()).split("")));
    ArrayList<String> listeRegles = transformeChaine(getRegles());
    HashMap<Character, Integer> ocurrencesDesAxiomes = associeAxiomeOccurences(listeRegles);

    String chaineFinale = genereChaineSysteme(listeAxiomes, listeRegles, ocurrencesDesAxiomes, nbIterations);
    GraphicsContext gc = initialiseContexteGraphique(canvas, couleur);
    dessineSysteme(chaineFinale, longueur, couleur, canvas, gc);
  }

  /**
   * Supprimer les espaces en trop dans la chaîne.
   * @param chaine la chaîne qui doit être analysée
   * @return la chaîne sans espaces en trop
   */
  private String supprimeEspaceDeChaine(String chaine) {
    return chaine.trim().replaceAll("\\s+", " ");
  }

  /**
   * Vérifie si un axiome donné existe.
   * @param axiome l'axiome pour lequel choisir une règle aléatoire
   * @param occurrencesDesAxiomes une HashMap contenant le nombre d'occurrences pour chaque axiome
   * @return true si l'axiome existe, sinon false.
   */
  private boolean existeAxiome(char axiome, HashMap<Character, Integer> occurrencesDesAxiomes) {
    Integer nbOccurences = occurrencesDesAxiomes.get(axiome);
    return nbOccurences != null && nbOccurences > 0;
  }

  /**
   * Retourne un HashMap contenant les occurences de chaque axiome de la liste des règles.
   * @param regles la liste des règles du système
   * @return un HashMap contenant les occurences de chaque axiome de la liste des règles
   */
  private HashMap<Character, Integer> associeAxiomeOccurences(ArrayList<String> regles) {
    HashMap<Character, Integer> occurrencesAxiomes = new HashMap<>();
    for (String regle : regles) {
        char axiome = regle.charAt(0);
        if (!occurrencesAxiomes.containsKey(axiome))
            occurrencesAxiomes.put(axiome, 1);
        else
            occurrencesAxiomes.put(axiome, occurrencesAxiomes.get(axiome)+1);
    }
    return occurrencesAxiomes;
  }

  /**
   * Choisit une règle aléatoirement dans une liste de règles.
   * @param axiome l'axiome pour lequel choisir une règle aléatoire
   * @param listeRegles la liste des règles du système
   * @param occurrencesDesAxiomes un HashMap contenant le nombre d'occurrences pour chaque axiome
   * @return Une règle aléatoire correspondant à l'axiome spécifié
   */
  private String choisitRegleAlea(char axiome, ArrayList<String> listeRegles, HashMap<Character, Integer> occurrencesDesAxiomes) {
    if (!existeAxiome(axiome, occurrencesDesAxiomes))
      return "";
    int occurrences = occurrencesDesAxiomes.get(axiome);
    String regle = listeRegles.get(rand.nextInt(occurrences));
    return regle;
  }

  /**
   * Génère une chaîne de caractères aléatoire.
   * @param listeAxiomes la liste des axiomes
   * @param listeRegles la liste des règles
   * @param occurrencesDesAxiomes un HashMap contenant le nombre d'occurrences pour chaque axiome
   * @return Une chaîne de caractères aléatoire
   */
  private String genereChaineAlea(ArrayList<String> listeAxiomes, ArrayList<String> listeRegles, HashMap<Character, Integer> occurrencesDesAxiomes) {
    String chaineAlea = "";
    int nbAxiomes = listeAxiomes.size();
    for (int posAxiome=0; posAxiome < nbAxiomes; posAxiome++) {
      char axiome = listeAxiomes.get(posAxiome).charAt(0);
      String regle = choisitRegleAlea(axiome, listeRegles, occurrencesDesAxiomes);
      chaineAlea += supprimeAxiomeDeRegle(regle);
    }
    return chaineAlea;
  }

  /**
   * Pour chaque itération génère une chaîne de caractères à partir des listes d'axiomes et de règles données.
   * @param listeAxiomes la liste des axiomes
   * @param listeRegles la liste des règles
   * @param occurrencesDesAxiomes un HashMap contenant le nombre d'occurrences pour chaque axiome
   * @param nbIterations le nombre d'itérations
   * @return une chaîne de caractères générée à partir des listes d'axiomes et de règles données
   */
  private String genereChaineSysteme(ArrayList<String> listeAxiomes, ArrayList<String> listeRegles, HashMap<Character, Integer> occurrencesDesAxiomes, int nbIterations) {
    String chaineSysteme = "";
    for (int iteration=0; iteration < nbIterations; iteration++) {
        String chaineIteration = genereChaineAlea(listeAxiomes, listeRegles, occurrencesDesAxiomes);
        chaineSysteme += chaineIteration;
    }
    return chaineSysteme;
  }
}

