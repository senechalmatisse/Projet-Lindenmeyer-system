package systeme.lindenmeyer;

/**
 * Cette classe représente un système de Lindenmayer déterministe.
 * Un système de Lindenmayer déterministe est un type de système de Lindenmayer qui utilise des règles de remplacement pour générer une chaîne de caractères.
 * Cette chaîne de caractères est ensuite interprétée pour être dessinée.
 * Chaque symbole a au plus une règle, sans contexte ni poids.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public class SystemeDeterministe extends SystemeLindenmeyer implements Generation {
  /**
   * Construit une nouvelle instance de la classe SystemeDeterministe.
   * @param axiomes les axiomes du système de Lindenmayer.
   * @param regles Les règles produisant la chaîne de caractère servant à construire le système.
   * @param angle L'angle (en degrés) pour tourner la figure graphique selon le symbole rencontré.
   * @param iteration Le nombre de fois que l'on construit les règles du système.
   * @param longueur La longueur du dessin.
   * @throws IllegalArgumentException si une règle est mal formée, contextuelle, pondérée ou répétée pour un même symbole
   */
  public SystemeDeterministe(String axiomes, String regles, double angle, int iteration, int longueur) {
    super(axiomes, regles, angle, iteration, longueur);
    verifieRegles("déterministe", false, false, false);
  }
  /**
   * Construit le système préconfiguré : la courbe du dragon (axiome « X Y », règles X=X+YF+ Y=-FX-Y, 90°).
   */
  public SystemeDeterministe() {
    this("X Y", "X=X+YF+ Y=-FX-Y", 90, 10, 6);
  }
}
