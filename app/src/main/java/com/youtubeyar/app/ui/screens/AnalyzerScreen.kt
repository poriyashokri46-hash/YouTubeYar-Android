package com.youtubeyar.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.ui.Routes
import com.youtubeyar.app.ui.components.Fmt
import com.youtubeyar.app.ui.components.SectionCard
import com.youtubeyar.app.ui.components.StatChip
import com.youtubeyar.app.youtube.ChannelAnalysis
import com.youtubeyar.app.youtube.ChannelAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalyzerViewModel @Inject constructor(
    private val analyzer: ChannelAnalyzer,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings

    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val own: ChannelAnalysis? = null,
        val competitor: ChannelAnalysis? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun analyze(handle: String, isOwn: Boolean) {
        viewModelScope.launch {
            val key = settingsRepository.current().youtubeApiKey
            if (key.isBlank()) {
                _state.value = _state.value.copy(loading = false, error = "NO_API_KEY")
                return@launch
            }
            _state.value = _state.value.copy(loading = true, error = null)
            val result = analyzer.analyze(handle, key)
            result.fold(
                onSuccess = { analysis ->
                    _state.value = if (isOwn)
                        _state.value.copy(loading = false, own = analysis)
                    else
                        _state.value.copy(loading = false, competitor = analysis)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(loading = false, error = e.message ?: "ERR")
                }
            )
        }
    }
}

private fun isFaLocale(): Boolean = Locale.getDefault().language == "fa"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(navController: NavController, vm: AnalyzerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val settings by vm.settings.collectAsState(initial = null)
    var tab by remember { mutableIntStateOf(0) }
    var ownInput by remember { mutableStateOf("@${settings?.channelHandle.orEmpty()}") }
    var competitorInput by remember { mutableStateOf("") }
    val persian = isFaLocale()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.an_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.an_own_tab)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.an_competitor_tab)) })
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.an_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                val apiKeyMissing = settings?.youtubeApiKey.isNullOrBlank()
                if (apiKeyMissing) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.an_api_missing), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.an_api_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { navController.navigate(Routes.SETTINGS) }, shape = RoundedCornerShape(12.dp)) {
                                Text(stringResource(R.string.an_go_settings))
                            }
                        }
                    }
                }
            }

            item {
                val input = if (tab == 0) ownInput else competitorInput
                val setter = if (tab == 0) { v: String -> ownInput = v } else { v: String -> competitorInput = v }
                OutlinedTextField(
                    value = input,
                    onValueChange = setter,
                    label = { Text(stringResource(R.string.an_handle)) },
                    placeholder = { Text(stringResource(R.string.an_handle_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.analyze(input, tab == 0) },
                    enabled = !state.loading && input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null, Modifier.height(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.an_analyze))
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(14.dp)) {
                        Text(
                            when (err) {
                                "NO_API_KEY" -> stringResource(R.string.an_api_missing)
                                "CHANNEL_NOT_FOUND" -> stringResource(R.string.an_not_found)
                                else -> stringResource(R.string.an_error)
                            },
                            Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            val analysis = if (tab == 0) state.own else state.competitor
            if (analysis != null) {
                item { AnalysisStats(analysis, persian) }
                item { InsightsCard(analysis.insights) }
                item { RecentVideos(analysis) }
            }

            if (tab == 1 && state.own != null && state.competitor != null) {
                item { ComparisonTable(state.own!!, state.competitor!!, persian) }
            }
            if (tab == 1 && state.competitor != null && state.own == null) {
                item {
                    Text(
                        stringResource(R.string.an_you_first),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisStats(a: ChannelAnalysis, persian: Boolean) {
    SectionCard(title = a.title) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip(stringResource(R.string.an_subs), Fmt.num(a.subscribers, persian))
            StatChip(stringResource(R.string.an_total_views), Fmt.compact(a.totalViews, persian))
            StatChip(stringResource(R.string.an_videos), Fmt.num(a.videoCount, persian))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip(stringResource(R.string.an_avg_views), Fmt.compact(a.avgViews, persian))
            StatChip(stringResource(R.string.an_freq), if (persian) Fmt.fa(String.format(Locale.US, "%.1f", a.videosPerMonth)) else String.format(Locale.US, "%.1f", a.videosPerMonth))
            StatChip(stringResource(R.string.an_engagement), "%${if (persian) Fmt.fa(String.format(Locale.US, "%.1f", a.engagementRate)) else String.format(Locale.US, "%.1f", a.engagementRate)}")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip(stringResource(R.string.an_ratio), String.format(Locale.US, "%.2f", a.viewSubRatio).let { if (persian) Fmt.fa(it) else it })
            StatChip(stringResource(R.string.an_age), stringResource(R.string.an_age_days, if (persian) Fmt.fa(a.channelAgeDays) else a.channelAgeDays.toString()))
        }
        a.topVideo?.let { top ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.an_top_video), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                top.snippet.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InsightsCard(insights: List<String>) {
    SectionCard(title = stringResource(R.string.an_insights)) {
        insights.forEach { insight ->
            Row(Modifier.padding(vertical = 6.dp)) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .width(6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(Modifier.width(10.dp))
                Text(insight, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RecentVideos(a: ChannelAnalysis) {
    SectionCard(title = stringResource(R.string.an_recent)) {
        a.recentVideos.forEach { v ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(v.snippet.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    Fmt.compact(v.statistics.viewCount.toLongOrNull() ?: 0, isFaLocale()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ComparisonTable(mine: ChannelAnalysis, theirs: ChannelAnalysis, persian: Boolean) {
    SectionCard(title = stringResource(R.string.an_vs_you)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(stringResource(R.string.an_metric), Modifier.weight(1.4f), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.an_yours), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.an_them), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        ComparisonRow(stringResource(R.string.an_subs), Fmt.num(mine.subscribers, persian), Fmt.num(theirs.subscribers, persian))
        ComparisonRow(stringResource(R.string.an_avg_views), Fmt.compact(mine.avgViews, persian), Fmt.compact(theirs.avgViews, persian))
        ComparisonRow(stringResource(R.string.an_freq), "%.1f".format(Locale.US, mine.videosPerMonth), "%.1f".format(Locale.US, theirs.videosPerMonth))
        ComparisonRow(stringResource(R.string.an_engagement), "%.1f%%".format(Locale.US, mine.engagementRate), "%.1f%%".format(Locale.US, theirs.engagementRate))
    }
}

@Composable
private fun ComparisonRow(label: String, a: String, b: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium)
        Text(a, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(b, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}
