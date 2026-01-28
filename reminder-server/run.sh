#!/bin/bash

# Скрипт для сборки и запуска reminder-server

set -e

echo "🔨 Сборка reminder-server..."

cd "$(dirname "$0")"

./gradlew shadowJar --no-daemon

if [ $? -ne 0 ]; then
    echo "❌ Ошибка сборки!"
    exit 1
fi

echo "✅ Сборка завершена успешно"
echo ""
echo "🚀 Запуск сервера..."
echo ""

# Проверка переменных окружения
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "⚠️  ВНИМАНИЕ: DEEPSEEK_API_KEY не установлен!"
    echo "   Установите: export DEEPSEEK_API_KEY='ваш_ключ'"
    exit 1
fi

# Установка значений по умолчанию
export NEWS_API_MCP_URL=${NEWS_API_MCP_URL:-"http://localhost:8080/mcp"}
export INTERVAL_HOURS=${INTERVAL_HOURS:-6}
export PORT=${PORT:-8081}
export STORAGE_PATH=${STORAGE_PATH:-"./summaries.json"}

echo "📋 Конфигурация:"
echo "   DEEPSEEK_API_KEY: установлен"
echo "   NEWS_API_MCP_URL: $NEWS_API_MCP_URL"
echo "   INTERVAL_HOURS: $INTERVAL_HOURS"
echo "   PORT: $PORT"
echo "   STORAGE_PATH: $STORAGE_PATH"
echo ""

java -jar build/libs/reminder-server-1.0.0.jar
