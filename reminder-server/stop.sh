#!/bin/bash

# Скрипт для остановки reminder-server

PID_FILE="./reminder-server.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "⚠️  PID файл не найден. Сервер может быть не запущен."
    exit 1
fi

PID=$(cat "$PID_FILE")

if [ -z "$PID" ]; then
    echo "⚠️  PID файл пуст."
    rm -f "$PID_FILE"
    exit 1
fi

if ! kill -0 "$PID" 2>/dev/null; then
    echo "⚠️  Процесс с PID $PID не найден. Удаляю PID файл."
    rm -f "$PID_FILE"
    exit 1
fi

echo "🛑 Остановка сервера (PID: $PID)..."
kill "$PID"

# Ждем завершения
for i in {1..10}; do
    if ! kill -0 "$PID" 2>/dev/null; then
        echo "✅ Сервер остановлен"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Если не остановился, принудительно
if kill -0 "$PID" 2>/dev/null; then
    echo "⚠️  Принудительная остановка..."
    kill -9 "$PID"
    rm -f "$PID_FILE"
    echo "✅ Сервер остановлен принудительно"
fi
