#!/bin/bash

# Скрипт для сборки и запуска reminder-server в фоновом режиме

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

LOG_FILE="./reminder-server.log"
PID_FILE="./reminder-server.pid"

echo "🚀 Запуск сервера в фоновом режиме..."
echo "📝 Логи будут записываться в: $LOG_FILE"
echo "🆔 PID файл: $PID_FILE"
echo ""

java -jar build/libs/reminder-server-1.0.0.jar > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

echo $SERVER_PID > "$PID_FILE"

echo "✅ Сервер запущен!"
echo "   PID: $SERVER_PID"
echo "   Логи: tail -f $LOG_FILE"
echo "   Остановка: kill $SERVER_PID или ./stop.sh"
echo ""
