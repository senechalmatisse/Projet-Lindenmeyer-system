package systeme.lindenmeyer;

import java.util.HashMap;
import java.util.ArrayList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Cette classe représente un système de Lindenmeyer déterministe.
 * Un système de Lindenmeyer déterministe est un type de système de Lindenmeyer qui utilise des règles de remplacement pour générer une chaîne de caractères.
 * Cette chaîne de caractères est ensuite interprétée pour être dessinée.
 * 
 * @author Ali Azou, Matisse Senechal, Rafik Halit, Université de Caen Normandie, France
 */
public class SystemeDeterministe extends SystemeLindenmeyer implements Generation {
  /**
   * Construit une nouvelle instance de la classe SystemeDeterministe.
   * @param axiomes les axiomes du système de Lindenmeyer.
   * @param regles Les règles produisant la chaîne de caractère servant à construire le système.
   * @param angle L'angle (en degrés) pour tourner la figure graphique selon le symbole rencontré.
   * @param iteration Le nombre de fois que l'on construit les règles du système.
   * @param longueur La longueur du dessin.
   */
   public SystemeDeterministe(String axiomes, String regles, double angle, int iteration, int longueur) {
   	super(axiomes, regles, angle, iteration, longueur);
   }
  public SystemeDeterministe() {
    this("X Y", "X=X+YF+ Y=-FX-Y", 90, 10, 6);
  }

  @Override
  public void generationSysteme(int nbIterations, double angle, int longueur, Color couleur, Canvas canvas) {
    HashMap<Character, String> axiomesReglesCorrrespondants = associeAxiomeRegle(getRegles());
    String chaineFinale = getAxiomes();
    GraphicsContext gc = initialiseContexteGraphique(canvas, couleur);
    for (int iteration=0; iteration < nbIterations; iteration++)
      chaineFinale = remplaceAxiomeParRegle(chaineFinale, axiomesReglesCorrrespondants);
    dessineSysteme(chaineFinale, longueur, couleur, canvas, gc);
  }

  /**
   * Associe les axiomes aux règles correspondantes.
   * @param chaineRegles la chaine contenant toutes les règles
   * @return un HashMap associant chaque axiome avec sa règle
   */
  private HashMap<Character, String> associeAxiomeRegle(String chaineRegles) {
    ArrayList<String> listeRegles = transformeChaine(chaineRegles);
    HashMap<Character, String> axiomeRegle = new HashMap<Character, String>();
    for (String regle : listeRegles) {
      char axiome = regle.charAt(0);
      String regleCorrespondante = supprimeAxiomeDeRegle(regle);
      axiomeRegle.put(axiome, regleCorrespondante);
    }
    return axiomeRegle;
  }

  /**
   * Remplace les symboles par leurs règles correspondantes, tandis que les symboles sans règles sont conservés tels quels.
   * @param chaine la chaine de symboles initiale à modifier.
   * @param axiomesReglesCorrrespondants le HashMap des axiomes et leurs règles.
   * @return La chaine modifiée résultante après la substitution.
   */
  private String remplaceAxiomeParRegle(String chaine, HashMap<Character, String> axiomesReglesCorrrespondants) {
    String chaineModifiee = "";
    for (int caractere = 0; caractere < chaine.length(); caractere++) {
        char symbole = chaine.charAt(caractere);
        String regle = axiomesReglesCorrrespondants.get(symbole);
        if (regle != null)
            chaineModifiee += regle;
        else
            chaineModifiee += symbole;
    }
    return chaineModifiee;
  }
}
