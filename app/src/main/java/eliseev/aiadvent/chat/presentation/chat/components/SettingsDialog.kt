package eliseev.aiadvent.chat.presentation.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSystemPromptClick: () -> Unit,
    onTemperatureClick: () -> Unit,
    onApiProviderClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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
