# ==========================================================================
#  Projet Lindenmayer : compilation, lancement, tests, archive jar, Javadoc
#
#  Usage    : make <cible> [VARIABLE=valeur ...]     (make seul : cette aide)
#  Exemples : make lancer
#             make tester
#             make lancer JAVAFX=/opt/javafx-sdk-21/lib
#
#  JavaFX 21 est cherche dans branches/lib/javafx-sdk-21*/lib ; s'il est
#  absent, son telechargement est propose (script installer-javafx.sh).
#  Prerequis : un JDK 17 ou plus recent (javac et java dans le PATH).
# ==========================================================================

SHELL := /bin/bash
.DEFAULT_GOAL := aide
.SUFFIXES:

# --- Variables modifiables (make <cible> VARIABLE=valeur) -----------------
# Dossier lib d'un SDK JavaFX 21 deja installe ailleurs (vide : branches/lib)
JAVAFX ?=
# Outils du JDK
JAVAC   ?= javac
JAVA    ?= java
JAR     ?= jar
JAVADOC ?= javadoc

# --- Constantes du projet -------------------------------------------------
JDK_MIN      := 17
MODULES      := javafx.controls
CLASSE       := systeme.affichage.AffichageSysteme
BRANCHES     := branches
SRC          := $(BRANCHES)/src
SOURCES      := $(wildcard $(SRC)/systeme/*/*.java)
MOTEUR       := $(wildcard $(SRC)/systeme/lindenmeyer/*.java)
TESTS        := $(BRANCHES)/test/TestsSystemes.java
EXEMPLES     := $(SRC)/systeme/affichage/exemples.txt
BUILD        := $(BRANCHES)/build
BUILD_MOTEUR := $(BRANCHES)/build-moteur
BUILD_TESTS  := $(BRANCHES)/build-tests
DOC          := $(BRANCHES)/doc
ARCHIVE      := jar/executable.jar
MANIFESTE    := jar/MANIFEST.MF

# Fichiers temoins : la compilation n'est refaite que si une source a change
TEMOIN_APP   := $(BUILD)/systeme/affichage/exemples.txt
TEMOIN_TESTS := $(BUILD_TESTS)/TestsSystemes.class

# Separateur du classpath (point-virgule sous Windows)
ifeq ($(OS),Windows_NT)
  SEP := ;
else
  SEP := :
endif

# Debut de recette : V_Vc_Rep_JavaFX recoit le dossier lib de JavaFX 21
# (installation proposee s'il est absent ; arret si l'utilisateur refuse)
JAVAFX_LIB = V_Vc_Rep_JavaFX="$$(bash installer-javafx.sh chemin "$(JAVAFX)")" || exit 1

.PHONY: aide help javafx verifier check verifier-jdk compiler build lancer run \
        tester test jar executer-jar run-jar doc javadoc nettoyer clean

# --- Cibles ---------------------------------------------------------------

aide help: ## Affiche cette aide
	@echo "Projet Lindenmayer - usage : make <cible> [JAVAFX=<dossier lib de JavaFX 21>]"
	@echo ""
	@awk 'BEGIN { FS = ":.*## " } /^[a-z][a-z -]*:.*## / { \
	  n = split($$1, t, " "); nom = t[1]; if (n > 1) nom = nom " (" t[2] ")"; \
	  printf "  %-24s %s\n", nom, $$2 }' $(firstword $(MAKEFILE_LIST))
	@echo ""
	@echo "JavaFX 21 est cherche dans branches/lib/javafx-sdk-21*/lib : s'il est absent,"
	@echo "son telechargement est propose au premier lancement (ou : make javafx)."

javafx: ## Installe JavaFX 21 dans branches/lib (depuis Maven Central)
	@bash installer-javafx.sh installer > /dev/null

verifier check: verifier-jdk ## Verifie la presence du JDK (17 ou plus) et de JavaFX 21
	@echo "JDK : $$($(JAVAC) -version 2>&1 | grep '^javac')"
	@V_Vc_Rep_JavaFX="$$(bash installer-javafx.sh verifier "$(JAVAFX)")" || exit 1; \
	echo "JavaFX 21 : $${V_Vc_Rep_JavaFX}"

verifier-jdk:
	@V_N_Version_Jdk="$$($(JAVAC) -version 2>&1 | sed -n 's/^javac \(1\.\)\{0,1\}\([0-9][0-9]*\).*/\2/p' | head -n 1)"; \
	if [ -z "$${V_N_Version_Jdk}" ]; then \
	  echo "javac introuvable : installer un JDK $(JDK_MIN) ou plus recent (par exemple https://adoptium.net)" >&2; \
	  exit 1; \
	fi; \
	if [ "$${V_N_Version_Jdk}" -lt "$(JDK_MIN)" ]; then \
	  echo "JDK $${V_N_Version_Jdk} detecte : JavaFX 21 demande un JDK $(JDK_MIN) ou plus recent" >&2; \
	  exit 1; \
	fi

compiler build: $(TEMOIN_APP) ## Compile l'application (dans branches/build)

$(TEMOIN_APP): $(SOURCES) $(EXEMPLES) | verifier-jdk
	@$(JAVAFX_LIB); \
	echo "Compilation de l'application (JavaFX : $${V_Vc_Rep_JavaFX})"; \
	mkdir -p "$(BUILD)" && \
	$(JAVAC) -encoding UTF-8 --release $(JDK_MIN) --module-path "$${V_Vc_Rep_JavaFX}" \
	  --add-modules $(MODULES) -d "$(BUILD)" $(SOURCES) && \
	cp "$(EXEMPLES)" "$@"

lancer run: $(TEMOIN_APP) ## Compile si besoin puis ouvre l'application
	@$(JAVAFX_LIB); \
	$(JAVA) --module-path "$${V_Vc_Rep_JavaFX}" --add-modules $(MODULES) -cp "$(BUILD)" $(CLASSE)

tester test: $(TEMOIN_TESTS) ## Compile et execute les tests du moteur (sans JavaFX)
	@cd "$(BRANCHES)" && $(JAVA) -cp "build-moteur$(SEP)build-tests" TestsSystemes

$(TEMOIN_TESTS): $(MOTEUR) $(TESTS)
	@echo "Compilation du moteur et des tests (sans JavaFX)"
	@mkdir -p "$(BUILD_MOTEUR)" "$(BUILD_TESTS)"
	@$(JAVAC) -encoding UTF-8 -d "$(BUILD_MOTEUR)" $(MOTEUR)
	@$(JAVAC) -encoding UTF-8 -cp "$(BUILD_MOTEUR)" -d "$(BUILD_TESTS)" $(TESTS)

jar: $(TEMOIN_APP) ## Reconstruit l'archive jar/executable.jar
	@$(JAR) --create --file "$(ARCHIVE)" --manifest "$(MANIFESTE)" -C "$(BUILD)" .
	@echo "Archive reconstruite : $(ARCHIVE)"

executer-jar run-jar: | verifier-jdk ## Lance l'archive jar/executable.jar
	@$(JAVAFX_LIB); \
	$(JAVA) --module-path "$${V_Vc_Rep_JavaFX}" --add-modules $(MODULES) -jar "$(ARCHIVE)"

doc javadoc: | verifier-jdk ## Genere la Javadoc dans branches/doc
	@$(JAVAFX_LIB); \
	$(JAVADOC) -quiet -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 \
	  --module-path "$${V_Vc_Rep_JavaFX}" --add-modules $(MODULES) \
	  -d "$(DOC)" -sourcepath "$(SRC)" -subpackages systeme && \
	echo "Javadoc generee : $(DOC)/index.html"

nettoyer clean: ## Supprime les fichiers compiles et la Javadoc (JavaFX est conserve)
	@rm -rf "$(BUILD)" "$(BUILD_MOTEUR)" "$(BUILD_TESTS)" "$(DOC)"
	@echo "Fichiers compiles et Javadoc supprimes"
