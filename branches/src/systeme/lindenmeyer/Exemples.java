package systeme.lindenmeyer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lecture du fichier d'exemples (par défaut {@code systeme/affichage/exemples.txt}, en UTF-8) :
 * on peut ajouter des exemples sans recompiler.
 * <p>
 * Format :
 * <ul>
 * <li>les exemples sont séparés par une ligne {@code ---} ;</li>
 * <li>chaque ligne a la forme {@code clé: valeur} ; on coupe au <b>premier</b> {@code :}, donc une valeur
 * peut contenir {@code :} ;</li>
 * <li>une ligne qui commence par au moins deux espaces prolonge la valeur précédente (les morceaux sont
 * réunis par une espace) ;</li>
 * <li>les lignes qui commencent par {@code #} et les lignes vides sont ignorées.</li>
 * </ul>
 * Clés : {@code nom}, {@code type} (Déterministe, Stochastique, Contextuel, Paramétrique), {@code axiome}, {@code regles}
 * (peut être vide), {@code angle}, {@code iterations}, {@code longueur}, et, facultatives, {@code description},
 * {@code observer}, {@code essayer}.
 * <p>
 * Un exemple invalide (clé inconnue ou manquante, nombre illisible, règle refusée…) est ignoré et signalé,
 * sans empêcher la lecture des autres.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
public final class Exemples {
    private static final Set<String> CLES = Set.of("nom", "type", "axiome", "regles", "angle", "iterations",
        "longueur", "description", "observer", "essayer");
    private static final List<String> CLES_OBLIGATOIRES = List.of("nom", "type", "axiome", "regles", "angle",
        "iterations", "longueur");

    /** Le résultat d'un chargement : les exemples utilisables et les messages à montrer à l'utilisateur. */
    public static final class Catalogue {
        /** Les exemples, dans l'ordre du fichier (ou les exemples intégrés en cas de repli). */
        public final List<Exemple> exemples;
        /** Les problèmes rencontrés (exemples ignorés, fichier absent…). */
        public final List<String> messages;
        /** true si le fichier est absent ou inutilisable et que les exemples intégrés le remplacent. */
        public final boolean repli;

        Catalogue(List<Exemple> exemples, List<String> messages, boolean repli) {
            this.exemples = Collections.unmodifiableList(exemples);
            this.messages = Collections.unmodifiableList(messages);
            this.repli = repli;
        }
    }

    private Exemples() {
    }

    /**
     * Les exemples intégrés au programme, utilisés si le fichier d'exemples est absent ou inutilisable.
     * @return les quatre exemples d'origine
     */
    public static List<Exemple> integres() {
        return List.of(
            new Exemple("Courbe fractale (déterministe)", TypeSysteme.DETERMINISTE, "X Y", "X=X+YF+ Y=-FX-Y", 90, 10, 6,
                "", "", ""),
            new Exemple("Plante fractale (déterministe)", TypeSysteme.DETERMINISTE, "F X", "X=F-[[X]+X]+F[+FX]-X F=FF",
                25, 5, 7, "", "", ""),
            new Exemple("Arbre (stochastique)", TypeSysteme.STOCHASTIQUE, "F", "F=F[+F]F[-F]F F=F[+F]F F=F[-F]F",
                25.7, 5, 7, "", "", ""),
            new Exemple("Plume à signal (contextuel)", TypeSysteme.CONTEXTUEL, "SFFFFFFFFFF",
                "S= T= S<F=F[+FF]T T<F=F[-FF]S", 30, 10, 40, "", "", ""));
    }

    /**
     * Charge les exemples d'un flux UTF-8, ou se replie sur les exemples intégrés si le flux est absent,
     * illisible ou ne contient aucun exemple valide.
     * @param flux le contenu du fichier d'exemples, ou null s'il est introuvable (le flux est fermé)
     * @return le catalogue
     */
    public static Catalogue charge(InputStream flux) {
        if (flux == null)
            return new Catalogue(integres(), List.of("Fichier exemples.txt introuvable : exemples intégrés utilisés."), true);
        try (Reader lecteur = new InputStreamReader(flux, StandardCharsets.UTF_8)) {
            Catalogue lu = lis(lecteur);
            if (!lu.exemples.isEmpty())
                return lu;
            List<String> messages = new ArrayList<>(lu.messages);
            messages.add("Aucun exemple valide dans exemples.txt : exemples intégrés utilisés.");
            return new Catalogue(integres(), messages, true);
        } catch (IOException e) {
            return new Catalogue(integres(), List.of("Lecture de exemples.txt impossible (" + e.getMessage()
                + ") : exemples intégrés utilisés."), true);
        }
    }

    /**
     * Lit des exemples (sans repli).
     * @param lecteur le texte du fichier d'exemples
     * @return les exemples valides et les messages sur les exemples ignorés
     * @throws IOException si la lecture échoue
     */
    public static Catalogue lis(Reader lecteur) throws IOException {
        List<Exemple> exemples = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        BufferedReader lignes = new BufferedReader(lecteur);
        Map<String, String> bloc = new LinkedHashMap<>();
        List<String> erreurs = new ArrayList<>();
        String derniereCle = null;
        int numero = 0, debutBloc = 1;
        String ligne;
        while ((ligne = lignes.readLine()) != null) {
            numero++;
            if (numero == 1 && ligne.startsWith("﻿"))
                ligne = ligne.substring(1);
            if (ligne.startsWith("#") || ligne.trim().isEmpty())
                continue;
            if (ligne.trim().equals("---")) {
                termineBloc(bloc, erreurs, debutBloc, exemples, messages);
                bloc.clear();
                erreurs.clear();
                derniereCle = null;
                debutBloc = numero + 1;
                continue;
            }
            if (ligne.startsWith("  ")) {
                if (derniereCle == null)
                    erreurs.add("ligne " + numero + " : suite de ligne sans clé avant elle");
                else
                    bloc.put(derniereCle, (bloc.get(derniereCle) + " " + ligne.trim()).trim());
                continue;
            }
            int deuxPoints = ligne.indexOf(':');
            if (deuxPoints < 0) {
                erreurs.add("ligne " + numero + " : « " + ligne.trim() + " » n'a pas la forme « clé: valeur »");
                continue;
            }
            String cle = ligne.substring(0, deuxPoints).trim();
            String valeur = ligne.substring(deuxPoints + 1).trim();
            if (!CLES.contains(cle))
                erreurs.add("ligne " + numero + " : clé inconnue « " + cle + " »");
            else if (bloc.containsKey(cle))
                erreurs.add("ligne " + numero + " : clé « " + cle + " » répétée");
            else {
                bloc.put(cle, valeur);
                derniereCle = cle;
            }
        }
        termineBloc(bloc, erreurs, debutBloc, exemples, messages);
        return new Catalogue(exemples, messages, false);
    }

    private static void termineBloc(Map<String, String> bloc, List<String> erreurs, int debut, List<Exemple> exemples,
                                    List<String> messages) {
        if (bloc.isEmpty() && erreurs.isEmpty())
            return;
        String nom = bloc.getOrDefault("nom", "sans nom (ligne " + debut + ")");
        try {
            if (!erreurs.isEmpty())
                throw new IllegalArgumentException(String.join(" ; ", erreurs));
            for (String cle : CLES_OBLIGATOIRES)
                if (!bloc.containsKey(cle))
                    throw new IllegalArgumentException("clé « " + cle + " » manquante");
            Exemple exemple = new Exemple(nom, TypeSysteme.depuisNom(bloc.get("type")), bloc.get("axiome"),
                bloc.get("regles"), nombre(bloc.get("angle"), "angle"), entier(bloc.get("iterations"), "iterations", 0),
                entier(bloc.get("longueur"), "longueur", 1), bloc.get("description"), bloc.get("observer"),
                bloc.get("essayer"));
            if (exemple.axiome().trim().isEmpty())
                throw new IllegalArgumentException("l'axiome est vide");
            exemple.creeSysteme(0L);
            exemples.add(exemple);
        } catch (IllegalArgumentException e) {
            messages.add("Exemple « " + nom + " » ignoré : " + e.getMessage());
        }
    }

    private static double nombre(String texte, String cle) {
        try {
            double valeur = Double.parseDouble(texte.trim().replace(',', '.'));
            if (Double.isFinite(valeur))
                return valeur;
        } catch (NumberFormatException e) {
            // Le message d'erreur est levé ci-dessous.
        }
        throw new IllegalArgumentException(cle + " : « " + texte + " » n'est pas un nombre");
    }

    private static int entier(String texte, String cle, int minimum) {
        try {
            int valeur = Integer.parseInt(texte.trim());
            if (valeur >= minimum)
                return valeur;
        } catch (NumberFormatException e) {
            // Le message d'erreur est levé ci-dessous.
        }
        throw new IllegalArgumentException(cle + " : « " + texte + " » n'est pas un entier supérieur ou égal à " + minimum);
    }
}
