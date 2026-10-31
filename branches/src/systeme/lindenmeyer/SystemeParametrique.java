package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Un système de Lindenmayer paramétrique : chaque symbole peut porter des paramètres numériques
 * ({@code F(1.5)}, {@code A(l,w)}), et les règles calculent les paramètres de leur successeur
 * (voir {@link RegleParametrique}) :
 * <pre>
 * axiome : A(1)
 * règle  : A(l) = F(l)[+(30)A(l*0.7)][-(30)A(l*0.7)]
 * </pre>
 * Les parenthèses désignent ici des paramètres (et non un poids comme pour les systèmes stochastiques) ;
 * une condition ({@code :l>0.1}) et un poids ({@code ~2}) peuvent précéder le signe {@code =}. Quand plusieurs
 * règles s'appliquent à un module, l'une est tirée au hasard selon son poids.
 * <p>
 * Interprétation : {@code F(l)} avance de l × longueur, {@code +(a)} tourne de a degrés, {@code !(w)} fixe la
 * largeur ; sans paramètre, les symboles gardent leur sens habituel.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public class SystemeParametrique extends SystemeLindenmeyer implements Generation {
    private final List<RegleParametrique> reglesParametriques;
    private final Map<Character, List<RegleParametrique>> reglesParSymbole = new HashMap<>();
    private final Random rand;
    private final Mot axiome;

    /**
     * Construit un système paramétrique (tirage au hasard entre règles applicables avec un générateur quelconque).
     * @param axiomes l'axiome (modules à paramètres constants, par exemple {@code A(1,10)})
     * @param regles les règles, séparées par des points-virgules (ou des espaces)
     * @param angle l'angle par défaut (en degrés) des virages sans paramètre
     * @param iteration le nombre d'itérations
     * @param longueur la longueur d'un pas ({@code F(l)} avance de l × longueur)
     * @throws IllegalArgumentException si l'axiome ou une règle est mal formé
     */
    public SystemeParametrique(String axiomes, String regles, double angle, int iteration, int longueur) {
        this(axiomes, regles, angle, iteration, longueur, new Random());
    }

    /**
     * Construit un système paramétrique avec une graine (la même graine redonne les mêmes chaînes).
     * @param axiomes l'axiome
     * @param regles les règles
     * @param angle l'angle par défaut (en degrés)
     * @param iteration le nombre d'itérations
     * @param longueur la longueur d'un pas
     * @param graine la graine du générateur aléatoire
     * @throws IllegalArgumentException si l'axiome ou une règle est mal formé
     */
    public SystemeParametrique(String axiomes, String regles, double angle, int iteration, int longueur, long graine) {
        this(axiomes, regles, angle, iteration, longueur, new Random(graine));
    }

    /**
     * Construit un système paramétrique avec un générateur imposé.
     * @param axiomes l'axiome
     * @param regles les règles
     * @param angle l'angle par défaut (en degrés)
     * @param iteration le nombre d'itérations
     * @param longueur la longueur d'un pas
     * @param rand le générateur de nombres aléatoires
     * @throws IllegalArgumentException si l'axiome ou une règle est mal formé
     */
    public SystemeParametrique(String axiomes, String regles, double angle, int iteration, int longueur, Random rand) {
        super(axiomes, regles, angle, iteration, longueur, false);
        this.rand = rand;
        this.reglesParametriques = Collections.unmodifiableList(RegleParametrique.analyse(regles));
        for (RegleParametrique regle : reglesParametriques)
            reglesParSymbole.computeIfAbsent(regle.symbole(), s -> new ArrayList<>()).add(regle);
        try {
            this.axiome = RegleParametrique.mot(getAxiomes());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Axiome « " + getAxiomes().trim() + " » invalide : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne les règles, dans l'ordre où elles ont été écrites.
     * @return les règles, dans l'ordre où elles ont été écrites
     */
    public List<RegleParametrique> getReglesParametriques() {
        return reglesParametriques;
    }

    @Override
    protected Random generateurAleatoire() {
        return rand;
    }

    @Override
    Mot motInitial() {
        return axiome;
    }

    @Override
    Mot versMot(String chaine) {
        return RegleParametrique.mot(chaine);
    }

    @Override
    Mot reecrire(Mot mot, Random aleatoire, ListeEntiers provenance) {
        StringBuilder symboles = new StringBuilder(2 * mot.taille());
        ListeEntiers debuts = new ListeEntiers();
        ListeReels valeurs = new ListeReels();
        List<RegleParametrique> applicables = new ArrayList<>();
        List<Double> poids = new ArrayList<>();
        for (int i = 0; i < mot.taille(); i++) {
            char symbole = mot.symbole(i);
            int n = mot.nbParametres(i);
            double[] parametres = new double[n];
            for (int j = 0; j < n; j++)
                parametres[j] = mot.parametre(i, j);
            RegleParametrique regle = null;
            List<RegleParametrique> candidates = reglesParSymbole.get(symbole);
            if (candidates != null) {
                applicables.clear();
                poids.clear();
                for (RegleParametrique candidate : candidates) {
                    if (candidate.arite() != n)
                        continue;
                    try {
                        if (!candidate.accepte(parametres))
                            continue;
                        double p = candidate.poids(parametres);
                        if (p > 0) {
                            applicables.add(candidate);
                            poids.add(p);
                        }
                    } catch (IllegalArgumentException e) {
                        throw erreur(candidate, symbole, parametres, e);
                    }
                }
                regle = choisit(applicables, poids, aleatoire);
            }
            int debut = symboles.length();
            if (regle == null) {
                symboles.append(symbole);
                debuts.ajoute(valeurs.taille());
                for (double parametre : parametres)
                    valeurs.ajoute(parametre);
            } else {
                try {
                    regle.produit(parametres, symboles, debuts, valeurs);
                } catch (IllegalArgumentException e) {
                    throw erreur(regle, symbole, parametres, e);
                }
                if (provenance != null && symboles.length() > debut) {
                    provenance.ajoute(debut);
                    provenance.ajoute(symboles.length());
                    provenance.ajoute(reglesParametriques.indexOf(regle));
                }
            }
            if (provenance != null && symboles.length() > MAX_SYMBOLES_PROVENANCE)
                provenance.abandonne();
            if (symboles.length() > MAX_SYMBOLES)
                throw new IllegalArgumentException("La chaîne générée dépasse " + MAX_SYMBOLES
                    + " symboles : réduisez le nombre d'itérations.");
        }
        debuts.ajoute(valeurs.taille());
        return new Mot(symboles.toString(), debuts.tableau(), valeurs.tableau());
    }

    private static RegleParametrique choisit(List<RegleParametrique> applicables, List<Double> poids, Random aleatoire) {
        if (applicables.isEmpty())
            return null;
        if (aleatoire == null || applicables.size() == 1)
            return applicables.get(0);
        double total = 0;
        for (double p : poids)
            total += p;
        double seuil = aleatoire.nextDouble() * total;
        for (int i = 0; i < applicables.size(); i++) {
            seuil -= poids.get(i);
            if (seuil < 0)
                return applicables.get(i);
        }
        return applicables.get(applicables.size() - 1);
    }

    private static IllegalArgumentException erreur(RegleParametrique regle, char symbole, double[] parametres,
                                                   IllegalArgumentException cause) {
        StringBuilder module = new StringBuilder().append(symbole);
        if (parametres.length > 0) {
            module.append('(');
            for (int j = 0; j < parametres.length; j++)
                module.append(j > 0 ? "," : "").append(Mot.nombre(parametres[j]));
            module.append(')');
        }
        return new IllegalArgumentException("Règle « " + regle + " » appliquée à " + module + " : " + cause.getMessage(),
            cause);
    }
}
