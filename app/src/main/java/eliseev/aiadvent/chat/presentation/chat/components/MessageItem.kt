package eliseev.aiadvent.chat.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.model.MovieInfo
import eliseev.aiadvent.chat.presentation.chat.model.UiMessage
import eliseev.aiadvent.chat.presentation.chat.model.UiMetrics
import java.util.Locale

@Composable
fun MessageItem(
    message: UiMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == MessageRole.USER
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isUser) 16.dp else 0.dp,
                vertical = 4.dp
            ),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.8f else 1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(12.dp)
        ) {
            DateField(
                date = message.formattedDate,
                isUser = isUser
            )
            
            // Индикатор summary для сообщений ассистента
            if (!isUser && message.isSummary) {
                SummaryIndicator(originalMessageCount = message.originalMessageCount)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (isUser) {
                UserMessage(content = message.content)
            } else {
                AssistantMessage(message = message)
                
                // Отображаем метрики для ответов ассистента
                message.metrics?.let { metrics ->
                    Spacer(modifier = Modifier.height(8.dp))
                    MetricsInfo(metrics = metrics)
                }
            }
        }
    }
}

@Composable
private fun DateField(
    date: String?,
    isUser: Boolean
) {
    date?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = if (isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun UserMessage(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
private fun AssistantMessage(message: UiMessage) {
    val bodyToShow = (message.body ?: message.content).trim()
    if (bodyToShow.isNotEmpty()) {
        Text(
            text = bodyToShow,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionSummary(summary: String?) {
    summary?.let {
        FieldSection(
            label = stringResource(R.string.question_label),
            content = {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ResponseBody(body: String) {
    FieldSection(
        label = stringResource(R.string.response_label),
        content = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun UrlsList(urls: List<String>?) {
    urls?.takeIf { it.isNotEmpty() }?.let {
        Spacer(modifier = Modifier.height(8.dp))
        FieldSection(
            label = stringResource(R.string.links_label),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    it.forEach { url ->
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsList(tags: List<String>?) {
    tags?.takeIf { it.isNotEmpty() }?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = stringResource(R.string.tags_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                it.forEach { tag ->
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieCard(movie: MovieInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.recommended_movie_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Название фильма
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.movie_title_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            // Год и жанр
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.movie_year_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = movie.year.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.movie_genre_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = movie.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Описание
            Text(
                text = stringResource(R.string.movie_description_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = movie.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Почему подходит
            movie.whyMatch?.let { whyMatch ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.movie_why_match_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = whyMatch,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun ClarificationQuestions(questions: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.clarification_questions_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                questions.forEachIndexed { index, question ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldSection(
    label: String?,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            content()
        }
    }
}

@Composable
private fun MetricsInfo(metrics: UiMetrics) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Метрики",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${metrics.providerName} • ${metrics.modelName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Время ответа
                MetricItem(
                    label = "Время",
                    value = formatTime(metrics.responseTimeMs),
                    modifier = Modifier.weight(1f)
                )
                
                // Токены
                MetricItem(
                    label = "Токены",
                    value = "${metrics.totalTokens}\n(${metrics.promptTokens} + ${metrics.completionTokens})",
                    modifier = Modifier.weight(1f)
                )
                
                // Стоимость
                MetricItem(
                    label = "Стоимость",
                    value = if (metrics.costUSD > 0) {
                        String.format(Locale.US, "$%.6f", metrics.costUSD)
                    } else {
                        "Бесплатно"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds}мс"
        milliseconds < 60000 -> String.format(Locale.US, "%.1fс", milliseconds / 1000.0)
        else -> {
            val minutes = milliseconds / 60000
            val seconds = (milliseconds % 60000) / 1000
            "${minutes}м ${seconds}с"
        }
    }
}

@Composable
private fun SummaryIndicator(originalMessageCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📦",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Резюме диалога ($originalMessageCount сообщений)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

