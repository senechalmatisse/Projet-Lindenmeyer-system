package systeme.affichage;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * La fenêtre d'aide (bouton « ? ») : définitions d'axiome, de règle et d'itération, ordres de la tortue,
 * types de systèmes et gestes de la souris.
 * <p>
 * Le texte utilise une mise en forme minimale : un paragraphe par ligne, {@code # } en début de ligne pour
 * un titre, {@code **gras**} et {@code `code`}.
 *
 * @author Matisse Senechal, Université de Caen Normandie, France
 */
final class FenetreAide {
    /** Le texte de l'aide. */
    static final String TEXTE = String.join("\n",
        "# Comprendre un L-système",
        "**Axiome** : la chaîne de départ (génération 0). Chaque caractère est un symbole ; les espaces sont ignorés.",
        "**Règle** : `S=remplacement`. À chaque itération, **tous** les `S` de la chaîne sont remplacés en même temps "
            + "par `remplacement`. Un symbole sans règle reste inchangé. Les règles sont séparées par des espaces.",
        "**Itération n** : la chaîne obtenue après n réécritures. Elle est ensuite lue de gauche à droite comme une "
            + "suite d'ordres donnés à une « tortue ». Le curseur du panneau « Chaîne générée » montre chaque "
            + "génération, de l'axiome (0) à la dernière ; le bouton « génération suivante » en calcule une de plus.",
        "# Ordres de la tortue",
        "`F` avance en traçant, `f` avance sans tracer, `+` / `-` tournent à gauche / à droite de l'angle, "
            + "`[` mémorise la position et la direction, `]` y revient.",
        "En 3D : `&` / `^` piquent / cabrent, `\\` / `/` font rouler, `|` fait demi-tour. Tous les virages utilisent "
            + "le même angle : répéter le symbole pour tourner davantage (`////` à 30° = 120°).",
        "`!` rend le trait plus fin (largeur × 0,7) et `'` passe à la couleur suivante (du brun au vert) ; "
            + "ils s'affichent quand « Épaisseur et couleur » est coché (la case se coche toute seule quand "
            + "la chaîne en contient).",
        "`{` ouvre un polygone rempli (une feuille) et `}` le ferme : entre les deux, chaque `f` ou `F` ajoute un "
            + "sommet au lieu de tracer un segment.",
        "Les autres symboles (`X`, `Y`, `A`…) ne dessinent rien : ils servent à piloter la réécriture.",
        "# Types de systèmes",
        "**Déterministe** : une règle par symbole.",
        "**Stochastique** : plusieurs règles pour un symbole, tirage selon le poids `S(poids)=…` (1 par défaut). "
            + "La graine affichée permet de retrouver une figure ; « Nouveau tirage » en tire une autre.",
        "**Contextuel** : `G<S>D=…` : la règle ne s'applique que si `S` a `G` à gauche et `D` à droite "
            + "(`G<` et `>D` sont facultatifs). Les branches `[…]` sont sautées pour trouver les voisins.",
        "**Paramétrique** : les symboles portent des nombres, `A(1,0.5)`. Règle : `S(paramètres):condition~poids="
            + "successeur`, par exemple `A(l,w) = !(w)F(l)[+(30)A(l*0.7,w*0.7)]`. La condition (`:l>0.1`) et le poids "
            + "(`~2`) sont facultatifs ; dans une condition, l'égalité s'écrit `==`. Opérations : `+ - * / % ^`, "
            + "comparaisons, `&&`, `||`, `!`, parenthèses, `sqrt`, `abs`, `min`, `max`, `floor`. Les règles se "
            + "séparent par `;` (elles peuvent alors contenir des espaces).",
        "Tortue paramétrique : `F(l)` avance de l × longueur, `+(a)` tourne de a degrés (de même pour `- & ^ \\ /`), "
            + "`!(w)` fixe la largeur, `'(i)` choisit la couleur i (0 à 7).",
        "# Affichage",
        "Vue 3D : glisser pour tourner, bouton droit (ou Maj) + glisser pour déplacer. Vue 2D : glisser pour "
            + "déplacer. Molette : zoom autour du curseur. Double-clic : vue de face, sans zoom.",
        "Vue « Volumes » : les branches deviennent des tubes éclairés (jusqu'à 50 000 segments ; au-delà, ou si la "
            + "machine ne sait pas afficher de 3D, la vue 3D en traits est utilisée). Menu Fichier : export OBJ "
            + "(Blender) ou STL (impression 3D, avec l'échelle en millimètres).",
        "La barre du bas résume la figure et signale en orange ce qui peut expliquer un dessin vide ou inattendu "
            + "(aucun `F`, symbole sans effet, règle jamais appliquée, crochet non refermé…).",
        "# Relier la chaîne et le dessin",
        "Survoler un segment du dessin surligne le `F` qui l'a tracé dans la chaîne ; survoler un `F` de la chaîne "
            + "surligne son segment (jusqu'à 100 000 segments, hors vue « Volumes »).",
        "« Colorer selon la règle » colore chaque morceau de la chaîne selon la règle qui l'a produit (légende dessous).",
        "Tortue animée : le bouton de lecture rejoue le tracé segment par segment, la pause le suspend, le retour "
            + "au début vide le dessin ; le curseur règle la vitesse (1 à 10 000 segments par seconde). Le symbole en "
            + "cours est surligné dans la chaîne.",
        "# Fichiers",
        "Menu Fichier : enregistrer ou ouvrir un système (`.lsys`), le copier sur une ligne (`lsys1:…`) pour le "
            + "partager et le coller, exporter une image PNG, un dessin SVG (la vue affichée), un maillage OBJ ou STL.");

    private FenetreAide() {
    }

    /**
     * Ouvre la fenêtre d'aide (non modale).
     * @param proprietaire la fenêtre principale
     */
    static void ouvre(Window proprietaire) {
        VBox contenu = new VBox(8);
        contenu.setPadding(new Insets(14));
        for (String ligne : TEXTE.split("\n")) {
            TextFlow paragraphe = new TextFlow();
            if (ligne.startsWith("# ")) {
                Text titre = new Text(ligne.substring(2));
                titre.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 15));
                paragraphe.getChildren().add(titre);
                paragraphe.setPadding(new Insets(6, 0, 0, 0));
            } else {
                paragraphe.getChildren().addAll(morceaux(ligne));
            }
            contenu.getChildren().add(paragraphe);
        }
        ScrollPane defilement = new ScrollPane(contenu);
        defilement.setFitToWidth(true);
        Stage fenetre = new Stage();
        fenetre.initOwner(proprietaire);
        fenetre.setTitle("Aide - Système de Lindenmayer");
        fenetre.setScene(new Scene(defilement, 640, 600));
        fenetre.show();
    }

    /** Découpe une ligne en morceaux de texte normal, gras ({@code **…**}) et code ({@code `…`}). */
    private static List<Text> morceaux(String ligne) {
        List<Text> textes = new ArrayList<>();
        double taille = Font.getDefault().getSize();
        int i = 0;
        while (i < ligne.length()) {
            boolean gras = ligne.startsWith("**", i), code = ligne.charAt(i) == '`';
            if (gras || code) {
                String marque = gras ? "**" : "`";
                int fin = ligne.indexOf(marque, i + marque.length());
                if (fin > 0) {
                    Text t = new Text(ligne.substring(i + marque.length(), fin));
                    t.setFont(gras ? Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, taille)
                        : Font.font("Monospaced", taille));
                    textes.add(t);
                    i = fin + marque.length();
                    continue;
                }
            }
            int suivant = i + 1;
            while (suivant < ligne.length() && !ligne.startsWith("**", suivant) && ligne.charAt(suivant) != '`')
                suivant++;
            textes.add(new Text(ligne.substring(i, suivant)));
            i = suivant;
        }
        return textes;
    }
}
