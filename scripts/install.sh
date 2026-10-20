#!/usr/bin/env bash
# Installs X-Tray from the GitHub releases on Debian, Ubuntu and Zorin.
# The .deb goes to a temporary file, is installed with apt, and is removed
# again afterwards, so nothing is left behind.
set -euo pipefail

REPO="andreasmlbngaol/x-tray"
PACKAGE="x-tray"

usage() {
    cat <<'EOF'
Install X-Tray from the GitHub releases.

Usage:
  install.sh [--version v1.1.1]

Without --version the latest release is used. The .deb is downloaded to a
temporary file, installed with apt, and removed afterwards.
EOF
}

version=""
while [ "$#" -gt 0 ]; do
    case "$1" in
        --version|-v)
            if [ "$#" -lt 2 ]; then
                echo "install.sh: --version needs a value" >&2
                exit 2
            fi
            version="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "install.sh: unknown argument '$1'" >&2
            usage >&2
            exit 2
            ;;
    esac
done

for tool in curl dpkg apt-get mktemp; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "install.sh: '$tool' is required" >&2
        exit 1
    fi
done

arch="$(dpkg --print-architecture)"
if [ "$arch" != "amd64" ]; then
    echo "install.sh: only amd64 packages are published (this machine is $arch)" >&2
    exit 1
fi

if [ -z "$version" ]; then
    # Follow the "latest" redirect instead of the API: no rate limit.
    latest="$(curl -fsSLI -o /dev/null -w '%{url_effective}' "https://github.com/$REPO/releases/latest")"
    version="${latest##*/}"
fi
case "$version" in
    v*) ;;
    *) version="v$version" ;;
esac

asset="${PACKAGE}_${version#v}_${arch}.deb"
url="https://github.com/$REPO/releases/download/$version/$asset"

tmp="$(mktemp --suffix=.deb)"
cleanup() { rm -f "$tmp"; }
trap cleanup EXIT

echo "X-Tray $version"
echo "Downloading $asset"
curl -fL --progress-bar -o "$tmp" "$url"

echo "Installing"
if [ "$(id -u)" -eq 0 ]; then
    apt-get install -y "$tmp"
else
    sudo apt-get install -y "$tmp"
fi

echo
echo "X-Tray $version installed. Restart it if it is already running."
