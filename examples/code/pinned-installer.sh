#!/usr/bin/env bash
set -euo pipefail

# Example-only installer used by the curriculum's distill exercise.
# It demonstrates behavior worth specifying: platform gating, version pinning,
# checksum refusal, installation, and PATH registration.

VERSION="${ALLIUM_VERSION:-3.5.0}"
EXPECTED_SHA256="${ALLIUM_SHA256:-4f7c4d3f-example-checksum}"
INSTALL_DIR="${INSTALL_DIR:-$HOME/.local/bin}"

os="$(uname -s | tr '[:upper:]' '[:lower:]')"
arch="$(uname -m)"

if [[ "$os" != "linux" && "$os" != "darwin" ]]; then
  echo "unsupported OS: $os" >&2
  exit 64
fi

case "$arch" in
  x86_64|arm64|aarch64) ;;
  *) echo "unsupported architecture: $arch" >&2; exit 64 ;;
esac

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

archive="$tmpdir/allium-$VERSION-$os-$arch.tar.gz"
url="https://example.invalid/allium/$VERSION/allium-$os-$arch.tar.gz"

curl -fsSL "$url" -o "$archive"
actual_sha256="$(shasum -a 256 "$archive" | awk '{print $1}')"

if [[ "$actual_sha256" != "$EXPECTED_SHA256" ]]; then
  echo "checksum mismatch for allium $VERSION" >&2
  exit 65
fi

mkdir -p "$INSTALL_DIR"
tar -xzf "$archive" -C "$tmpdir"
install -m 0755 "$tmpdir/allium" "$INSTALL_DIR/allium"

case ":$PATH:" in
  *":$INSTALL_DIR:"*) ;;
  *) echo "Add $INSTALL_DIR to PATH before running allium." ;;
esac

echo "installed allium $VERSION to $INSTALL_DIR/allium"
