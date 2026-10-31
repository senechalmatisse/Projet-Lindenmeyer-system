package systeme.lindenmeyer;

import java.util.Random;

/**
 * Une classe représentant un système de Lindemayer stochastique.
 * Un système stochastique est similaire à un système déterministe sauf qu'un symbole peut avoir
 * plusieurs règles : à chaque réécriture de chaque symbole, l'une d'elles est tirée au hasard selon son poids
 * (écrit entre parenthèses avant le signe '=', par exemple {@code F(2)=FF} ; 1 par défaut).
 * Les règles contextuelles sont aussi acceptées.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public class SystemeStochastique extends SystemeLindenmeyer implements Generation {
  /** Le générateur de nombre aléatoire utilisé pour générer des valeurs aléatoires pour choisir les règles. */
  private final Random rand;

  /**
   * Construit une instance d'un système de Lindemayer stochastique.
   * @param axiomes le (ou les) axiome(s) du système
   * @param regles la (ou les) règle(s) du système
   * @param angle l'angle (en degrés) pour tourner lors de l'affichage
   * @param iteration le nombre d'itérations
   * @param longueur la longueur du dessin
   * @throws IllegalArgumentException si une règle est mal formée
   */
  public SystemeStochastique(String axiomes, String regles, double angle, int iteration, int longueur) {
    this(axiomes, regles, angle, iteration, longueur, new Random());
  }

  /**
   * Construit une instance d'un système de Lindemayer stochastique avec un générateur imposé
   * (par exemple {@code new Random(graine)} pour obtenir toujours la même figure).
   * @param axiomes le (ou les) axiome(s) du système
   * @param regles la (ou les) règle(s) du système
   * @param angle l'angle (en degrés) pour tourner lors de l'affichage
   * @param iteration le nombre d'itérations
   * @param longueur la longueur du dessin
   * @param rand le générateur de nombres aléatoires
   * @throws IllegalArgumentException si une règle est mal formée
   */
  public SystemeStochastique(String axiomes, String regles, double angle, int iteration, int longueur, Random rand) {
    super(axiomes, regles, angle, iteration, longueur);
    this.rand = rand;
  }

  /**
   * Construit une instance d'un système de Lindemayer stochastique à partir d'une graine :
   * deux systèmes construits avec la même graine donnent les mêmes chaînes.
   * @param axiomes le (ou les) axiome(s) du système
   * @param regles la (ou les) règle(s) du système
   * @param angle l'angle (en degrés) pour tourner lors de l'affichage
   * @param iteration le nombre d'itérations
   * @param longueur la longueur du dessin
   * @param graine la graine du générateur de nombres aléatoires
   * @throws IllegalArgumentException si une règle est mal formée
   */
  public SystemeStochastique(String axiomes, String regles, double angle, int iteration, int longueur, long graine) {
    this(axiomes, regles, angle, iteration, longueur, new Random(graine));
  }

  /**
   * Construit le système préconfiguré : un arbre stochastique (axiome F, trois règles pour F, 25,7°).
   */
  public SystemeStochastique() {
    this("F", "F=F[+F]F[-F]F F=F[+F]F F=F[-F]F", 25.7, 5, 7);
  }

  @Override
  protected Random generateurAleatoire() {
    return rand;
  }
}
