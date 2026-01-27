#!/usr/bin/env python3
"""
Простой MCP тестовый сервер для проверки Android клиента.
Запуск: python test_mcp_server.py
Сервер будет доступен на: http://localhost:8000/mcp
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
import json
import asyncio
from typing import Any, Dict

app = FastAPI(title="MCP Test Server")

# Включаем CORS для Android приложения
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Хранилище для состояния
tools = [
    {
        "name": "add_numbers",
        "description": "Складывает два числа",
        "inputSchema": {
            "type": "object",
            "properties": {
                "a": {"type": "number", "description": "Первое число"},
                "b": {"type": "number", "description": "Второе число"}
            },
            "required": ["a", "b"]
        }
    },
    {
        "name": "greet",
        "description": "Приветствует пользователя",
        "inputSchema": {
            "type": "object",
            "properties": {
                "name": {"type": "string", "description": "Имя пользователя"}
            },
            "required": ["name"]
        }
    },
    {
        "name": "get_weather",
        "description": "Получает погоду для указанного города",
        "inputSchema": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "Название города"}
            },
            "required": ["city"]
        }
    }
]

@app.post("/mcp")
async def mcp_endpoint(request: Dict[str, Any]):
    """Обработка MCP запросов"""
    method = request.get("method")
    params = request.get("params", {})
    request_id = request.get("id")
    
    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {}
                },
                "serverInfo": {
                    "name": "test-mcp-server",
                    "version": "1.0.0"
                }
            }
        }
    
    elif method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "tools": tools
            }
        }
    
    elif method == "tools/call":
        tool_name = params.get("name")
        arguments = params.get("arguments", {})
        
        if tool_name == "add_numbers":
            result = arguments.get("a", 0) + arguments.get("b", 0)
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": str(result)
                        }
                    ]
                }
            }
        elif tool_name == "greet":
            name = arguments.get("name", "Гость")
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": f"Привет, {name}!"
                        }
                    ]
                }
            }
        elif tool_name == "get_weather":
            city = arguments.get("city", "Неизвестный город")
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": f"Погода в {city}: +20°C, солнечно"
                        }
                    ]
                }
            }
    
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "error": {
            "code": -32601,
            "message": "Method not found"
        }
    }

@app.get("/")
async def root():
    return {
        "message": "MCP Test Server",
        "endpoint": "/mcp",
        "tools_count": len(tools)
    }

if __name__ == "__main__":
    import uvicorn
    print("=" * 50)
    print("MCP Test Server запущен!")
    print("URL: http://localhost:8000/mcp")
    print("Для Android эмулятора: http://10.0.2.2:8000/mcp")
    print("Для реального устройства: http://<ваш_IP>:8000/mcp")
    print("=" * 50)
    uvicorn.run(app, host="0.0.0.0", port=8000)
