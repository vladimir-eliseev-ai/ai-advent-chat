package eliseev.aiadvent.chat.domain.model

sealed class ChatResult<out T> {
    data class Success<T>(val data: T) : ChatResult<T>()
    data class Error(val message: String) : ChatResult<Nothing>()
    object Loading : ChatResult<Nothing>()
}

