package systeme.lindenmeyer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Une classe abstraite qui représente un système de Lindenmayer, fournissant le moteur de réécriture
 * (règles simples, pondérées ou contextuelles) et l'interprétation de la chaîne obtenue par une tortue 3D
 * (voir {@link #interprete(Mot, double, double)}). Le moteur n'utilise pas JavaFX : le dessin est fait
 * par le paquetage {@code systeme.affichage}.
 * <p>
 * Les classes filles ne choisissent que les types de règles qu'elles acceptent
 * (voir {@link #verifieRegles}) et le générateur aléatoire utilisé (voir {@link #generateurAleatoire()}).
 * Les systèmes paramétriques ({@link SystemeParametrique}) remplacent aussi la réécriture elle-même.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public abstract class SystemeLindenmeyer implements Generation {
    /** Nombre maximal de symboles d'une chaîne générée (évite de figer ou de saturer la mémoire). */
    public static final int MAX_SYMBOLES = 2_000_000;
    /** Au-delà de cette longueur, la provenance des règles d'une génération n'est pas conservée. */
    public static final int MAX_SYMBOLES_PROVENANCE = 20_000;
    /**
     * Coût minimal (en symboles) compté pour chaque génération conservée par une {@link Derivation} :
     * borne aussi le nombre de générations gardées quand la chaîne ne grandit pas.
     */
    private static final int COUT_MINIMAL_GENERATION = 64;
    /** Taille de l'état de la tortue empilé par « [ » : position, H, L, U (3 nombres chacun), largeur, couleur. */
    private static final int ETAT = 14;

    /**
     * Le nombre d'itérations.
     * La longueur du dessin.
     * L'angle du dessin (en degrés).
     * L'axiome(s) du système.
     * La (ou les) règle(s) du système.
     */
    private int iteration, longueur;
    private double angle;
    private String axiomes, regles;
    /** Les règles analysées, regroupées par symbole réécrit. */
    private final List<Regle> reglesAnalysees;
    private final Map<Character, List<Regle>> reglesParSymbole = new HashMap<>();
    /** Le numéro de chaque règle (dans l'ordre d'écriture), pour la provenance. */
    private final Map<Regle, Integer> numeros = new IdentityHashMap<>();
    private final boolean aDesContextes;

    /**
     * Construit une nouvelle instance d'un système de Lindenmayer.
     * @param axiomes le (ou les) axiome(s) du système
     * @param regles la (ou les) règle(s) du système, séparées par des espaces (voir {@link Regle})
     * @param angle l'angle (en degrés) pour tourner lors de l'affichage
     * @param iteration le nombre d'itérations
     * @param longueur la longueur du dessin
     * @throws IllegalArgumentException si une règle est mal formée
    */
    public SystemeLindenmeyer(String axiomes, String regles, double angle, int iteration, int longueur) {
        this(axiomes, regles, angle, iteration, longueur, true);
    }

    /**
     * Construit une nouvelle instance d'un système de Lindenmayer.
     * @param axiomes le (ou les) axiome(s) du système
     * @param regles la (ou les) règle(s) du système
     * @param angle l'angle (en degrés) pour tourner lors de l'affichage
     * @param iteration le nombre d'itérations
     * @param longueur la longueur du dessin
     * @param reglesClassiques false si la classe fille analyse elle-même ses règles (systèmes paramétriques)
     * @throws IllegalArgumentException si une règle est mal formée
     */
    protected SystemeLindenmeyer(String axiomes, String regles, double angle, int iteration, int longueur,
                                 boolean reglesClassiques) {
        this.axiomes = axiomes == null ? "" : axiomes;
        this.regles = regles == null ? "" : regles;
        this.angle = angle;
        this.iteration = iteration;
        this.longueur = longueur;
        this.reglesAnalysees = reglesClassiques ? Regle.analyse(this.regles) : new ArrayList<>();
        boolean contextes = false;
        for (Regle regle : reglesAnalysees) {
            reglesParSymbole.computeIfAbsent(regle.symbole(), s -> new ArrayList<>()).add(regle);
            numeros.put(regle, numeros.size());
            contextes |= regle.specificite() > 0;
        }
        this.aDesContextes = contextes;
    }

    @Override
    public double getAngle() {
        return this.angle;
    }
    @Override
    public int getIteration() {
        return this.iteration;
    }
    /**
     * Retourne le (ou les) axiome(s) du L-Système.
     * @return le (ou les) axiome(s) du L-Système
     */
    public String getAxiomes() {
        return this.axiomes;
    }
    /**
     * Retourne la (ou les) règle(s) du L-Système.
     * @return la (ou les) règle(s) du L-Système
     */
    public String getRegles() {
        return this.regles;
    }
    /**
     * Retourne les règles analysées, dans l'ordre où elles ont été écrites.
     * @return la liste (non modifiable) des règles du système (vide pour un système paramétrique)
     */
    public List<Regle> getReglesAnalysees() {
        return Collections.unmodifiableList(reglesAnalysees);
    }
    @Override
    public int getLongueur() {
        return this.longueur;
    }

    /**
     * Refuse les types de règles que le système ne gère pas.
     * @param type le nom du type de système (pour le message d'erreur)
     * @param accepteContextes true si les règles contextuelles sont acceptées
     * @param acceptePoids true si les poids sont acceptés
     * @param accepteRepetitions true si un symbole peut avoir plusieurs règles
     * @throws IllegalArgumentException si une règle n'est pas acceptée
     */
    protected final void verifieRegles(String type, boolean accepteContextes, boolean acceptePoids, boolean accepteRepetitions) {
        Map<Character, Regle> vues = new HashMap<>();
        for (Regle regle : reglesAnalysees) {
            if (!accepteContextes && regle.specificite() > 0)
                throw new IllegalArgumentException("Règle « " + regle + " » : un système " + type
                    + " n'accepte pas de contexte (choisissez le type contextuel ou stochastique).");
            if (!acceptePoids && regle.poidsExplicite())
                throw new IllegalArgumentException("Règle « " + regle + " » : un système " + type
                    + " n'accepte pas de poids (choisissez le type stochastique).");
            Regle precedente = vues.put(regle.symbole(), regle);
            if (!accepteRepetitions && precedente != null)
                throw new IllegalArgumentException("Règles « " + precedente + " » et « " + regle + " » : un système "
                    + type + " n'accepte qu'une règle par symbole (choisissez le type stochastique).");
        }
    }

    /**
     * Fournit le générateur aléatoire servant à départager plusieurs règles applicables.
     * @return le générateur, ou null pour toujours retenir la première règle applicable
     */
    protected Random generateurAleatoire() {
        return null;
    }

    @Override
    public String genereChaine(int nbIterations) {
        Derivation derivation = derive();
        for (int i = 0; i < nbIterations; i++)
            derivation.avance();
        return derivation.chaine(derivation.nbGenerations() - 1);
    }

    /**
     * Commence une dérivation : la génération 0 est l'axiome sans espaces, les suivantes se calculent
     * une à une avec {@link Derivation#suivante()}. Le générateur aléatoire est pris une fois pour toutes
     * (voir {@link #generateurAleatoire()}) : toutes les générations viennent d'un seul tirage.
     * @return une nouvelle dérivation, qui ne contient que l'axiome
     */
    public Derivation derive() {
        return new Derivation();
    }

    /**
     * La génération 0 (l'axiome, sans espaces).
     * @return le mot de départ
     */
    Mot motInitial() {
        return new Mot(axiomes.replaceAll("\\s+", ""));
    }

    /**
     * Applique une fois les règles à tout le mot (réécriture parallèle).
     * @param mot le mot à réécrire
     * @param aleatoire le générateur pour départager les règles, ou null
     * @param provenance reçoit (début, fin, numéro de règle) pour chaque règle appliquée, ou null
     * @return le mot réécrit
     * @throws IllegalArgumentException si le résultat dépasse {@link #MAX_SYMBOLES} symboles
     */
    Mot reecrire(Mot mot, Random aleatoire, ListeEntiers provenance) {
        return new Mot(reecrire(mot.symboles(), aleatoire, provenance));
    }

    /**
     * Relit le texte d'une génération (tel que {@link #genereChaine(int)} le renvoie).
     * @param chaine le texte
     * @return le mot
     */
    Mot versMot(String chaine) {
        return new Mot(chaine);
    }

    /**
     * Les générations successives d'un système : l'axiome (génération 0), puis chaque réécriture.
     * <p>
     * Mémoire bornée : la dernière génération est toujours conservée ; les précédentes le sont tant que
     * la somme des longueurs conservées ne dépasse pas {@link #MAX_SYMBOLES}. Au-delà, les générations
     * les plus récentes (donc les plus longues) sont libérées en premier, et seule leur longueur reste
     * connue ({@link #estStockee(int)} renvoie alors false).
     * <p>
     * Sur demande ({@link #avecProvenance(boolean)}), chaque génération d'au plus
     * {@link #MAX_SYMBOLES_PROVENANCE} symboles garde la trace des règles qui l'ont produite.
     */
    public final class Derivation {
        private final Random aleatoire;
        /** Les mots de chaque génération (null quand elle n'est plus conservée). */
        private final List<Mot> mots = new ArrayList<>();
        /** La provenance de chaque génération (null si elle n'a pas été enregistrée). */
        private final List<int[]> provenances = new ArrayList<>();
        private int[] longueurs = new int[16];
        private long coutConserve = 0;
        private boolean provenance = false;

        private Derivation() {
            this.aleatoire = generateurAleatoire();
            ajoute(motInitial(), null);
        }

        /**
         * Active l'enregistrement de la provenance des règles pour les générations calculées ensuite
         * (désactivé par défaut pour ne pas ralentir le moteur).
         * @param active true pour enregistrer la provenance
         * @return cette dérivation
         */
        public Derivation avecProvenance(boolean active) {
            provenance = active;
            return this;
        }

        /**
         * Retourne le nombre de générations calculées (au moins 1 : l'axiome est la génération 0).
         * @return le nombre de générations calculées (au moins 1 : l'axiome est la génération 0)
         */
        public int nbGenerations() {
            return mots.size();
        }

        /**
         * Retourne la chaîne d'une génération (avec les paramètres pour un système paramétrique).
         * @param k le numéro de la génération (0 = l'axiome)
         * @return la chaîne de la génération k
         * @throws IndexOutOfBoundsException si la génération k n'a pas été calculée
         * @throws IllegalStateException si la génération k n'est plus conservée en mémoire
         */
        public String chaine(int k) {
            return mot(k).texte();
        }

        /**
         * Retourne une génération sous forme de modules (symboles et paramètres).
         * @param k le numéro de la génération (0 = l'axiome)
         * @return le mot de la génération k
         * @throws IndexOutOfBoundsException si la génération k n'a pas été calculée
         * @throws IllegalStateException si la génération k n'est plus conservée en mémoire
         */
        public Mot mot(int k) {
            verifieNumero(k);
            Mot mot = mots.get(k);
            if (mot == null)
                throw new IllegalStateException("La génération " + k + " (" + longueurs[k]
                    + " symboles) n'est plus conservée en mémoire : seule sa longueur est connue.");
            return mot;
        }

        /**
         * Retourne la longueur d'une génération, même si sa chaîne n'est plus conservée.
         * @param k le numéro de la génération
         * @return le nombre de symboles (modules) de la génération k
         * @throws IndexOutOfBoundsException si la génération k n'a pas été calculée
         */
        public int longueur(int k) {
            verifieNumero(k);
            return longueurs[k];
        }

        /**
         * Indique si la chaîne d'une génération est encore conservée.
         * @param k le numéro de la génération
         * @return true si {@link #chaine(int)} peut renvoyer la génération k
         * @throws IndexOutOfBoundsException si la génération k n'a pas été calculée
         */
        public boolean estStockee(int k) {
            verifieNumero(k);
            return mots.get(k) != null;
        }

        /**
         * Retourne la provenance d'une génération : pour chaque règle appliquée, trois entiers
         * (début, fin, numéro de la règle) ; les modules [début, fin) de la génération k viennent de cette règle.
         * Les symboles recopiés sans règle n'y figurent pas.
         * @param k le numéro de la génération
         * @return les triplets, ou null (génération 0, provenance désactivée ou génération trop longue)
         * @throws IndexOutOfBoundsException si la génération k n'a pas été calculée
         */
        public int[] provenance(int k) {
            verifieNumero(k);
            return provenances.get(k);
        }

        /**
         * Calcule la génération suivante (en réécrivant la dernière) et la conserve.
         * @return la nouvelle génération
         * @throws IllegalArgumentException si elle dépasse {@link SystemeLindenmeyer#MAX_SYMBOLES} symboles
         */
        public String suivante() {
            return avance().texte();
        }

        /**
         * Calcule la génération suivante, comme {@link #suivante()}, sans écrire son texte.
         * @return la nouvelle génération
         * @throws IllegalArgumentException si elle dépasse {@link SystemeLindenmeyer#MAX_SYMBOLES} symboles
         */
        public Mot avance() {
            ListeEntiers liste = provenance ? new ListeEntiers() : null;
            Mot suivant = reecrire(mots.get(mots.size() - 1), aleatoire, liste);
            ajoute(suivant, liste == null || liste.estAbandonnee() ? null : liste.tableau());
            return suivant;
        }

        private void ajoute(Mot mot, int[] origine) {
            int k = mots.size();
            if (k == longueurs.length)
                longueurs = Arrays.copyOf(longueurs, 2 * k);
            longueurs[k] = mot.taille();
            mots.add(mot);
            provenances.add(origine);
            coutConserve += cout(mot);
            // On libère d'abord les générations les plus récentes : ce sont les plus longues.
            for (int j = k - 1; j >= 0 && coutConserve > MAX_SYMBOLES; j--) {
                Mot ancien = mots.get(j);
                if (ancien != null) {
                    mots.set(j, null);
                    provenances.set(j, null);
                    coutConserve -= cout(ancien);
                }
            }
        }

        private long cout(Mot mot) {
            return Math.max(COUT_MINIMAL_GENERATION, mot.cout());
        }

        private void verifieNumero(int k) {
            if (k < 0 || k >= mots.size())
                throw new IndexOutOfBoundsException("Génération " + k + " non calculée (0 à "
                    + (mots.size() - 1) + ").");
        }
    }

    /**
     * Applique une fois les règles à tous les symboles de la chaîne (réécriture parallèle).
     * Les symboles sans règle applicable sont conservés tels quels.
     * @param chaine la chaîne à réécrire
     * @param aleatoire le générateur pour départager les règles, ou null
     * @param provenance reçoit (début, fin, numéro de règle) pour chaque règle appliquée, ou null
     * @return la chaîne réécrite
     * @throws IllegalArgumentException si le résultat dépasse {@link #MAX_SYMBOLES} symboles
     */
    private String reecrire(String chaine, Random aleatoire, ListeEntiers provenance) {
        int[] crochets = aDesContextes ? apparieCrochets(chaine) : null;
        StringBuilder resultat = new StringBuilder(chaine.length() * 2);
        for (int i = 0; i < chaine.length(); i++) {
            Regle regle = choisitRegle(chaine, i, crochets, aleatoire);
            if (regle == null) {
                resultat.append(chaine.charAt(i));
            } else {
                int debut = resultat.length();
                resultat.append(regle.remplacement());
                if (provenance != null && resultat.length() > debut) {
                    provenance.ajoute(debut);
                    provenance.ajoute(resultat.length());
                    provenance.ajoute(numeros.get(regle));
                }
            }
            if (provenance != null && resultat.length() > MAX_SYMBOLES_PROVENANCE)
                provenance.abandonne();
            if (resultat.length() > MAX_SYMBOLES)
                throw new IllegalArgumentException("La chaîne générée dépasse " + MAX_SYMBOLES
                    + " symboles : réduisez le nombre d'itérations.");
        }
        return resultat.toString();
    }

    /**
     * Choisit la règle à appliquer au symbole à la position donnée : parmi les règles applicables,
     * on garde celles qui ont le plus de contextes, puis on tire au sort selon les poids (ou on prend la première).
     * @return la règle choisie, ou null si aucune règle ne s'applique
     */
    private Regle choisitRegle(String chaine, int position, int[] crochets, Random aleatoire) {
        List<Regle> candidates = reglesParSymbole.get(chaine.charAt(position));
        if (candidates == null)
            return null;
        if (candidates.size() == 1) {
            Regle seule = candidates.get(0);
            return correspond(seule, chaine, position, crochets) ? seule : null;
        }
        List<Regle> applicables = new ArrayList<>(candidates.size());
        int specificiteMax = -1;
        for (Regle regle : candidates) {
            if (regle.specificite() < specificiteMax || !correspond(regle, chaine, position, crochets))
                continue;
            if (regle.specificite() > specificiteMax) {
                specificiteMax = regle.specificite();
                applicables.clear();
            }
            applicables.add(regle);
        }
        if (applicables.isEmpty())
            return null;
        if (aleatoire == null || applicables.size() == 1)
            return applicables.get(0);
        double total = 0;
        for (Regle regle : applicables)
            total += regle.poids();
        double seuil = aleatoire.nextDouble() * total;
        for (Regle regle : applicables) {
            seuil -= regle.poids();
            if (seuil < 0)
                return regle;
        }
        return applicables.get(applicables.size() - 1);
    }

    /** Vérifie que les contextes de la règle correspondent à ceux du symbole à cette position. */
    private boolean correspond(Regle regle, String chaine, int position, int[] crochets) {
        if (regle.gauche() != Regle.AUCUN && contexteGauche(chaine, position, crochets) != regle.gauche())
            return false;
        return regle.droite() == Regle.AUCUN || contexteDroit(chaine, position, crochets) == regle.droite();
    }

    /**
     * Associe chaque crochet à son crochet correspondant.
     * @return un tableau donnant, pour chaque position, la position du crochet associé (-1 si aucun)
     */
    private static int[] apparieCrochets(String chaine) {
        int[] associes = new int[chaine.length()];
        Arrays.fill(associes, -1);
        int[] pile = new int[chaine.length()];
        int hauteur = 0;
        for (int i = 0; i < chaine.length(); i++) {
            char c = chaine.charAt(i);
            if (c == '[') {
                pile[hauteur++] = i;
            } else if (c == ']' && hauteur > 0) {
                int ouvrant = pile[--hauteur];
                associes[ouvrant] = i;
                associes[i] = ouvrant;
            }
        }
        return associes;
    }

    /**
     * Cherche le symbole qui précède celui de la position donnée. Les branches déjà fermées sont sautées,
     * et pour un symbole en début de branche, le contexte gauche est le symbole qui précède la branche.
     * @return le symbole trouvé, ou {@link Regle#AUCUN}
     */
    private static char contexteGauche(String chaine, int position, int[] crochets) {
        int j = position - 1;
        while (j >= 0) {
            char c = chaine.charAt(j);
            if (c == ']' && crochets[j] >= 0)
                j = crochets[j] - 1;
            else if (c == '[' || c == ']')
                j--;
            else
                return c;
        }
        return Regle.AUCUN;
    }

    /**
     * Cherche le symbole qui suit celui de la position donnée. Les branches sont sautées,
     * et la fin d'une branche n'a pas de contexte droit.
     * @return le symbole trouvé, ou {@link Regle#AUCUN}
     */
    private static char contexteDroit(String chaine, int position, int[] crochets) {
        int j = position + 1;
        while (j < chaine.length()) {
            char c = chaine.charAt(j);
            if (c == '[' && crochets[j] >= 0)
                j = crochets[j] + 1;
            else if (c == '[')
                j++;
            else if (c == ']')
                return Regle.AUCUN;
            else
                return c;
        }
        return Regle.AUCUN;
    }

    /**
     * Interprète la chaîne comme des instructions données à une tortue 3D, sans rien dessiner
     * (voir {@link #interprete(Mot, double, double)}).
     *
     * @param chaine les instructions (pour un système paramétrique : le texte d'une génération, paramètres compris)
     * @param longueur la longueur d'un pas
     * @param angleDegres l'angle de chaque virage, en degrés
     * @return les segments tracés
     * @throws IllegalArgumentException si un crochet fermant n'a pas de crochet ouvrant, si une accolade fermante
     *         n'a pas d'accolade ouvrante ou si des polygones sont imbriqués
     */
    public Trace interprete(String chaine, double longueur, double angleDegres) {
        return interprete(versMot(chaine), longueur, angleDegres);
    }

    /**
     * Interprète un mot comme des instructions données à une tortue 3D, sans rien dessiner.
     * <p>
     * La tortue part de l'origine avec le repère H = (0, 1, 0) (cap, vers le haut de l'écran),
     * L = (−1, 0, 0) (gauche) et U = (0, 0, 1) (haut de la tortue, vers l'observateur) :
     * <ul>
     * <li>{@code F} avance de {@code longueur} en traçant un segment, {@code f} avance sans tracer ;</li>
     * <li>{@code +} / {@code -} : lacet, tourne à gauche / à droite autour de U ;</li>
     * <li>{@code &} / {@code ^} : tangage, pique / cabre autour de L ;</li>
     * <li>{@code \} / {@code /} : roulis à gauche / à droite autour de H ;</li>
     * <li>{@code |} : demi-tour ;</li>
     * <li>{@code !} : largeur × 0,7 (sans descendre sous {@link Trace#LARGEUR_MIN}) ;
     * {@code '} : couleur suivante de la palette ;</li>
     * <li>{@code [} / {@code ]} : empile / dépile l'état complet de la tortue (position, repère, largeur, couleur) ;</li>
     * <li><code>&#123;</code> / <code>&#125;</code> : ouvre / ferme un polygone rempli (une feuille). Entre les deux, {@code F}
     * et {@code f} ne tracent plus de segment : chacun avance et ajoute la nouvelle position comme sommet
     * (la position d'ouverture est le premier sommet). Les polygones ne s'imbriquent pas ; un polygone jamais
     * fermé est ignoré.</li>
     * </ul>
     * Sans paramètre, tous les virages utilisent le même angle : pour un autre angle, répéter le symbole.
     * Avec un paramètre (systèmes paramétriques), {@code F(l)} et {@code f(l)} avancent de l × longueur,
     * {@code +(a)}, {@code -(a)}, {@code &(a)}, {@code ^(a)}, {@code \(a)} et {@code /(a)} tournent de a degrés,
     * {@code !(w)} fixe la largeur relative à w et {@code '(i)} choisit la couleur i.
     * Les autres symboles sont ignorés. Avec seulement {@code F f + - [ ]}, la figure reste dans le plan z = 0.
     *
     * @param mot les instructions
     * @param longueur la longueur d'un pas
     * @param angleDegres l'angle de chaque virage, en degrés
     * @return les segments tracés ; {@link Trace#indexSymbole} donne le numéro du module {@code F} (pour un mot
     *         sans paramètres, c'est la position du caractère dans la chaîne)
     * @throws IllegalArgumentException si un crochet fermant n'a pas de crochet ouvrant, si une accolade fermante
     *         n'a pas d'accolade ouvrante ou si des polygones sont imbriqués
     */
    public Trace interprete(Mot mot, double longueur, double angleDegres) {
        Trace trace = new Trace();
        double delta = Math.toRadians(angleDegres), c = Math.cos(delta), s = Math.sin(delta);
        double[] p = {0, 0, 0}, q = new double[3];
        double[] h = {0, 1, 0}, l = {-1, 0, 0}, u = {0, 0, 1};
        double largeur = 1;
        int couleur = 0;
        // Polygone en cours : ses sommets (x, y, z répétés), ou null.
        double[] polygone = null;
        int nbCoordonnees = 0, ouverturePolygone = -1, couleurPolygone = 0;
        // Pile des états : 14 nombres par niveau (position, H, L, U, largeur, couleur).
        double[] pile = new double[ETAT * 16];
        int hauteur = 0;
        boolean parametres = mot.aDesParametres();
        for (int i = 0; i < mot.taille(); i++) {
            char symbole = mot.symbole(i);
            boolean avecValeur = parametres && mot.nbParametres(i) > 0;
            double valeur = avecValeur ? mot.parametre(i, 0) : 0;
            double cv = c, sv = s;
            if (avecValeur && "+-&^\\/".indexOf(symbole) >= 0) {
                cv = Math.cos(Math.toRadians(valeur));
                sv = Math.sin(Math.toRadians(valeur));
            }
            switch (symbole) {
                case 'F':
                case 'f':
                    double pas = avecValeur ? valeur * longueur : longueur;
                    for (int a = 0; a < 3; a++)
                        q[a] = p[a] + pas * h[a];
                    if (polygone != null) {
                        if (nbCoordonnees + 3 > polygone.length)
                            polygone = Arrays.copyOf(polygone, 2 * polygone.length);
                        System.arraycopy(q, 0, polygone, nbCoordonnees, 3);
                        nbCoordonnees += 3;
                    } else if (symbole == 'F') {
                        trace.ajoute(p, q, i, hauteur, largeur, couleur);
                    }
                    double[] echange = p;
                    p = q;
                    q = echange;
                    break;
                case '+':
                    tourne(h, l, cv, sv);
                    break;
                case '-':
                    tourne(h, l, cv, -sv);
                    break;
                case '&':
                    tourne(h, u, cv, -sv);
                    break;
                case '^':
                    tourne(h, u, cv, sv);
                    break;
                case '\\':
                    tourne(l, u, cv, -sv);
                    break;
                case '/':
                    tourne(l, u, cv, sv);
                    break;
                case '|':
                    for (int a = 0; a < 3; a++) {
                        h[a] = -h[a];
                        l[a] = -l[a];
                    }
                    break;
                case '{':
                    if (polygone != null)
                        throw new IllegalArgumentException("L'accolade ouvrante « { » (position " + (i + 1)
                            + " de la chaîne générée) est dans le polygone ouvert en position " + (ouverturePolygone + 1)
                            + " : les polygones imbriqués ne sont pas pris en charge.");
                    polygone = new double[3 * 8];
                    System.arraycopy(p, 0, polygone, 0, 3);
                    nbCoordonnees = 3;
                    ouverturePolygone = i;
                    couleurPolygone = couleur;
                    break;
                case '}':
                    if (polygone == null)
                        throw new IllegalArgumentException("L'accolade fermante « } » (position " + (i + 1)
                            + " de la chaîne générée) n'a pas d'accolade ouvrante correspondante.");
                    termine(trace, polygone, nbCoordonnees, couleurPolygone, i, longueur);
                    polygone = null;
                    break;
                case '!':
                    largeur = avecValeur ? Math.max(0, valeur) : Math.max(Trace.LARGEUR_MIN, largeur * 0.7);
                    trace.largeursExplicites = true;
                    break;
                case '\'':
                    couleur = avecValeur ? (int) Math.max(0, Math.min(Trace.TAILLE_PALETTE - 1, Math.round(valeur)))
                        : Math.min(couleur + 1, Trace.TAILLE_PALETTE - 1);
                    trace.couleursExplicites = true;
                    break;
                case '[':
                    if (ETAT * (hauteur + 1) > pile.length)
                        pile = Arrays.copyOf(pile, 2 * pile.length);
                    int sommet = ETAT * hauteur;
                    System.arraycopy(p, 0, pile, sommet, 3);
                    System.arraycopy(h, 0, pile, sommet + 3, 3);
                    System.arraycopy(l, 0, pile, sommet + 6, 3);
                    System.arraycopy(u, 0, pile, sommet + 9, 3);
                    pile[sommet + 12] = largeur;
                    pile[sommet + 13] = couleur;
                    hauteur++;
                    break;
                case ']':
                    if (hauteur == 0)
                        throw new IllegalArgumentException("Le crochet fermant « ] » (position " + (i + 1)
                            + " de la chaîne générée) n'a pas de crochet ouvrant correspondant.");
                    hauteur--;
                    int haut = ETAT * hauteur;
                    System.arraycopy(pile, haut, p, 0, 3);
                    System.arraycopy(pile, haut + 3, h, 0, 3);
                    System.arraycopy(pile, haut + 6, l, 0, 3);
                    System.arraycopy(pile, haut + 9, u, 0, 3);
                    largeur = pile[haut + 12];
                    couleur = (int) pile[haut + 13];
                    break;
                default:
                    break;
            }
        }
        return trace;
    }

    /**
     * Ajoute un polygone fermé à la trace : le dernier sommet est retiré s'il revient sur le premier,
     * et un polygone de moins de trois sommets est ignoré.
     */
    private static void termine(Trace trace, double[] polygone, int nbCoordonnees, int couleur, int position,
                                double longueur) {
        double tolerance = 1e-9 * Math.max(1, Math.abs(longueur));
        if (nbCoordonnees >= 6 && Math.abs(polygone[nbCoordonnees - 3] - polygone[0]) <= tolerance
            && Math.abs(polygone[nbCoordonnees - 2] - polygone[1]) <= tolerance
            && Math.abs(polygone[nbCoordonnees - 1] - polygone[2]) <= tolerance)
            nbCoordonnees -= 3;
        if (nbCoordonnees >= 9)
            trace.ajoutePolygone(Arrays.copyOf(polygone, nbCoordonnees), couleur, position);
    }

    /**
     * Fait tourner deux vecteurs du repère de la tortue dans leur plan (les vecteurs sont modifiés en place) :
     * a' = c·a + s·b ; b' = −s·a + c·b.
     */
    private static void tourne(double[] a, double[] b, double c, double s) {
        for (int i = 0; i < 3; i++) {
            double ai = a[i], bi = b[i];
            a[i] = c * ai + s * bi;
            b[i] = -s * ai + c * bi;
        }
    }
}
