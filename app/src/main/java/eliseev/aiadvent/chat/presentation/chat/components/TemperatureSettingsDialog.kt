package eliseev.aiadvent.chat.presentation.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R
import kotlin.math.abs

@Composable
fun TemperatureSettingsDialog(
    currentTemperature: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    // Фиксированные значения температуры
    val fixedValues = listOf(0f, 0.7f, 1.2f)
    
    // Определяем текущую позицию на основе значения
    val currentPosition = when {
        currentTemperature <= 0.35f -> 0f
        currentTemperature <= 0.95f -> 0.7f
        else -> 1.2f
    }
    
    var sliderValue by remember(currentPosition) { 
        mutableStateOf(currentPosition) 
    }
    
    // Функция для привязки к ближайшему фиксированному значению
    fun snapToFixedValue(value: Float): Float {
        return fixedValues.minByOrNull { abs(it - value) } ?: value
    }
    
    // Определяем выбранное значение и его название
    val selectedTemperature = snapToFixedValue(sliderValue)
    val temperatureLabel = when (selectedTemperature) {
        0f -> stringResource(R.string.temperature_0)
        0.7f -> stringResource(R.string.temperature_07)
        1.2f -> stringResource(R.string.temperature_12)
        else -> "$selectedTemperature"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.temperature_settings_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.temperature_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        sliderValue = snapToFixedValue(newValue)
                    },
                    valueRange = 0f..1.2f,
                    steps = 1, // 2 шага между 3 значениями (0 -> 0.7 -> 1.2)
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = temperatureLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedTemperature)
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
