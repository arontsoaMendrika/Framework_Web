#!/bin/bash

# Définition des variables
SRC_DIR="src/main/java"
BUILD_DIR="build"
TOMCAT_HOME="/home/mandresy/Documents/tomcat9"
SERVLET_API_JAR="$TOMCAT_HOME/lib/servlet-api.jar"
FRAMEWORK_JAR="framework-web.jar"

# Nettoyage et création du répertoire temporaire
rm -rf "$BUILD_DIR" "$FRAMEWORK_JAR" sources.txt
mkdir -p "$BUILD_DIR/classes"

# Compilation des fichiers Java avec le JAR des Servlets
find "$SRC_DIR" -type f -name '*.java' ! -name '._*' > sources.txt
javac --release 8 -cp "$SERVLET_API_JAR" -d "$BUILD_DIR/classes" @sources.txt
rm -f sources.txt

jar -cf "$FRAMEWORK_JAR" -C "$BUILD_DIR/classes" .
cp -f "$FRAMEWORK_JAR" "../sprint7/lib/$FRAMEWORK_JAR"

echo ""

echo "Framework compilé : $FRAMEWORK_JAR"
echo "Copié dans ../sprint7/lib/"

echo ""
