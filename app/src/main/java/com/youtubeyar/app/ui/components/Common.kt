package com.youtubeyar.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.min

/* ------------------------- اعداد فارسی و فرمت‌ها ------------------------- */

object Fmt {
    private val faDigits = mapOf(
        '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
        '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
    )

    fun fa(s: String): String = s.map { faDigits[it] ?: it }.joinToString("")

    fun fa(n: Int): String = fa(n.toString())
    fun fa(n: Long): String = fa(n.toString())

    fun compact(n: Long, persian: Boolean): String {
        val v = when {
            n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
            n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
            else -> n.toString()
        }
        return if (persian) fa(v.replace(".0M", "M").replace(".0K", "K")) else v
    }

    fun num(n: Long, persian: Boolean): String {
        val s = String.format(Locale.US, "%,d", n)
        return if (persian) fa(s) else s
    }

    fun faFloat(f: Float): String = fa(String.format(Locale.US, "%.0f", f))
}

/* ------------------------------ کارت بخش‌ها ----------------------------- */

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

/* ------------------------------ حلقه پیشرفت ----------------------------- */

@Composable
fun ProgressRing(
    progress: Float,              // 0..1
    size: Dp,
    stroke: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(progress) { target = progress.coerceIn(0f, 1f) }
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(700), label = "ring")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokePx, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - strokePx, this.size.height - strokePx)
            )
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.55f), color)),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    style = Stroke(strokePx, cap = StrokeCap.Round),
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(this.size.width - strokePx, this.size.height - strokePx)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

/* ------------------------------ نمودار خطی ------------------------------ */

@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val line = color
    val fill = color.copy(alpha = 0.15f)
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (values.size < 2) return@Canvas
        val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val stepX = this.size.width / (values.size - 1)
        fun y(v: Float) = this.size.height - 8f - (v / maxV) * (this.size.height - 20f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val yy = y(v)
            if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
        }
        val area = Path().apply {
            addPath(path)
            lineTo((values.size - 1) * stepX, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, fill)
        drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        values.forEachIndexed { i, v ->
            drawCircle(line, radius = min(5f, 5f), center = Offset(i * stepX, y(v)))
        }
    }
}

/* ------------------------------- چیپ آماری ------------------------------ */

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/* --------------------------- ردیف انتخاب‌گر ساده ------------------------ */

@Composable
fun SelectableRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}
