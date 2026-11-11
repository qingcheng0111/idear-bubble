#!/usr/bin/env bash
# Windows 端构建：把三端共用的网页嵌进 Go 程序，交叉编译 x64 和 ARM64 两个单文件 exe
set -euo pipefail
cd "$(dirname "$0")"

VERSION="1.0"

mkdir -p assets build
cp "../web/idear泡泡.html" assets/index.html

for arch in amd64 arm64; do
  name="x64"; [ "$arch" = arm64 ] && name="arm64"
  out="build/idear-bubble-${VERSION}-windows-${name}.exe"
  # -H windowsgui：作为窗口程序运行，不弹出命令行窗口
  GOOS=windows GOARCH="$arch" CGO_ENABLED=0 go build -trimpath -ldflags "-H windowsgui -s -w" -o "$out" .
  echo "✓ windows/$out（$(wc -c < "$out" | tr -d ' ') 字节）"
done
