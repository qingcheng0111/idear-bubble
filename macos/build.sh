#!/usr/bin/env bash
# Mac 端构建：编译 Swift 外壳（Apple 芯片 + Intel 通用版），
# 组装 macos/build/idear泡泡.app（内含三端共用的网页核心），并打包成 .dmg 安装镜像
set -euo pipefail
cd "$(dirname "$0")"

APP="build/idear泡泡.app"
VERSION="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' Info.plist)"
DMG="build/idear-bubble-${VERSION}-macos.dmg"
ARCHS=(--arch arm64 --arch x86_64)

swift build -c release "${ARCHS[@]}"
BIN="$(swift build -c release "${ARCHS[@]}" --show-bin-path)/IdearBubble"

rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BIN" "$APP/Contents/MacOS/IdearBubble"
cp Info.plist "$APP/Contents/Info.plist"
cp "../web/idear泡泡.html" "$APP/Contents/Resources/index.html"

# 本机运行用的临时签名（ad-hoc）；要让别的电脑免确认打开，需要换成 Apple 开发者证书签名并公证
codesign --force --sign - "$APP"
echo "✓ 应用：macos/${APP}（$(lipo -archs "${APP}/Contents/MacOS/IdearBubble")）"

# 安装镜像：应用 + 「应用程序」文件夹的快捷方式，打开后拖进去即可安装
stage="$(mktemp -d)"
trap 'rm -rf "$stage"' EXIT
ditto "$APP" "$stage/idear泡泡.app"
ln -s /Applications "$stage/Applications"
rm -f "$DMG"
hdiutil create -quiet -volname "idear 泡泡" -srcfolder "$stage" -format UDZO -ov "$DMG"
echo "✓ 安装镜像：macos/$DMG"
