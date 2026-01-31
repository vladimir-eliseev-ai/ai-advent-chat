# Article Summary MCP Server

MCP-сервер: загрузка статьи по ссылке → суммаризация (DeepSeek API) → сохранение в файл.

## Инструменты

- **fetch_article** — загрузить статью по URL, вернуть текст (без HTML).
- **summarize** — краткое резюме текста (DeepSeek или обрезка по длине).
- **save_to_file** — сохранить текст в файл в `OUTPUT_DIR`.
- **summarize_article** — цепочка: загрузка по URL → резюме.

## Переменные окружения

| Переменная | Описание |
|------------|----------|
| `DEEPSEEK_API_KEY` | API-ключ DeepSeek (обязателен для суммаризации через LLM). |
| `DEEPSEEK_MODEL` | Модель (по умолчанию `deepseek-chat`). |
| `PORT` | Порт HTTP-сервера (по умолчанию 8081). |
| `OUTPUT_DIR` | Папка для сохранения файлов (по умолчанию `./output`). |

Без `DEEPSEEK_API_KEY` суммаризация делается простой обрезкой текста по длине.

## Запуск

Из корня проекта:

```bash
export DEEPSEEK_API_KEY=sk-...
./gradlew :article-summary-mcp-server:run
```

Для Android-эмулятора в приложении укажите URL: `http://10.0.2.2:8081`.
