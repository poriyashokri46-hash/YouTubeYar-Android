package com.youtubeyar.app.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.ads.TapsellBanner
import com.youtubeyar.app.data.ProgressEntry
import com.youtubeyar.app.data.RoadmapContent
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.data.AppDatabase
import com.youtubeyar.app.ui.Routes
import com.youtubeyar.app.ui.components.Fmt
import com.youtubeyar.app.ui.components.ProgressRing
import com.youtubeyar.app.ui.components.SectionCard
import com.youtubeyar.app.ui.components.SimpleLineChart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val progressDao: com.youtubeyar.app.data.ProgressDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings

    private val _entries = MutableStateFlow<List<ProgressEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _entries.value = progressDao.all().sortedBy { it.createdAt }
    }

    fun saveStats(subs: Int, hours: Float) = viewModelScope.launch {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        progressDao.insert(ProgressEntry(date = today, subscribers = subs, watchHours = hours))
        refresh()
    }

    fun toggleMilestone(i: Int) = viewModelScope.launch { settingsRepository.toggleMilestone(i) }
}

private fun isFa(): Boolean = Locale.getDefault().language == "fa"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, premium: Boolean, vm: DashboardViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsState(initial = null)
    val entries by vm.entries.collectAsState()
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    val s = settings
    val latest = entries.lastOrNull()
    val currentSubs = latest?.subscribers ?: 0
    val currentHours = latest?.watchHours ?: 0f
    val persian = isFa()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        stringResource(R.string.dash_hello, s?.channelName ?: stringResource(R.string.app_name)),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        stringResource(R.string.dash_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { navController.navigate(Routes.PREMIUM) }) {
                    Icon(
                        Icons.Filled.WorkspacePremium,
                        contentDescription = stringResource(R.string.premium_title),
                        tint = if (premium) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /* ---------- دو حلقه پیشرفت ---------- */
            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProgressRing(
                            progress = currentSubs / 1000f,
                            size = 130.dp,
                            content = {
                                Text(Fmt.compact(currentSubs.toLong(), persian), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(R.string.dash_of_1000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                        ProgressRing(
                            progress = currentHours / 4000f,
                            size = 130.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            content = {
                                Text(Fmt.faFloat(currentHours), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                Text(stringResource(R.string.dash_of_4000), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text(
                            stringResource(R.string.dash_remaining_subs, (1000 - currentSubs).coerceAtLeast(0)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.dash_remaining_hours, Fmt.faFloat((4000 - currentHours).coerceAtLeast(0f))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.dash_update_stats))
                    }
                }
            }

            /* ---------- ماشین‌حساب مانیتایز ---------- */
            item {
                SectionCard(title = stringResource(R.string.dash_calculator)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.dash_calc_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    EtaSection(entries)
                }
            }

            /* ---------- نمودار پیشرفت ---------- */
            item {
                SectionCard(title = stringResource(R.string.dash_chart)) {
                    if (entries.size >= 2) {
                        SimpleLineChart(values = entries.map { it.subscribers.toFloat() })
                        Spacer(Modifier.height(6.dp))
                        SimpleLineChart(
                            values = entries.map { it.watchHours },
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text(
                                stringResource(R.string.dash_subs_progress) + "  •  " + stringResource(R.string.dash_hours_progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.dash_no_history),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            /* ---------- مایل‌استون‌ها ---------- */
            item {
                SectionCard(title = stringResource(R.string.dash_milestones)) {
                    val done = (s?.milestones ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
                    RoadmapContent.milestones.forEachIndexed { i, m ->
                        val isDone = done.contains(i)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { vm.toggleMilestone(i) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (persian) m.titleFa else m.titleEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            /* ---------- شرایط مانیتایز ---------- */
            item {
                SectionCard(title = stringResource(R.string.dash_requirements)) {
                    listOf(
                        stringResource(R.string.dash_req_1),
                        stringResource(R.string.dash_req_2),
                        stringResource(R.string.dash_req_3),
                        stringResource(R.string.dash_req_4)
                    ).forEach { req ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(req, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.dash_req_alt),
                            Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(2.dp)) }
        }

        if (!premium) TapsellBanner()
    }

    if (showDialog) {
        UpdateStatsDialog(
            initialSubs = currentSubs,
            initialHours = currentHours,
            onDismiss = { showDialog = false },
            onSave = { subs, hours ->
                vm.saveStats(subs, hours)
                showDialog = false
            }
        )
    }
}

@Composable
private fun EtaSection(entries: List<ProgressEntry>) {
    val persian = isFa()
    if (entries.size < 2) {
        Text(
            stringResource(R.string.dash_eta_need_data),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val first = entries.first()
    val last = entries.last()
    val days = ((last.createdAt - first.createdAt) / (24 * 60 * 60 * 1000.0)).coerceAtLeast(1.0)
    val subsPerDay = (last.subscribers - first.subscribers) / days
    val hoursPerDay = (last.watchHours - first.watchHours) / days

    val etaSubs = if (subsPerDay > 0) ((1000 - last.subscribers) / subsPerDay).toLong() else -1
    val etaHours = if (hoursPerDay > 0) ((4000 - last.watchHours) / hoursPerDay).toLong() else -1

    val etaTextSubs = if (etaSubs >= 0) {
        stringResource(R.string.dash_eta_days, if (persian) Fmt.fa(etaSubs.toString()) else etaSubs.toString())
    } else stringResource(R.string.dash_eta_need_data)
    val etaTextHours = if (etaHours >= 0) {
        stringResource(R.string.dash_eta_days, if (persian) Fmt.fa(etaHours.toString()) else etaHours.toString())
    } else stringResource(R.string.dash_eta_need_data)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.dash_eta), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text("⏱ " + stringResource(R.string.dash_eta_subs, etaTextSubs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("⏱ " + stringResource(R.string.dash_eta_hours, etaTextHours), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun UpdateStatsDialog(
    initialSubs: Int,
    initialHours: Float,
    onDismiss: () -> Unit,
    onSave: (Int, Float) -> Unit
) {
    var subsText by remember { mutableStateOf(if (initialSubs > 0) initialSubs.toString() else "") }
    var hoursText by remember { mutableStateOf(if (initialHours > 0f) "%.0f".format(Locale.US, initialHours) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dash_update_stats)) },
        text = {
            Column {
                OutlinedTextField(
                    value = subsText,
                    onValueChange = { subsText = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text(stringResource(R.string.dash_subs_input)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter { c -> c.isDigit() || c == '.' }.take(7) },
                    label = { Text(stringResource(R.string.dash_hours_input)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val subs = subsText.toIntOrNull() ?: 0
                val hours = hoursText.toFloatOrNull() ?: 0f
                if (subs >= 0 || hours >= 0f) onSave(subs, hours)
            }) { Text(stringResource(R.string.dash_save)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
