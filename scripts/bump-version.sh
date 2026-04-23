#!/bin/bash
# bump-version.sh — Incrementa el PATCH de version.properties en 1
# Uso: ./scripts/bump-version.sh
# Sale: nueva versión impresa a stdout y version.properties actualizado

set -e

PROP_FILE="version.properties"

if [ ! -f "$PROP_FILE" ]; then
    echo "ERROR: $PROP_FILE no encontrado" >&2
    exit 1
fi

# Leer versión actual
CURRENT_CODE=$(grep "^VERSION_CODE=" "$PROP_FILE" | cut -d'=' -f2)
CURRENT_NAME=$(grep "^VERSION_NAME=" "$PROP_FILE" | cut -d'=' -f2)

if [ -z "$CURRENT_CODE" ] || [ -z "$CURRENT_NAME" ]; then
    echo "ERROR: No se pudo leer VERSION_CODE o VERSION_NAME" >&2
    exit 1
fi

# Incrementar versionCode
NEW_CODE=$((CURRENT_CODE + 1))

# Incrementar PATCH (último número de X.Y.Z)
IFS='.' read -ra PARTS <<< "$CURRENT_NAME"
MAJOR=${PARTS[0]}
MINOR=${PARTS[1]}
PATCH=${PARTS[2]}
NEW_PATCH=$((PATCH + 1))
NEW_NAME="${MAJOR}.${MINOR}.${NEW_PATCH}"

# Escribir de vuelta
sed -i "s/^VERSION_CODE=.*/VERSION_CODE=${NEW_CODE}/" "$PROP_FILE"
sed -i "s/^VERSION_NAME=.*/VERSION_NAME=${NEW_NAME}/" "$PROP_FILE"

# Exportar para que el workflow las use
echo "VERSION_CODE=${NEW_CODE}"
echo "VERSION_NAME=${NEW_NAME}"
echo "TAG=v${NEW_NAME}"
