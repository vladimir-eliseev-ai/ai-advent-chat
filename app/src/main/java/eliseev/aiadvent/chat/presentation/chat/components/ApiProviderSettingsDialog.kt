package eliseev.aiadvent.chat.presentation.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R
import eliseev.aiadvent.chat.data.model.ApiProvider

@Composable
fun ApiProviderSettingsDialog(
    currentProvider: ApiProvider,
    currentOllamaModel: String,
    currentDeepSeekModel: String,
    onDismiss: () -> Unit,
    onSave: (ApiProvider, String, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var ollamaModel by remember { mutableStateOf(currentOllamaModel) }
    var deepSeekModel by remember { mutableStateOf(currentDeepSeekModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.api_provider_settings))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Выбор провайдера
                Text(
                    text = stringResource(R.string.select_provider),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedProvider == ApiProvider.DEEPSEEK,
                                onClick = { selectedProvider = ApiProvider.DEEPSEEK },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProvider == ApiProvider.DEEPSEEK,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DeepSeek")
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedProvider == ApiProvider.OLLAMA,
                                onClick = { selectedProvider = ApiProvider.OLLAMA },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProvider == ApiProvider.OLLAMA,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ollama (локальный)")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Настройки DeepSeek
                if (selectedProvider == ApiProvider.DEEPSEEK) {
                    Text(
                        text = stringResource(R.string.deepseek_model),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = deepSeekModel,
                        onValueChange = { deepSeekModel = it },
                        label = { Text(stringResource(R.string.model_name)) },
                        placeholder = { Text("deepseek-chat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text(
                        text = stringResource(R.string.deepseek_models_examples),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Настройки Ollama
                if (selectedProvider == ApiProvider.OLLAMA) {
                    Text(
                        text = stringResource(R.string.ollama_model),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Переключатель между tinyllama и llama3.1:8b
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = ollamaModel == "tinyllama",
                                    onClick = { ollamaModel = "tinyllama" },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ollamaModel == "tinyllama",
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "tinyllama",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Маленькая модель (~637MB, быстрая)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = ollamaModel == "llama3.1:8b",
                                    onClick = { ollamaModel = "llama3.1:8b" },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ollamaModel == "llama3.1:8b",
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "llama3.1:8b",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Большая модель (~4.7GB, качественная)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = stringResource(R.string.ollama_install_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedProvider, ollamaModel, deepSeekModel)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
