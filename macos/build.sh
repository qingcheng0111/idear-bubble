#!/usr/bin/env bash
# Mac 端构建：编译 Swift 外壳，组装 macos/build/idear泡泡.app（内含三端共用的网页核心）
set -euo pipefail
cd "$(dirname "$0")"

APP="build/idear泡泡.app"

swift build -c release
BIN="$(swift build -c release --show-bin-path)/IdearBubble"

rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BIN" "$APP/Contents/MacOS/IdearBubble"
cp Info.plist "$APP/Contents/Info.plist"
cp "../web/idear泡泡.html" "$APP/Contents/Resources/index.html"

# 本机运行用的临时签名（ad-hoc）；分发给别人需要换成自己的开发者证书
codesign --force --sign - "$APP"

echo "✓ Mac 端构建完成：macos/$APP"
