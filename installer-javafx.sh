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
# FONCTION : Recherche du SDK JavaFX 21 utilise par le projet et, s'il est absent,
#            proposition de le telecharger depuis Maven Central (modules javafx.base,
#            javafx.graphics et javafx.controls de la plateforme du JDK).
#            Seul le repertoire lib de JavaFX est ecrit sur la sortie standard :
#            les messages et la question sont ecrits sur la sortie d'erreur.
#
# PARAMETRES : Ce programme prend en parametre
# (facultatif) 1: l'action a realiser
#                 chemin    : (defaut) affiche le repertoire lib de JavaFX 21
#                             et propose de l'installer s'il est absent
#                 verifier  : affiche le repertoire lib de JavaFX 21, code retour 1 s'il est absent
#                 installer : installe JavaFX 21 dans branches/lib sans poser de question
# (facultatif) 2: le repertoire lib d'un SDK JavaFX deja installe ailleurs
#                 (prioritaire sur la recherche dans branches/lib)
#
# VARIABLES D'ENVIRONNEMENT (facultatives) :
#   JAVAFX_VERSION : version de JavaFX a telecharger (defaut : 21.0.8)
#   JAVAFX_DEPOT   : adresse du depot Maven (defaut : https://repo1.maven.org/maven2)
#
# CODE RETOUR : 0 si JavaFX est disponible, 1 sinon
#

V_Vc_Rep_Projet="$(cd "$(dirname "$0")" && pwd)"
V_Vc_Rep_Lib="${V_Vc_Rep_Projet}/branches/lib"
V_Vc_Action="${1:-chemin}"
V_Vc_Rep_Parametre="$2"
V_Vc_Version="${JAVAFX_VERSION:-21.0.8}"
V_Vc_Depot="${JAVAFX_DEPOT:-https://repo1.maven.org/maven2}"
V_Vc_Modules="base graphics controls"
V_Vc_Rep_JavaFX=""
V_Vc_Rep_Temp=""

#
# message : ecrit un message sur la sortie d'erreur (la sortie standard est reservee au chemin)
#   1: le message
#
message()
{
  local P_E_Vc_Texte="$1"
  echo "${P_E_Vc_Texte}" >&2
}

#
# instructions_manuelles : explique comment installer JavaFX 21 sans ce programme
#
instructions_manuelles()
{
  message "Installation manuelle : telecharger le SDK JavaFX 21 de votre plateforme"
  message "  (https://gluonhq.com/products/javafx) et le decompresser dans branches/lib,"
  message "  ou indiquer le dossier lib d'un SDK deja installe :"
  message "    make lancer JAVAFX=/chemin/javafx-sdk-21/lib"
  message "    ./lancer.sh /chemin/javafx-sdk-21/lib"
}

#
# est_javafx : code retour 0 si le repertoire contient les modules base, graphics et controls
#              (SDK Gluon : javafx.controls.jar ; Maven : javafx-controls-<version>-<plateforme>.jar)
#   1: le repertoire a examiner
#
est_javafx()
{
  local P_E_Vc_Rep="$1"
  local V_Vc_Module=""
  local V_Vc_Fichier=""
  local V_N_Trouve=0
  if [ -z "${P_E_Vc_Rep}" ] || [ ! -d "${P_E_Vc_Rep}" ]
  then
    return 1
  fi
  for V_Vc_Module in ${V_Vc_Modules}
  do
    V_N_Trouve=0
    for V_Vc_Fichier in "${P_E_Vc_Rep}/javafx.${V_Vc_Module}.jar" "${P_E_Vc_Rep}/javafx-${V_Vc_Module}-"*.jar
    do
      if [ -f "${V_Vc_Fichier}" ]
      then
        V_N_Trouve=1
      fi
    done
    if [ "${V_N_Trouve}" -eq "0" ]
    then
      return 1
    fi
  done
  return 0
}

#
# cherche_javafx : ecrit le repertoire lib du SDK JavaFX 21 present dans branches/lib (rien si aucun)
#
cherche_javafx()
{
  local V_Vc_Rep=""
  local V_Vc_Resultat=""
  for V_Vc_Rep in "${V_Vc_Rep_Lib}/javafx-sdk-21"*/lib
  do
    if est_javafx "${V_Vc_Rep}"
    then
      V_Vc_Resultat="${V_Vc_Rep}"
    fi
  done
  echo "${V_Vc_Resultat}"
}

#
# chemin_natif : ecrit le chemin sous la forme attendue par java
#                (C:/... sous Git Bash, MSYS2 ou Cygwin ; inchange ailleurs)
#   1: le chemin
#
chemin_natif()
{
  local P_E_Vc_Chemin="$1"
  if command -v cygpath > /dev/null 2>&1
  then
    cygpath -m "${P_E_Vc_Chemin}"
  else
    echo "${P_E_Vc_Chemin}"
  fi
}

#
# plateforme : ecrit le classifieur Maven de la plateforme du JDK (linux, mac, mac-aarch64, win...)
#              Le systeme et l'architecture sont lus aupres de java (un JDK x64 sur un Mac M1
#              a besoin des bibliotheques x64), sinon aupres de uname. Code retour 1 si inconnue.
#
plateforme()
{
  local V_Vc_Proprietes=""
  local V_Vc_Systeme=""
  local V_Vc_Architecture=""
  local V_Vc_Suffixe=""
  if command -v java > /dev/null 2>&1
  then
    V_Vc_Proprietes="$(java -XshowSettings:properties -version 2>&1 | tr -d '\r')"
    V_Vc_Systeme="$(echo "${V_Vc_Proprietes}" | sed -n 's/^ *os\.name = //p' | head -n 1)"
    V_Vc_Architecture="$(echo "${V_Vc_Proprietes}" | sed -n 's/^ *os\.arch = //p' | head -n 1)"
  fi
  if [ -z "${V_Vc_Systeme}" ]
  then
    V_Vc_Systeme="$(uname -s)"
  fi
  if [ -z "${V_Vc_Architecture}" ]
  then
    V_Vc_Architecture="$(uname -m)"
  fi
  case "${V_Vc_Architecture}" in
    x86_64|amd64)
      V_Vc_Suffixe=""
      ;;
    aarch64|arm64)
      V_Vc_Suffixe="-aarch64"
      ;;
    *)
      return 1
      ;;
  esac
  case "${V_Vc_Systeme}" in
    Linux*)
      echo "linux${V_Vc_Suffixe}"
      ;;
    Mac*|Darwin*)
      echo "mac${V_Vc_Suffixe}"
      ;;
    Windows*|MINGW*|MSYS*|CYGWIN*)
      echo "win${V_Vc_Suffixe}"
      ;;
    *)
      return 1
      ;;
  esac
  return 0
}

#
# telecharge : telecharge une adresse dans un fichier (avec curl, sinon wget)
#   1: l'adresse
#   2: le fichier a ecrire
#   3: 1 pour afficher la progression, 0 sinon
#
telecharge()
{
  local P_E_Vc_Adresse="$1"
  local P_E_Vc_Fichier="$2"
  local P_E_N_Progression="$3"
  if command -v curl > /dev/null 2>&1
  then
    if [ "${P_E_N_Progression}" -eq "1" ]
    then
      curl -fL --retry 2 --connect-timeout 20 --progress-bar -o "${P_E_Vc_Fichier}" "${P_E_Vc_Adresse}"
    else
      curl -fsSL --retry 2 --connect-timeout 20 -o "${P_E_Vc_Fichier}" "${P_E_Vc_Adresse}"
    fi
  elif command -v wget > /dev/null 2>&1
  then
    if [ "${P_E_N_Progression}" -eq "1" ]
    then
      wget -O "${P_E_Vc_Fichier}" "${P_E_Vc_Adresse}" >&2
    else
      wget -q -O "${P_E_Vc_Fichier}" "${P_E_Vc_Adresse}" >&2
    fi
  else
    message "Ni curl ni wget n'est installe : telechargement impossible."
    return 1
  fi
}

#
# somme_sha1 : ecrit la somme SHA-1 d'un fichier (avec sha1sum, sinon shasum), rien si aucun outil
#   1: le fichier
#
somme_sha1()
{
  local P_E_Vc_Fichier="$1"
  if command -v sha1sum > /dev/null 2>&1
  then
    sha1sum "${P_E_Vc_Fichier}" | cut -d " " -f 1
  elif command -v shasum > /dev/null 2>&1
  then
    shasum -a 1 "${P_E_Vc_Fichier}" | cut -d " " -f 1
  fi
}

#
# abandonne : supprime le telechargement en cours (et branches/lib s'il est reste vide)
#
abandonne()
{
  if [ -n "${V_Vc_Rep_Temp}" ]
  then
    rm -rf "${V_Vc_Rep_Temp}"
    V_Vc_Rep_Temp=""
  fi
  rmdir "${V_Vc_Rep_Lib}" 2> /dev/null
}

#
# installe_javafx : telecharge et verifie les modules de JavaFX, puis les place dans
#                   branches/lib/javafx-sdk-<version>/lib (renseigne V_Vc_Rep_JavaFX)
#
installe_javafx()
{
  local V_Vc_Plateforme=""
  local V_Vc_Module=""
  local V_Vc_Nom=""
  local V_Vc_Adresse=""
  local V_Vc_Somme_Attendue=""
  local V_Vc_Somme_Obtenue=""
  local V_Vc_Rep_Cible="${V_Vc_Rep_Lib}/javafx-sdk-${V_Vc_Version}/lib"

  V_Vc_Plateforme="$(plateforme)"
  if [ -z "${V_Vc_Plateforme}" ]
  then
    message "Plateforme non reconnue : JavaFX ne peut pas etre telecharge automatiquement."
    instructions_manuelles
    return 1
  fi

  # Telechargement dans un repertoire temporaire : rien n'est installe si une etape echoue
  V_Vc_Rep_Temp="${V_Vc_Rep_Lib}/.telechargement-javafx-$$"
  mkdir -p "${V_Vc_Rep_Temp}" || return 1
  for V_Vc_Module in ${V_Vc_Modules}
  do
    V_Vc_Nom="javafx-${V_Vc_Module}-${V_Vc_Version}-${V_Vc_Plateforme}.jar"
    V_Vc_Adresse="${V_Vc_Depot}/org/openjfx/javafx-${V_Vc_Module}/${V_Vc_Version}/${V_Vc_Nom}"
    message "Telechargement de ${V_Vc_Nom}"
    if ! telecharge "${V_Vc_Adresse}" "${V_Vc_Rep_Temp}/${V_Vc_Nom}" "1" \
      || ! telecharge "${V_Vc_Adresse}.sha1" "${V_Vc_Rep_Temp}/${V_Vc_Nom}.sha1" "0"
    then
      message "Echec du telechargement de ${V_Vc_Adresse}"
      message "(connexion a Internet, proxy, ou version ${V_Vc_Version} non publiee pour ${V_Vc_Plateforme})."
      abandonne
      instructions_manuelles
      return 1
    fi

    # Verification de l'integrite avec la somme SHA-1 publiee a cote de l'archive
    V_Vc_Somme_Attendue="$(cut -c 1-40 "${V_Vc_Rep_Temp}/${V_Vc_Nom}.sha1" | head -n 1 | tr 'A-F' 'a-f')"
    V_Vc_Somme_Obtenue="$(somme_sha1 "${V_Vc_Rep_Temp}/${V_Vc_Nom}" | tr 'A-F' 'a-f')"
    if [ -z "${V_Vc_Somme_Obtenue}" ]
    then
      message "Attention : ni sha1sum ni shasum n'est installe, la somme de controle n'est pas verifiee."
    elif [ "${V_Vc_Somme_Obtenue}" != "${V_Vc_Somme_Attendue}" ]
    then
      message "Somme SHA-1 incorrecte pour ${V_Vc_Nom} : fichier corrompu, installation annulee."
      abandonne
      return 1
    fi
    rm -f "${V_Vc_Rep_Temp}/${V_Vc_Nom}.sha1"
  done

  # Mise en place du SDK (remplace une installation incomplete de la meme version)
  rm -rf "${V_Vc_Rep_Cible}"
  mkdir -p "$(dirname "${V_Vc_Rep_Cible}")" || return 1
  if ! mv "${V_Vc_Rep_Temp}" "${V_Vc_Rep_Cible}"
  then
    abandonne
    return 1
  fi
  V_Vc_Rep_Temp=""
  message "JavaFX ${V_Vc_Version} (${V_Vc_Plateforme}) installe dans ${V_Vc_Rep_Cible}"
  V_Vc_Rep_JavaFX="${V_Vc_Rep_Cible}"
  return 0
}

#
# propose_installation : demande a l'utilisateur s'il veut installer JavaFX 21 ; code retour 0 si oui
#
propose_installation()
{
  local V_Vc_Reponse=""
  message "JavaFX 21 est introuvable dans branches/lib (javafx-sdk-21*/lib)."
  if [ ! -t 0 ]
  then
    message "Session non interactive : lancer 'make javafx' ou './installer-javafx.sh installer'."
    instructions_manuelles
    return 1
  fi
  message "Il peut etre telecharge depuis Maven Central (JavaFX ${V_Vc_Version}, environ 10 Mo)"
  message "dans branches/lib/javafx-sdk-${V_Vc_Version}/lib."
  printf "Installer JavaFX %s maintenant ? [o/N] " "${V_Vc_Version}" >&2
  read -r V_Vc_Reponse
  case "${V_Vc_Reponse}" in
    o|O|oui|Oui|OUI|y|Y|yes|Yes|YES)
      return 0
      ;;
  esac
  message "Installation annulee."
  instructions_manuelles
  return 1
}

# Interruption (Ctrl+C) : suppression du telechargement en cours
trap 'abandonne; exit 1' INT TERM

case "${V_Vc_Action}" in
  chemin|verifier|installer)
    ;;
  *)
    message "Action inconnue : ${V_Vc_Action} (attendu : chemin, verifier ou installer)"
    exit 1
    ;;
esac

# 1. Repertoire donne en parametre : il doit contenir les modules JavaFX
if [ -n "${V_Vc_Rep_Parametre}" ] && [ "${V_Vc_Action}" != "installer" ]
then
  if ! est_javafx "${V_Vc_Rep_Parametre}"
  then
    message "${V_Vc_Rep_Parametre} ne contient pas les modules javafx.base, javafx.graphics et javafx.controls."
    exit 1
  fi
  chemin_natif "$(cd "${V_Vc_Rep_Parametre}" && pwd)"
  exit 0
fi

# 2. SDK deja present dans branches/lib
V_Vc_Rep_JavaFX="$(cherche_javafx)"
if [ -n "${V_Vc_Rep_JavaFX}" ]
then
  if [ "${V_Vc_Action}" = "installer" ]
  then
    message "JavaFX 21 est deja installe : ${V_Vc_Rep_JavaFX}"
  fi
  chemin_natif "${V_Vc_Rep_JavaFX}"
  exit 0
fi

# 3. SDK absent : installation, apres accord de l'utilisateur pour l'action chemin
if [ "${V_Vc_Action}" = "verifier" ]
then
  message "JavaFX 21 est introuvable dans branches/lib : lancer 'make javafx' pour l'installer."
  exit 1
fi
if [ "${V_Vc_Action}" = "chemin" ]
then
  propose_installation || exit 1
fi
installe_javafx || exit 1
chemin_natif "${V_Vc_Rep_JavaFX}"
exit 0
