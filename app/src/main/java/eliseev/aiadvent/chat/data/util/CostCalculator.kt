package eliseev.aiadvent.chat.data.util

import eliseev.aiadvent.chat.data.model.ApiProvider

object CostCalculator {
    fun calculateCost(
        provider: ApiProvider,
        modelName: String,
        promptTokens: Int,
        completionTokens: Int
    ): Double {
        return when (provider) {
            ApiProvider.DEEPSEEK -> {
                // DeepSeek pricing (по состоянию на 2024)
                // Input: $0.14 / 1M tokens
                // Output: $0.28 / 1M tokens
                val inputCost = (promptTokens / 1_000_000.0) * 0.14
                val outputCost = (completionTokens / 1_000_000.0) * 0.28
                inputCost + outputCost
            }
            ApiProvider.OLLAMA -> {
                // Ollama бесплатный
                0.0
            }
        }
    }
}
