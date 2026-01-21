package eliseev.aiadvent.chat.presentation.simplechat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import eliseev.aiadvent.chat.R
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.presentation.chat.components.ApiProviderSettingsDialog
import eliseev.aiadvent.chat.presentation.chat.components.MessageItem
import eliseev.aiadvent.chat.presentation.chat.components.ModelInfoBar
import eliseev.aiadvent.chat.presentation.chat.components.SettingsDialog
import eliseev.aiadvent.chat.presentation.chat.components.SystemPromptEditDialog
import eliseev.aiadvent.chat.presentation.chat.components.TemperatureSettingsDialog
import eliseev.aiadvent.chat.presentation.chat.components.ThinkingIndicator
import eliseev.aiadvent.chat.presentation.chat.model.UiMessage
import eliseev.aiadvent.chat.presentation.simplechat.components.SimpleChatInput
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatScreen(
    onBackClick: () -> Unit,
    viewModel: SimpleChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showTemperatureDialog by remember { mutableStateOf(false) }
    var showApiProviderDialog by remember { mutableStateOf(false) }

    val visibleMessages = uiState.messages.filter { 
        it.role != MessageRole.SYSTEM 
    }

    AutoScrollToLatestMessage(
        messagesSize = visibleMessages.size,
        isLoading = uiState.isLoading,
        listState = listState
    )

    ShowErrorSnackbar(
        errorMessage = uiState.errorMessage,
        snackbarHostState = snackbarHostState,
        onDismiss = viewModel::dismissError
    )

    Scaffold(
        topBar = { 
            Column {
                SimpleChatTopBar(
                    onBackClick = onBackClick,
                    onSettingsClick = { showSettingsDialog = true }
                )
                ModelInfoBar(
                    currentProvider = viewModel.getApiProvider(),
                    currentModel = viewModel.getCurrentModel()
                )
            }
        },
        snackbarHost = { ChatSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ChatContent(
            paddingValues = paddingValues,
            visibleMessages = visibleMessages,
            isLoading = uiState.isLoading,
            inputText = uiState.inputText,
            listState = listState,
            onTextChange = viewModel::updateInputText,
            onSendClick = viewModel::sendMessage
        )
    }
    
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onSystemPromptClick = { showSystemPromptDialog = true },
            onTemperatureClick = { showTemperatureDialog = true },
            onApiProviderClick = { showApiProviderDialog = true }
        )
    }
    
    if (showSystemPromptDialog) {
        SystemPromptEditDialog(
            currentUserPrompt = viewModel.getUserPrompt(),
            modeName = stringResource(R.string.chat),
            onDismiss = { showSystemPromptDialog = false },
            onSave = { prompt ->
                viewModel.saveUserPrompt(prompt)
            }
        )
    }
    
    if (showTemperatureDialog) {
        TemperatureSettingsDialog(
            currentTemperature = viewModel.getTemperature(),
            onDismiss = { showTemperatureDialog = false },
            onSave = { temperature ->
                viewModel.saveTemperature(temperature)
            }
        )
    }
    
    if (showApiProviderDialog) {
        ApiProviderSettingsDialog(
            currentProvider = viewModel.getApiProvider(),
            currentOllamaModel = viewModel.getOllamaModel(),
            currentDeepSeekModel = viewModel.getDeepSeekModel(),
            onDismiss = { showApiProviderDialog = false },
            onSave = { provider, ollamaModel, deepSeekModel ->
                viewModel.saveApiSettings(provider, ollamaModel, deepSeekModel)
            }
        )
    }
}

@Composable
private fun AutoScrollToLatestMessage(
    messagesSize: Int,
    isLoading: Boolean,
    listState: LazyListState
) {
    LaunchedEffect(messagesSize, isLoading) {
        if (messagesSize > 0) {
            delay(100)
            val targetIndex = if (isLoading) {
                messagesSize
            } else {
                messagesSize - 1
            }
            listState.animateScrollToItem(targetIndex)
        }
    }
}

@Composable
private fun ShowErrorSnackbar(
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onDismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleChatTopBar(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.chat)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_button_description)
                )
            }
        }
    )
}

@Composable
private fun ChatSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ChatContent(
    paddingValues: PaddingValues,
    visibleMessages: List<UiMessage>,
    isLoading: Boolean,
    inputText: String,
    listState: LazyListState,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
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
            MessagesContent(
                visibleMessages = visibleMessages,
                isLoading = isLoading,
                listState = listState
            )
        }

        SimpleChatInput(
            text = inputText,
            onTextChange = onTextChange,
            onSendClick = onSendClick,
            enabled = !isLoading,
            modifier = Modifier
        )
    }
}

@Composable
private fun BoxScope.MessagesContent(
    visibleMessages: List<UiMessage>,
    isLoading: Boolean,
    listState: LazyListState
) {
    when {
        isLoading && visibleMessages.isEmpty() -> {
            InitialLoadingIndicator()
        }
        visibleMessages.isEmpty() -> {
            EmptyStateMessage()
        }
        else -> {
            MessagesList(
                visibleMessages = visibleMessages,
                isLoading = isLoading,
                listState = listState
            )
        }
    }
}

@Composable
private fun BoxScope.InitialLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun BoxScope.EmptyStateMessage() {
    Text(
        text = stringResource(R.string.empty_state_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun MessagesList(
    visibleMessages: List<UiMessage>,
    isLoading: Boolean,
    listState: LazyListState
) {
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
        
        if (isLoading) {
            item {
                ThinkingIndicator()
            }
        }
    }
}
