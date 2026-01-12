package eliseev.aiadvent.chat.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.presentation.chat.components.ChatInput
import eliseev.aiadvent.chat.presentation.chat.components.MessageItem
import eliseev.aiadvent.chat.presentation.chat.components.ThinkingIndicator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val visibleMessages = uiState.messages.filter { 
        it.role != MessageRole.SYSTEM 
    }
    
    // Прокрутка к последнему сообщению при добавлении новых или при загрузке
    LaunchedEffect(visibleMessages.size, uiState.isLoading) {
        if (visibleMessages.isNotEmpty()) {
            delay(100)
            val targetIndex = if (uiState.isLoading) {
                // Если идет загрузка, прокручиваем к индикатору "Сильно думаю ..."
                visibleMessages.size
            } else {
                visibleMessages.size - 1
            }
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Показ ошибок через Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Advent Chat") }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading && visibleMessages.isEmpty()) {
                    // Показываем лоадер по центру при первой загрузке
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (visibleMessages.isEmpty()) {
                    Text(
                        text = "Начните разговор с AI",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = false
                    ) {
                        items(
                            items = visibleMessages,
                            key = { it.timestamp }
                        ) { message ->
                            MessageItem(message = message)
                        }
                        
                        // Показываем индикатор "Сильно думаю ..." если идет загрузка
                        if (uiState.isLoading) {
                            item {
                                ThinkingIndicator()
                            }
                        }
                    }
                }
            }

            ChatInput(
                text = uiState.inputText,
                onTextChange = viewModel::updateInputText,
                onSendClick = viewModel::sendMessage,
                enabled = !uiState.isLoading
            )
        }
    }
}

