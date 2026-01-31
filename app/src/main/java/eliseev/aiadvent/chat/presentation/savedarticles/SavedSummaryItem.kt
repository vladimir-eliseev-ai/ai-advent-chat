package eliseev.aiadvent.chat.presentation.savedarticles

data class SavedSummaryItem(
    val id: String,
    val originalUrl: String,
    val contentPreview: String,
    val content: String,
    val savedAt: Long
)
