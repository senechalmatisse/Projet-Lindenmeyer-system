#!/bin/bash
#
# $Author$
#
# $RCSfile$
#
# $Date$
#
# $Revision$
#
# FONCTION : Compilation et lancement de l'interface du systeme de Lindenmayer
#            (JavaFX 21 : si le SDK est absent de branches/lib, son installation est proposee)
#
# PARAMETRES : Ce programme prend en parametre
# (facultatif) 1: le repertoire lib d'un SDK JavaFX 21 deja installe
#                 (defaut : branches/lib/javafx-sdk-21*/lib)
#

# Repertoires du projet : le script peut etre lance depuis n'importe quel repertoire
V_Vc_Rep_Projet="$(cd "$(dirname "$0")" && pwd)"
V_Vc_Rep_Branches="${V_Vc_Rep_Projet}/branches"
V_Vc_Modules="javafx.controls"
V_N_Version_Min="17"

# JavaFX 21 demande un JDK 17 ou plus recent
V_N_Version_Jdk="$(javac -version 2>&1 | sed -n 's/^javac \(1\.\)\{0,1\}\([0-9][0-9]*\).*/\2/p' | head -n 1)"
if [ -z "${V_N_Version_Jdk}" ]
then
  echo "javac introuvable : installer un JDK ${V_N_Version_Min} ou plus recent (par exemple https://adoptium.net)" >&2
  exit 1
fi
if [ "${V_N_Version_Jdk}" -lt "${V_N_Version_Min}" ]
then
  echo "JDK ${V_N_Version_Jdk} detecte : JavaFX 21 demande un JDK ${V_N_Version_Min} ou plus recent" >&2
  exit 1
fi

# Dossier lib de JavaFX 21 (le script propose de l'installer s'il est absent)
V_Vc_Rep_JavaFX="$(bash "${V_Vc_Rep_Projet}/installer-javafx.sh" chemin "$1")"
V_N_Code_Retour=$?
if [ "${V_N_Code_Retour}" -ne "0" ]
then
  exit "${V_N_Code_Retour}"
fi

cd "${V_Vc_Rep_Branches}" || exit 1

# Compilation de toutes les classes
mkdir -p build
javac -encoding UTF-8 --module-path "${V_Vc_Rep_JavaFX}" --add-modules "${V_Vc_Modules}" \
  -d build src/systeme/*/*.java
V_N_Code_Retour=$?
if [ "${V_N_Code_Retour}" -ne "0" ]
then
  echo "Echec de la compilation (code ${V_N_Code_Retour})" >&2
  exit "${V_N_Code_Retour}"
fi

# Copie des exemples a cote des classes (javac ne copie pas les ressources)
cp src/systeme/affichage/exemples.txt build/systeme/affichage/

# Lancement de l'interface
java --module-path "${V_Vc_Rep_JavaFX}" --add-modules "${V_Vc_Modules}" \
  -cp build systeme.affichage.AffichageSysteme
V_N_Code_Retour=$?
exit "${V_N_Code_Retour}"
