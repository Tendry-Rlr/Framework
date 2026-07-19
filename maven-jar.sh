#!/bin/bash

# Définition des variables
APP_NAME="framework"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"

echo "=== 1. Nettoyage et compilation avec Maven ==="

# On lance la compilation et le packaging Maven
mvn clean package

# On vérifie si Maven a réussi à construire le projet
if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation Maven. Le déploiement est annulé."
    exit 1
fi

echo "=== 2. Déploiement du fichier JAR ==="

cp -f target/$APP_NAME.jar ../Framework-Build/App-Test/lib

echo "=== Déploiement terminé avec succès ! ==="


