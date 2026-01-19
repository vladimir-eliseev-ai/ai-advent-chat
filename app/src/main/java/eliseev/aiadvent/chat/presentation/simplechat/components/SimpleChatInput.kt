package eliseev.aiadvent.chat.presentation.simplechat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R

@Composable
fun SimpleChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    // Проверяем, был ли добавлен символ новой строки в самом конце
                    val wasNewlineAdded = !text.endsWith("\n") && newText.endsWith("\n")
                    val isSingleCharAdded = newText.length == text.length + 1
                    
                    if (wasNewlineAdded && isSingleCharAdded && enabled && text.isNotBlank()) {
                        // Enter нажат - отправляем сообщение, не добавляем новую строку
                        onSendClick()
                    } else {
                        // Любое другое изменение текста
                        onTextChange(newText)
                    }
                },
                modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = {
                Text(stringResource(R.string.input_placeholder))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (enabled && text.isNotBlank()) {
                        onSendClick()
                    }
                }
            ),
            singleLine = false,
            maxLines = 4
        )

        IconButton(
            onClick = onSendClick,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = stringResource(R.string.send_button_description),
                tint = if (enabled && text.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}
