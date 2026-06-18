#!/bin/bash

# Définition des variables
APP_NAME="framework"
SRC_DIR="src/main/java"
WEB_DIR="src/main/webapp"
BUILD_DIR="build"
LIB_DIR="lib"
TOMCAT_WEBAPPS="/Users/mendrika/Documents/TOMCAT/webapps"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"
FRAMEWORK_JAR="framework-web.jar"

# Nettoyage et création du répertoire temporaire
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR
cp -r $WEB_DIR/* $BUILD_DIR

mkdir -p $BUILD_DIR/WEB-INF/classes
mkdir -p $BUILD_DIR/WEB-INF/lib

# Compilation des fichiers Java avec le JAR des Servlets
find $SRC_DIR -name "*.java" > sources.txt
chmod +r sources.txt
echo "source="
cat sources.txt
javac -cp "$SERVLET_API_JAR" -d $BUILD_DIR/WEB-INF/classes @sources.txt
rm sources.txt

cd $BUILD_DIR/WEB-INF/classes
jar -cvf $FRAMEWORK_JAR .

cd ../../../

mv  $BUILD_DIR/WEB-INF/classes/$FRAMEWORK_JAR  $BUILD_DIR/WEB-INF/lib

# Copier les fichiers web (web.xml, JSP, etc.)
cp -r $WEB_DIR/* $BUILD_DIR/

# Générer le fichier .war dans le dossier build
cd $BUILD_DIR || exit
mv web.xml WEB-INF/ #le namoronana anle xml
jar -cvf $APP_NAME.war *
cd ..

# Déploiement dans Tomcat
cp -f $BUILD_DIR/$APP_NAME.war $TOMCAT_WEBAPPS/

echo ""

echo "Déploiement terminé. Redémarrez Tomcat si nécessaire."

echo ""
