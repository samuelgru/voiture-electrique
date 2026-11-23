#!/usr/bin/env bash
# Construit les images Docker de l'appli pour le NAS (Synology DS220+ = linux/amd64)
# et les exporte en archives .tar.gz dans ce dossier (deploy/).
#
# Usage : ./deploy/build-images.sh [version] [--avec-postgres]
#   version          : par défaut celle de backend/pom.xml (doit correspondre à deploy/docker-compose.yml)
#   --avec-postgres  : exporte aussi l'image postgres:19-alpine (si le NAS ne peut pas la télécharger)
#
# Prérequis : Docker avec buildx (Docker Desktop ou docker-buildx-plugin).
set -euo pipefail

RACINE="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY="$RACINE/deploy"
PLATEFORME="linux/amd64"
IMAGE_PG="postgres:19-alpine"

VERSION=""
AVEC_PG=0
for arg in "$@"; do
  case "$arg" in
    --avec-postgres) AVEC_PG=1 ;;
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    *) VERSION="$arg" ;;
  esac
done

if [[ -z "$VERSION" ]]; then
  # première balise <version> après <artifactId>zoe-backend</artifactId>
  VERSION="$(sed -n '/<artifactId>zoe-backend<\/artifactId>/,/<version>/s:.*<version>\(.*\)</version>.*:\1:p' "$RACINE/backend/pom.xml" | head -n1)"
fi
[[ -n "$VERSION" ]] || { echo "Version introuvable, la passer en argument" >&2; exit 1; }

if ! grep -q "zoe-backend:$VERSION" "$DEPLOY/docker-compose.yml"; then
  echo "Attention : deploy/docker-compose.yml ne référence pas zoe-backend:$VERSION, pensez à le mettre à jour." >&2
fi

construire() {
  local nom="$1" dossier="$2"
  echo "==> Construction de $nom:$VERSION ($PLATEFORME)"
  docker buildx build --platform "$PLATEFORME" -t "$nom:$VERSION" --load "$RACINE/$dossier"
  echo "==> Export de $DEPLOY/$nom-$VERSION.tar.gz"
  docker save "$nom:$VERSION" | gzip > "$DEPLOY/$nom-$VERSION.tar.gz"
}

construire zoe-backend backend
construire zoe-frontend frontend

if [[ "$AVEC_PG" == 1 ]]; then
  echo "==> Export de $IMAGE_PG"
  docker pull --platform "$PLATEFORME" "$IMAGE_PG"
  docker save "$IMAGE_PG" | gzip > "$DEPLOY/postgres-19-alpine.tar.gz"
fi

echo
echo "Archives prêtes :"
ls -lh "$DEPLOY"/*.tar.gz
echo
echo "À copier sur le NAS dans /volume1/docker/zoe avec docker-compose.yml, puis :"
echo "  sudo docker load -i zoe-backend-$VERSION.tar.gz"
echo "  sudo docker load -i zoe-frontend-$VERSION.tar.gz"
echo "  sudo docker compose up -d"
