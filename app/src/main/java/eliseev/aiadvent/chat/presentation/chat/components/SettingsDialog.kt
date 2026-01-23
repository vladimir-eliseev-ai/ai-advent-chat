package eliseev.aiadvent.chat.presentation.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSystemPromptClick: () -> Unit,
    onTemperatureClick: () -> Unit,
    onApiProviderClick: () -> Unit,
    isHistoryCompressionEnabled: Boolean,
    onHistoryCompressionToggle: ((Boolean) -> Unit),
) {
    // Локальное состояние для мгновленного обновления UI
    var localCompressionState by remember(isHistoryCompressionEnabled) { 
        mutableStateOf(isHistoryCompressionEnabled) 
    }
    
    // Синхронизируем с внешним значением при изменении
    LaunchedEffect(isHistoryCompressionEnabled) {
        localCompressionState = isHistoryCompressionEnabled
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Переключатель компрессии истории (в начале списка для видимости)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_compression),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = localCompressionState,
                        onCheckedChange = { newValue ->
                            localCompressionState = newValue
                            onHistoryCompressionToggle?.invoke(newValue)
                        },
                        enabled = onHistoryCompressionToggle != null
                    )
                }
                
                TextButton(
                    onClick = {
                        onSystemPromptClick()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 0.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.system_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                TextButton(
                    onClick = {
                        onTemperatureClick()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 0.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.temperature),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                TextButton(
                    onClick = {
                        onApiProviderClick()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 0.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.api_provider),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
