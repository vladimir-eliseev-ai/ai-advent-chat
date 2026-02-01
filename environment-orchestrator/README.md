# Оркестратор окружения (MCP)

MCP-сервер на хосте: по вызову инструментов выполняет `docker compose up -d` / `down` / `ps` в директории проекта.

## Инструменты

- **start_articles_environment** — поднять окружение: `docker compose up -d`
- **stop_articles_environment** — остановить: `docker compose down`
- **status_articles_environment** — статус: `docker compose ps -a`

## Запуск на хосте

Из корня проекта (чтобы `docker compose` видел `docker-compose.yml`):

```bash
COMPOSE_DIR=$(pwd) ./gradlew :environment-orchestrator:run
```

Или из любой директории:

```bash
COMPOSE_DIR=/path/to/AIAdventChat ./gradlew :environment-orchestrator:run
```

Порт по умолчанию: **8090**. Для эмулятора приложение обращается к `http://10.0.2.2:8090`.

## Как ИИ вызывает

1. **Экран «Краткая статья»** — кнопка «Поднять окружение (Docker)» вызывает `start_articles_environment`.
2. **Экран «MCP ИНСТРУМЕНТЫ»** — выберите «Оркестратор (Docker)», нажмите «Подключиться», затем вызывайте инструменты из списка (в т.ч. для чата/агента с tool calling).

Требуется: на хосте запущен оркестратор и установлен Docker.
