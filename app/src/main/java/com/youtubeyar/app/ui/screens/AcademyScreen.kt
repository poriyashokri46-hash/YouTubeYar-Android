package com.youtubeyar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.data.AcademyContent
import com.youtubeyar.app.data.AcademyContent.Lesson
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AcademyViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings = settingsRepository.settings

    fun toggleLesson(id: Int) = viewModelScope.launch { settingsRepository.toggleLesson(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(navController: NavController, vm: AcademyViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsState(initial = null)
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    val isEn = Locale.getDefault().language != "fa"
    val doneIds = (settings?.lessonsDone ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }

    val categories = if (isEn) AcademyContent.categoriesEn else AcademyContent.categoriesFa

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.ac_title)) },
            navigationIcon = {
                if (selectedLesson != null) {
                    IconButton(onClick = { selectedLesson = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                } else {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (selectedLesson == null) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.ac_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.ac_progress, doneIds.size, AcademyContent.lessons.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { doneIds.size.toFloat() / AcademyContent.lessons.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                categories.forEachIndexed { catIndex, catName ->
                    item(key = "cat_$catIndex") {
                        Text(
                            catName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    val catLessons = AcademyContent.lessons.filter { it.category == catIndex }
                    items(count = catLessons.size, key = { i -> catLessons[i].id }) { i ->
                        val lesson = catLessons[i]
                        val done = doneIds.contains(lesson.id)
                        SectionCard {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLesson = lesson }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (isEn) lesson.titleEn else lesson.titleFa,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        stringResource(R.string.ac_minutes, lesson.minutes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (done) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = stringResource(R.string.ac_completed),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LessonDetail(
                lesson = selectedLesson!!,
                isEn = isEn,
                done = doneIds.contains(selectedLesson!!.id),
                onToggleDone = { vm.toggleLesson(selectedLesson!!.id) }
            )
        }
    }
}

@Composable
private fun ColumnScope.LessonDetail(lesson: Lesson, isEn: Boolean, done: Boolean, onToggleDone: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (isEn) lesson.titleEn else lesson.titleFa,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.ac_minutes, lesson.minutes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        val body = if (isEn) lesson.bodyEn else lesson.bodyFa
        body.forEach { paragraph ->
            Text(
                paragraph,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        SectionCard(title = stringResource(R.string.ac_key_points)) {
            val points = if (isEn) lesson.pointsEn else lesson.pointsFa
            points.forEach { p ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text("✓ ", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                    Text(p, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onToggleDone,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (done) stringResource(R.string.ac_completed) else stringResource(R.string.ac_mark_done))
        }
        Spacer(Modifier.height(24.dp))
    }
}
