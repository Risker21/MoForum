#!/bin/bash

# ==================== 前端编译脚本 ====================
# 用法: chmod +x build-frontend.sh && ./build-frontend.sh

set -e

echo "========================================="
echo "  MoForum 前端编译脚本"
echo "========================================="
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/../moforum-web"
OUTPUT_DIR="$SCRIPT_DIR/docker/frontend"

# 检查 Node.js
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未检测到 Node.js"
    echo "   请先安装 Node.js 20+"
    exit 1
fi

echo "✅ Node.js 版本: $(node -v)"
echo "📁 前端目录: $FRONTEND_DIR"
echo "📁 输出目录: $OUTPUT_DIR"

# 检查前端目录
if [ ! -d "$FRONTEND_DIR" ]; then
    echo "❌ 错误: 前端目录不存在 $FRONTEND_DIR"
    exit 1
fi

cd "$FRONTEND_DIR"

# 安装依赖
echo ""
echo "📦 安装依赖..."
if [ ! -d "node_modules" ]; then
    npm ci || npm install
else
    echo "   跳过（node_modules 已存在）"
fi

# 编译前端
echo ""
echo "🔨 编译前端..."
npm run build

# 复制产物
echo ""
echo "📁 复制产物到 $OUTPUT_DIR ..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cp -r dist/* "$OUTPUT_DIR/"

echo ""
echo "========================================="
echo "✅ 前端编译完成！"
echo ""
echo "📊 产物位置: $OUTPUT_DIR"
echo "📁 文件数量: $(find "$OUTPUT_DIR" -type f | wc -l)"
echo "📦 总大小: $(du -sh "$OUTPUT_DIR" | cut -f1)"
echo ""
echo "🚀 现在可以执行: docker compose up -d"
echo "========================================="