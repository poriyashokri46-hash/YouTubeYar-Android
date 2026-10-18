package com.youtubeyar.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.data.AppSettings
import com.youtubeyar.app.data.AppDatabase
import com.youtubeyar.app.data.ChecklistTask
import com.youtubeyar.app.data.RoadmapContent
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val tasks: Flow<List<ChecklistTask>> = db.checklistDao().observeAll()
    val settings = settingsRepository.settings

    init { seedIfEmpty() }

    private fun seedIfEmpty() = viewModelScope.launch {
        if (db.checklistDao().count() == 0) {
            val isEn = Locale.getDefault().language != "fa"
            db.checklistDao().insertAll(RoadmapContent.seedTasks(isEn))
        }
    }

    fun setDone(task: ChecklistTask, done: Boolean) = viewModelScope.launch {
        db.checklistDao().setDone(task.id, done, if (done) System.currentTimeMillis() else null)
    }

    fun addCustomTask(phase: Int, title: String) = viewModelScope.launch {
        if (title.isNotBlank()) db.checklistDao().insert(
            ChecklistTask(phase = phase, orderIndex = 999, title = title, description = "", isCustom = true)
        )
    }

    fun deleteTask(task: ChecklistTask) = viewModelScope.launch { db.checklistDao().delete(task.id) }

    fun dailyState(s: AppSettings): List<Boolean> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (s.dailyDate != today) return List(RoadmapContent.dailyTasks.size) { false }
        val parsed = s.dailyState.split(",").map { it == "1" }
        return List(RoadmapContent.dailyTasks.size) { parsed.getOrElse(it) { false } }
    }

    fun toggleDaily(i: Int) = viewModelScope.launch {
        val s = settingsRepository.current()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val state = if (s.dailyDate == today) s.dailyState.split(",").map { it == "1" } else List(6) { false }
        val newState = state.toMutableList().also {
            while (it.size < RoadmapContent.dailyTasks.size) it.add(false)
            it[i] = !it[i]
        }
        settingsRepository.setDailyState(today, newState.take(RoadmapContent.dailyTasks.size))
    }

    fun setReminder(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setReminderEnabled(enabled)
    }
}

private val phaseNames = listOf(R.string.cl_phase1, R.string.cl_phase2, R.string.cl_phase3, R.string.cl_phase4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(navController: NavController, vm: ChecklistViewModel = hiltViewModel()) {
    val tasks by vm.tasks.collectAsState(initial = emptyList())
    val settings by vm.settings.collectAsState(initial = null)
    var addDialogPhase by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    // مجوز نوتیفیکیشن (اندروید ۱۳+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.setReminder(granted) }

    fun requestReminder(enable: Boolean) {
        if (enable && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setReminder(enable)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.cl_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.cl_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            /* وظایف روزانه */
            item {
                DailyTasksCard(vm, settings)
            }

            /* یادآور */
            item {
                SectionCard(title = stringResource(R.string.st_reminder_section)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.st_reminder), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.st_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = settings?.reminderEnabled ?: false,
                            onCheckedChange = { requestReminder(it) }
                        )
                    }
                }
            }

            /* فازها */
            val isEn = Locale.getDefault().language != "fa"
            items(count = 4) { phase ->
                PhaseCard(
                    phaseIndex = phase,
                    phaseTitle = stringResource(phaseNames[phase]),
                    tasks = tasks.filter { it.phase == phase },
                    isEnglish = isEn,
                    onToggle = { vm.setDone(it, !it.isDone) },
                    onAdd = { addDialogPhase = phase },
                    onDelete = { vm.deleteTask(it) }
                )
            }
        }

        addDialogPhase?.let { phase ->
            AddTaskDialog(
                onDismiss = { addDialogPhase = null },
                onAdd = { title ->
                    vm.addCustomTask(phase, title)
                    addDialogPhase = null
                }
            )
        }
    }
}

@Composable
private fun DailyTasksCard(vm: ChecklistViewModel, settings: AppSettings?) {
    val dailyDone = if (settings == null) List(6) { false } else vm.dailyState(settings)
    val isEn = Locale.getDefault().language != "fa"
    SectionCard(title = stringResource(R.string.cl_daily)) {
        Text(
            stringResource(R.string.cl_daily_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        RoadmapContent.dailyTasks.forEachIndexed { i, t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.toggleDaily(i) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = dailyDone[i], onCheckedChange = { vm.toggleDaily(i) })
                Text(
                    if (isEn) t.titleEn else t.titleFa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dailyDone[i]) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (dailyDone.isEmpty()) 0f else dailyDone.count { it }.toFloat() / dailyDone.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun PhaseCard(
    phaseIndex: Int,
    phaseTitle: String,
    tasks: List<ChecklistTask>,
    isEnglish: Boolean,
    onToggle: (ChecklistTask) -> Unit,
    onAdd: () -> Unit,
    onDelete: (ChecklistTask) -> Unit
) {
    var expanded by remember { mutableStateOf(phaseIndex == 0) }
    val doneCount = tasks.count { it.isDone }
    val progress = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size

    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(phaseTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.cl_progress, doneCount, tasks.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary
        )
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(8.dp))
                if (progress == 1f && tasks.isNotEmpty()) {
                    Text(
                        stringResource(R.string.cl_all_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                }
                tasks.forEach { task ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(checked = task.isDone, onCheckedChange = { onToggle(task) })
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    task.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (task.isCustom) {
                            IconButton(onClick = { onDelete(task) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.width(20.dp)
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null, Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.cl_add_task))
                }
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cl_add_task)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.cl_new_task_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
