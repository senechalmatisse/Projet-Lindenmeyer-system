Le but de ce projet était de réaliser un interpéteur de L-système qui prend des règles de réécritures en entrée et produit une image 2D de l'objet obtenu par la simulation de ce système. Nous avons donc dû implémenter un parser de L-système, un moteur de réécriture, puis un moteur de rendu graphique pour visualiser ces plantes. Le modèle a ensuite était étendu aux L-systèmes stochastiques (les règles ont une probabilité d'être mise en application) et/ou contextuels (les règles s'appliquent en fonction des symboles avant ou après les éléments qu'elles modifient).

- compiler :
    Dans src/ :
        javac -d ../build/ --module-path ../lib/javafx-sdk-19.0.2.1/lib --add-modules javafx.controls,javafx.fxml systeme/*/*.java

- exécuter :
    Dans branches/ :
        java -cp build --module-path lib/javafx-sdk-19.0.2.1/lib --add-modules javafx.controls,javafx.fxml systeme.affichage.AffichageSysteme

- exécuter archive jar:
     Dans branches/ : 
        java --module-path lib/javafx-sdk-19.0.2.1/lib --add-modules javafx.controls,javafx.fxml -jar ../jar/executable.jar
