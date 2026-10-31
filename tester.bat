@echo off
rem Compilation et execution des tests du moteur de Lindenmayer, sans JavaFX (Windows).
setlocal
cd /d "%~dp0branches" || exit /b 1
if not exist build-moteur mkdir build-moteur
if not exist build-tests mkdir build-tests
call :liste_sources src\systeme\lindenmeyer build-moteur\sources.txt
javac -encoding UTF-8 -d build-moteur @build-moteur\sources.txt
if errorlevel 1 exit /b 1
javac -encoding UTF-8 -cp build-moteur -d build-tests test\TestsSystemes.java
if errorlevel 1 exit /b 1
java -cp build-moteur;build-tests TestsSystemes
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
