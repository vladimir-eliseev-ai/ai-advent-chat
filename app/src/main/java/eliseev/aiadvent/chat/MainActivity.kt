package eliseev.aiadvent.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eliseev.aiadvent.chat.presentation.home.HomeScreen
import eliseev.aiadvent.chat.presentation.logictasks.LogicTasksScreen
import eliseev.aiadvent.chat.presentation.mcp.McpToolsScreen
import eliseev.aiadvent.chat.presentation.simplechat.SimpleChatScreen

sealed class Screen {
    object Home : Screen()
    object Chat : Screen()
    object LogicTasks : Screen()
    object McpTools : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation()
                }
            }
        }
    }
}

@Composable
private fun Navigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    
    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                onChatClick = { currentScreen = Screen.Chat },
                onLogicTasksClick = { currentScreen = Screen.LogicTasks },
                onMcpToolsClick = { currentScreen = Screen.McpTools }
            )
        }
        is Screen.Chat -> {
            SimpleChatScreen(
                onBackClick = { currentScreen = Screen.Home }
            )
        }
        is Screen.LogicTasks -> {
            LogicTasksScreen(
                onBackClick = { currentScreen = Screen.Home }
            )
        }
        is Screen.McpTools -> {
            McpToolsScreen(
                onBackClick = { currentScreen = Screen.Home }
            )
        }
    }
}

