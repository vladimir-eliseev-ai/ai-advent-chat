package eliseev.aiadvent.chat.presentation.briefarticle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eliseev.aiadvent.chat.R
import org.koin.androidx.compose.koinViewModel
import eliseev.aiadvent.chat.presentation.briefarticle.StepStatus.FAILED
import eliseev.aiadvent.chat.presentation.briefarticle.StepStatus.OK
import eliseev.aiadvent.chat.presentation.briefarticle.StepStatus.PENDING
import eliseev.aiadvent.chat.presentation.briefarticle.StepStatus.RUNNING

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefArticleScreen(
    onBackClick: () -> Unit,
    onSavedArticlesClick: () -> Unit = {},
    viewModel: BriefArticleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.brief_article)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = uiState.urlInput,
                onValueChange = viewModel::updateUrl,
                label = { Text(stringResource(R.string.brief_article_url_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.runBriefSummary() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.brief_summary_button))
                }
                OutlinedButton(
                    onClick = onSavedArticlesClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.saved_articles))
                }
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(onClick = { viewModel.clearError() }, modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }

            StepRow(
                step = 1,
                label = uiState.step1.label.ifBlank { stringResource(R.string.step_fetch) },
                status = uiState.step1.status
            )
            StepRow(
                step = 2,
                label = uiState.step2.label.ifBlank { stringResource(R.string.step_summarize) },
                status = uiState.step2.status
            )
            StepRow(
                step = 3,
                label = uiState.step3.label.ifBlank { stringResource(R.string.step_save) },
                status = uiState.step3.status
            )

            uiState.summaryResult?.let { summary ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.step_done),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun StepRow(
    step: Int,
    label: String,
    status: StepStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$step. $label",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        when (status) {
            PENDING -> Text(
                text = stringResource(R.string.step_pending),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RUNNING -> CircularProgressIndicator(modifier = Modifier.height(20.dp).fillMaxWidth(0.2f))
            OK -> Text(
                text = stringResource(R.string.step_ok),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            FAILED -> Text(
                text = stringResource(R.string.step_failed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
