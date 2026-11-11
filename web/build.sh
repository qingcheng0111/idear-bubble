#!/usr/bin/env bash
# 网页端构建：检查内嵌脚本语法，输出可直接部署的 web/dist/index.html
set -euo pipefail
cd "$(dirname "$0")"

SRC="idear泡泡.html"
OUT="dist"

command -v node >/dev/null 2>&1 || { echo "需要 Node.js（用来检查脚本语法）" >&2; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

# 取出 <script> 与 </script> 之间的内容做语法检查
awk '/<script>/{f=1; next} /<\/script>/{f=0} f' "$SRC" > "$tmp/app.js"
[ -s "$tmp/app.js" ] || { echo "没有在 $SRC 里找到脚本" >&2; exit 1; }
node --check "$tmp/app.js"

mkdir -p "$OUT"
cp "$SRC" "$OUT/index.html"
echo "✓ 网页端构建完成：web/$OUT/index.html（$(wc -c < "$OUT/index.html" | tr -d ' ') 字节）"
