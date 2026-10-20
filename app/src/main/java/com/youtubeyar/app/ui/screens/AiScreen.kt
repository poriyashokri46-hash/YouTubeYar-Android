package com.youtubeyar.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.ads.TapsellAdsManager
import com.youtubeyar.app.ads.TapsellBanner
import com.youtubeyar.app.data.AiHistory
import com.youtubeyar.app.ai.AiResult
import com.youtubeyar.app.ai.AiSection
import com.youtubeyar.app.ai.AiTool
import com.youtubeyar.app.ai.AiTone
import com.youtubeyar.app.ai.OfflineAiEngine
import com.youtubeyar.app.ai.RemoteAiProvider
import com.youtubeyar.app.data.AppDatabase
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.ui.Routes
import com.youtubeyar.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val remote: RemoteAiProvider,
    private val db: AppDatabase,
    val adsManager: TapsellAdsManager
) : ViewModel() {

    val settings = settingsRepository.settings
    val history = db.aiHistoryDao().observeRecent()

    data class UiState(
        val loading: Boolean = false,
        val result: AiResult? = null,
        val note: String? = null,
        val limitReached: Boolean = false,
        val remaining: Int = SettingsRepository.FREE_DAILY_LIMIT
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()
    private val _generationCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val generationCompleted = _generationCompleted.asSharedFlow()

    init {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(remaining = settingsRepository.remainingAiUses())
        }
    }

    fun generate(tool: AiTool, topic: String, tone: AiTone, premium: Boolean) {
        if (topic.isBlank() || _ui.value.loading) return
        viewModelScope.launch {
            if (!premium) {
                val allowed = settingsRepository.consumeAiUse()
                if (!allowed) {
                    _ui.value = _ui.value.copy(limitReached = true, loading = false)
                    return@launch
                }
            }
            val s = settingsRepository.current()
            _ui.value = _ui.value.copy(loading = true, note = null, limitReached = false)
            val niche = s.channelNiche.ifBlank { topic }
            val isEnglish = Locale.getDefault().language != "fa"
            var result: AiResult? = null

            if (s.aiProvider != "offline" && s.aiApiKey.isNotBlank()) {
                val prompt = remote.buildPrompt(tool, topic, niche, tone)
                remote.generate(s.aiProvider, s.aiApiKey, prompt).fold(
                    onSuccess = { text ->
                        result = AiResult(tool, topic, listOf(AiSection("نتایج", text)), s.aiProvider)
                    },
                    onFailure = { _ui.value = _ui.value.copy(note = "REMOTE_FAILED") }
                )
            }
            if (result == null) {
                result = OfflineAiEngine.generate(tool, topic, niche, tone, isEnglish)
            }

            db.aiHistoryDao().insert(
                AiHistory(
                    toolType = tool.id,
                    topic = topic,
                    result = result!!.sections.joinToString("\n\n") { "${it.title}:\n${it.content}" }
                )
            )
            _ui.value = _ui.value.copy(
                loading = false,
                result = result,
                remaining = settingsRepository.remainingAiUses()
            )
            _generationCompleted.tryEmit(Unit)
        }
    }

    fun clearHistory() = viewModelScope.launch {
        db.aiHistoryDao().clear()
    }
}

private data class ToolUi(val tool: AiTool, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(navController: NavController, premium: Boolean, vm: AiViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val history by vm.history.collectAsState(initial = emptyList())
    val settings by vm.settings.collectAsState(initial = null)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedTool by remember { mutableStateOf(AiTool.TITLE) }
    var topic by remember { mutableStateOf("") }
    var tone by remember { mutableIntStateOf(AiTone.EDUCATIONAL.ordinal) }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(premium) {
        vm.generationCompleted.collect {
            (context as? Activity)?.let { vm.adsManager.recordSuccessfulAiGeneration(it, premium) }
        }
    }

    val tools = listOf(
        ToolUi(AiTool.TITLE, Icons.Filled.Title),
        ToolUi(AiTool.DESCRIPTION, Icons.Filled.Description),
        ToolUi(AiTool.TAGS, Icons.Filled.Sell),
        ToolUi(AiTool.SCRIPT, Icons.Filled.EditNote),
        ToolUi(AiTool.IDEAS, Icons.Filled.Lightbulb)
    )


    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.ai_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                IconButton(onClick = { showHistory = !showHistory }) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = stringResource(R.string.ai_history))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { if (!premium) TapsellBanner() }
            item {
                Text(
                    stringResource(R.string.ai_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            /* انتخاب ابزار */
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tools) { t ->
                        val selected = t.tool == selectedTool
                        Card(
                            modifier = Modifier
                                .width(118.dp)
                                .clickable { selectedTool = t.tool },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(t.icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    toolLabel(t.tool),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(toolHint(selectedTool), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (showHistory) {
                item { HistoryCard(history, clipboard, onClear = { vm.clearHistory() }) }
            }

            /* موضوع و لحن */
            item {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text(stringResource(R.string.ai_topic)) },
                    placeholder = { Text(stringResource(R.string.ai_topic_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp)
                )
            }
            item {
                Text(stringResource(R.string.ai_tone), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        stringResource(R.string.tone_edu),
                        stringResource(R.string.tone_fun),
                        stringResource(R.string.tone_hype),
                        stringResource(R.string.tone_pro)
                    ).forEachIndexed { i, label ->
                        val selected = tone == i
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { tone = i }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                if (!premium) {
                    Text(
                        stringResource(R.string.ai_limit_left, if (ui.remaining == Int.MAX_VALUE) "∞" else ui.remaining.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
                val providerBadge = if (settings?.aiProvider == "offline" || settings?.aiApiKey.isNullOrBlank())
                    stringResource(R.string.ai_offline_badge) else stringResource(R.string.ai_remote_badge)
                Text(
                    "⚡ $providerBadge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        vm.generate(selectedTool, topic, AiTone.entries[tone], premium)
                    },
                    enabled = !ui.loading && topic.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (ui.loading) {
                        CircularProgressIndicator(Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.height(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (ui.result == null) stringResource(R.string.ai_generate)
                            else stringResource(R.string.ai_again)
                        )
                    }
                }
            }

            ui.note?.let {
                item {
                    Text(
                        stringResource(R.string.ai_error_remote),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            /* محدودیت رایگان */
            if (ui.limitReached) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.gate_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.ai_limit_reached), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { navController.navigate(Routes.PREMIUM) }, shape = RoundedCornerShape(12.dp)) {
                                Text(stringResource(R.string.premium_title))
                            }
                        }
                    }
                }
            }

            /* نتایج */
            ui.result?.let { result ->
                items(result.sections.size) { i ->
                    val section = result.sections[i]
                    ResultSection(section, clipboard)
                }
            }

        }
    }
}

@Composable
private fun toolLabel(tool: AiTool): String = when (tool) {
    AiTool.TITLE -> stringResource(R.string.tool_title_gen)
    AiTool.DESCRIPTION -> stringResource(R.string.tool_desc_gen)
    AiTool.TAGS -> stringResource(R.string.tool_tag_gen)
    AiTool.SCRIPT -> stringResource(R.string.tool_script_gen)
    AiTool.IDEAS -> stringResource(R.string.tool_idea_gen)
}

@Composable
private fun toolHint(tool: AiTool): String = when (tool) {
    AiTool.TITLE -> stringResource(R.string.tool_title_desc)
    AiTool.DESCRIPTION -> stringResource(R.string.tool_desc_desc)
    AiTool.TAGS -> stringResource(R.string.tool_tag_desc)
    AiTool.SCRIPT -> stringResource(R.string.tool_script_desc)
    AiTool.IDEAS -> stringResource(R.string.tool_idea_desc)
}

@Composable
private fun ResultSection(section: AiSection, clipboard: androidx.compose.ui.platform.ClipboardManager) {
    SectionCard(title = section.title) {
        Text(section.content, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(section.content)) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, Modifier.height(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.copy), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun HistoryCard(
    items: List<AiHistory>,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onClear: () -> Unit
) {
    if (items.isEmpty()) return
    SectionCard(title = stringResource(R.string.ai_history)) {
        items.take(6).forEach { h ->
            val date = SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(h.createdAt))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { clipboard.setText(AnnotatedString(h.result)) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(h.topic, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text("$date • ${h.toolType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = onClear, shape = RoundedCornerShape(10.dp)) {
                Text(stringResource(R.string.ai_clear), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
