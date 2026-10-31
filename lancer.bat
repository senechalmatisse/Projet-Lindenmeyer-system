@echo off
rem Compilation et lancement de l interface du systeme de Lindenmayer (Windows).
rem JavaFX 21 est cherche dans branches\lib\javafx-sdk-21*\lib ; s il est absent,
rem son telechargement depuis Maven Central est propose (avec PowerShell).
rem Parametre facultatif : le dossier lib d un SDK JavaFX 21 deja installe.
setlocal
set "REP_BRANCHES=%~dp0branches"
set "VERSION_JAVAFX=21.0.8"
if not "%JAVAFX_VERSION%"=="" set "VERSION_JAVAFX=%JAVAFX_VERSION%"
set "DEPOT=https://repo1.maven.org/maven2"
if not "%JAVAFX_DEPOT%"=="" set "DEPOT=%JAVAFX_DEPOT%"
set "REP_JAVAFX="

rem JavaFX 21 demande un JDK 17 ou plus recent
set "VERSION_JDK="
for /f "tokens=2" %%V in ('javac -version 2^>^&1 ^| findstr /b /c:"javac "') do set "VERSION_JDK=%%V"
if "%VERSION_JDK%"=="" goto :sans_jdk
for /f "delims=." %%M in ("%VERSION_JDK%") do set "MAJEUR_JDK=%%M"
if %MAJEUR_JDK% LSS 17 goto :jdk_ancien

rem Dossier lib de JavaFX : celui donne en parametre, sinon celui de branches\lib
if "%~1"=="" goto :cherche
if exist "%~1\javafx.controls.jar" set "REP_JAVAFX=%~1"
if exist "%~1\javafx-controls-*.jar" set "REP_JAVAFX=%~1"
if not "%REP_JAVAFX%"=="" goto :compile
echo Le dossier %~1 ne contient pas les modules JavaFX.
exit /b 1

:cherche
for /d %%D in ("%REP_BRANCHES%\lib\javafx-sdk-21*") do (
  if exist "%%D\lib\javafx.controls.jar" set "REP_JAVAFX=%%D\lib"
  if exist "%%D\lib\javafx-controls-*.jar" set "REP_JAVAFX=%%D\lib"
)
if not "%REP_JAVAFX%"=="" goto :compile

rem JavaFX absent : proposition de l installer
echo JavaFX 21 est introuvable dans %REP_BRANCHES%\lib.
echo Il peut etre telecharge depuis Maven Central (JavaFX %VERSION_JAVAFX%, environ 10 Mo)
echo dans branches\lib\javafx-sdk-%VERSION_JAVAFX%\lib.
choice /c ON /n /m "Installer JavaFX %VERSION_JAVAFX% maintenant ? [O/N] "
if errorlevel 2 goto :refus
if not errorlevel 1 goto :refus
set "REP_SDK=%REP_BRANCHES%\lib\javafx-sdk-%VERSION_JAVAFX%\lib"
if not exist "%REP_SDK%" mkdir "%REP_SDK%"
for %%M in (base graphics controls) do call :telecharge %%M || goto :echec
echo JavaFX %VERSION_JAVAFX% installe dans %REP_SDK%
set "REP_JAVAFX=%REP_SDK%"

:compile
cd /d "%REP_BRANCHES%" || exit /b 1
if not exist build mkdir build
call :liste_sources src build\sources.txt
javac -encoding UTF-8 --module-path "%REP_JAVAFX%" --add-modules javafx.controls -d build @build\sources.txt
if errorlevel 1 exit /b 1
rem Copie des exemples a cote des classes (javac ne copie pas les ressources)
copy /Y src\systeme\affichage\exemples.txt build\systeme\affichage\ >nul
java --module-path "%REP_JAVAFX%" --add-modules javafx.controls -cp build systeme.affichage.AffichageSysteme
exit /b %errorlevel%

:sans_jdk
echo javac introuvable : installer un JDK 17 ou plus recent (par exemple https://adoptium.net).
exit /b 1

:jdk_ancien
echo JDK %VERSION_JDK% detecte : JavaFX 21 demande un JDK 17 ou plus recent.
exit /b 1

:echec
echo Echec du telechargement de JavaFX %VERSION_JAVAFX% (connexion, proxy ou version non publiee).
rmdir /s /q "%REP_BRANCHES%\lib\javafx-sdk-%VERSION_JAVAFX%" 2>nul
rmdir "%REP_BRANCHES%\lib" 2>nul
goto :manuel

:refus
echo Installation annulee.

:manuel
echo Installation manuelle : telecharger le SDK JavaFX 21 pour Windows
echo (https://gluonhq.com/products/javafx) et le decompresser dans branches\lib,
echo ou donner le chemin de son dossier lib : lancer.bat C:\javafx-sdk-21\lib
exit /b 1

rem Telecharge un module de JavaFX (1: base, graphics ou controls) et verifie sa somme SHA-1
:telecharge
set "NOM=javafx-%~1-%VERSION_JAVAFX%-win.jar"
set "ADRESSE=%DEPOT%/org/openjfx/javafx-%~1/%VERSION_JAVAFX%/%NOM%"
echo Telechargement de %NOM%
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference = 'Stop'; [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor 3072; $f = '%REP_SDK%\%NOM%'; $c = New-Object Net.WebClient; $c.DownloadFile('%ADRESSE%', $f); $a = $c.DownloadString('%ADRESSE%.sha1').Trim().Substring(0, 40).ToLower(); $o = (Get-FileHash -Algorithm SHA1 -LiteralPath $f).Hash.ToLower(); if ($a -ne $o) { Remove-Item -LiteralPath $f; Write-Host 'Somme SHA-1 incorrecte : fichier corrompu.'; exit 1 }"
exit /b %errorlevel%

rem Ecrit la liste des sources Java d un dossier (1) dans un fichier pour javac (2) :
rem chemins entre guillemets et avec des / (entre guillemets, javac lit \ comme un echappement)
:liste_sources
setlocal EnableDelayedExpansion
(for /r "%~1" %%F in (*.java) do (
  set "FICHIER=%%F"
  echo "!FICHIER:\=/!"
)) > "%~2"
endlocal
exit /b 0
