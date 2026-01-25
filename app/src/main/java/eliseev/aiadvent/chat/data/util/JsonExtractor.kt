package eliseev.aiadvent.chat.data.util

object JsonExtractor {
    fun extractJsonObject(text: String): String? {
        val trimmed = text.trim()

        // Частый случай: модель заворачивает JSON в fenced code block
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
        val fenced = fenceRegex.find(trimmed)?.groupValues?.getOrNull(1)?.trim()
        val candidate = if (!fenced.isNullOrBlank()) fenced else trimmed

        val start = candidate.indexOf('{')
        val end = candidate.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return candidate.substring(start, end + 1).trim()
    }
}
