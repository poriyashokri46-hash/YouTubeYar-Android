package com.youtubeyar.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.ai.RemoteAiProvider
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.ui.components.SelectableRow
import com.youtubeyar.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    val remote: RemoteAiProvider,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val settings = settingsRepository.settings

    fun setTheme(v: String) = viewModelScope.launch { settingsRepository.setTheme(v) }
    fun saveChannel(name: String, handle: String, niche: String) =
        viewModelScope.launch { settingsRepository.setChannel(name, handle, niche) }

    fun setYoutubeKey(v: String) = viewModelScope.launch { settingsRepository.setYoutubeKey(v) }
    fun setAiProvider(v: String) = viewModelScope.launch { settingsRepository.setAiProvider(v) }
    fun setAiKey(v: String) = viewModelScope.launch { settingsRepository.setAiKey(v) }
    fun setBazaarRsa(v: String) = viewModelScope.launch { settingsRepository.setBazaarRsa(v) }
    fun setMyketRsa(v: String) = viewModelScope.launch { settingsRepository.setMyketRsa(v) }
    fun setReminder(enabled: Boolean) = viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsState(initial = null)
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var savedTick by remember { mutableStateOf(false) }

    val s = settings

    // فیلدها
    var name by remember(s?.channelName) { mutableStateOf(s?.channelName ?: "") }
    var handle by remember(s?.channelHandle) { mutableStateOf(s?.channelHandle ?: "") }
    var niche by remember(s?.channelNiche) { mutableStateOf(s?.channelNiche ?: "") }
    var ytKey by remember(s?.youtubeApiKey) { mutableStateOf(s?.youtubeApiKey ?: "") }
    var aiKey by remember(s?.aiApiKey) { mutableStateOf(s?.aiApiKey ?: "") }
    var rsa by remember(s?.bazaarRsa) { mutableStateOf(s?.bazaarRsa ?: "") }
    var myketRsa by remember(s?.myketRsa) { mutableStateOf(s?.myketRsa ?: "") }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminder(granted)
    }

    fun requestReminder(enable: Boolean) {
        if (enable && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.setReminder(enable)
    }

    LaunchedEffect(savedTick) {
        if (savedTick) {
            snackbar.showSnackbar(context.getString(R.string.st_saved))
            savedTick = false
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (s == null) { Text(stringResource(R.string.loading)); return@Column }

            /* ظاهر */
            SectionCard(title = stringResource(R.string.st_appearance)) {
                Text(stringResource(R.string.st_theme), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                val themes = listOf(
                    "dark" to stringResource(R.string.st_dark),
                    "light" to stringResource(R.string.st_light),
                    "system" to stringResource(R.string.st_system)
                )
                themes.forEach { (key, label) ->
                    SelectableRow(label, s.theme == key) { vm.setTheme(key) }
                }
            }

            /* کانال */
            SectionCard(title = stringResource(R.string.st_channel_section)) {
                Text(
                    stringResource(R.string.st_channel_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.st_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text(stringResource(R.string.st_handle)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = niche, onValueChange = { niche = it }, label = { Text(stringResource(R.string.st_niche)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.saveChannel(name, handle, niche); savedTick = true }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.save))
                }
            }

            /* کلیدها */
            SectionCard(title = stringResource(R.string.st_keys_section)) {
                OutlinedTextField(
                    value = ytKey, onValueChange = { ytKey = it },
                    label = { Text(stringResource(R.string.st_yt_key)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text(
                    stringResource(R.string.st_yt_key_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = { vm.setYoutubeKey(ytKey); savedTick = true }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.save))
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.st_ai_provider), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                vm.remote.providers.forEach { p ->
                    SelectableRow(p.name, s.aiProvider == p.id) {
                        vm.setAiProvider(p.id)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (s.aiProvider == "offline") {
                    Text(stringResource(R.string.st_ai_offline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                } else {
                    OutlinedTextField(
                        value = aiKey, onValueChange = { aiKey = it },
                        label = { Text(stringResource(R.string.st_ai_key)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { vm.setAiKey(aiKey); savedTick = true }, shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(R.string.save))
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = rsa, onValueChange = { rsa = it },
                    label = { Text(stringResource(R.string.st_bazaar_rsa)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 2
                )
                Text(
                    stringResource(R.string.st_bazaar_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = { vm.setBazaarRsa(rsa); savedTick = true }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.save))
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = myketRsa, onValueChange = { myketRsa = it },
                    label = { Text(stringResource(R.string.st_myket_rsa)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 2
                )
                Text(
                    stringResource(R.string.st_myket_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Button(onClick = { vm.setMyketRsa(myketRsa); savedTick = true }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.save))
                }
            }

            /* یادآور */
            SectionCard(title = stringResource(R.string.st_reminder_section)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.st_reminder), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.st_reminder_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = s.reminderEnabled, onCheckedChange = { requestReminder(it) })
                }
            }

            /* درباره */
            SectionCard(title = stringResource(R.string.st_about)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.st_version), style = MaterialTheme.typography.bodyMedium)
                    Text(com.youtubeyar.app.BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.st_lang_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "آیکون یوتیوب‌یار",
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "نام سازنده: سیدحمیدموسوی زاده",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "یوتیوب‌یار — از صفر تا مانیتایز 🚀",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(hostState = snackbar)
    }
}
