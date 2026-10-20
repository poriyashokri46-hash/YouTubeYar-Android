package com.youtubeyar.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "youtubeyar_settings")

/** تنظیمات و وضعیت اپ — همه در DataStore ذخیره می‌شود */
data class AppSettings(
    val theme: String = "dark",              // dark / light / system
    val firstRunDone: Boolean = false,
    val channelName: String = "",
    val channelHandle: String = "",
    val channelNiche: String = "",
    val youtubeApiKey: String = "",
    val aiProvider: String = "offline",      // offline / gemini / openai / deepseek
    val aiApiKey: String = "",
    val bazaarRsa: String = "",
    val myketRsa: String = "",
    val premiumActive: Boolean = false,
    val premiumTempUntil: Long = 0,          // Legacy field retained for data compatibility
    val rewardedAiUses: Int = 0,              // تعداد استفاده‌های AI که با تبلیغ جایزه‌ای باز شده‌اند
    val aiUsedToday: Int = 0,
    val aiUsedDate: String = "",
    val dailyDate: String = "",              // تاریخ آخرین ریست وظایف روزانه
    val dailyState: String = "0,0,0,0,0,0",  // وضعیت ۶ وظیفه روزانه
    val milestones: String = "",             // لیست مایل‌استون‌های انجام‌شده مثل "0,2,4"
    val lessonsDone: String = "",            // درس‌های خوانده‌شده آکادمی مثل "1,3,5"
    val reminderEnabled: Boolean = false
) {
    fun isPremium(now: Long = System.currentTimeMillis()): Boolean = premiumActive
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object K {
        val THEME = stringPreferencesKey("theme")
        val FIRST_RUN = booleanPreferencesKey("first_run_done")
        val CHANNEL_NAME = stringPreferencesKey("channel_name")
        val CHANNEL_HANDLE = stringPreferencesKey("channel_handle")
        val CHANNEL_NICHE = stringPreferencesKey("channel_niche")
        val YT_KEY = stringPreferencesKey("youtube_api_key")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_KEY = stringPreferencesKey("ai_api_key")
        val BAZAAR_RSA = stringPreferencesKey("bazaar_rsa")
        val MYKET_RSA = stringPreferencesKey("myket_rsa")
        val PREMIUM = booleanPreferencesKey("premium_active")
        val PREMIUM_TOKEN = stringPreferencesKey("premium_token")
        val PREMIUM_TEMP = longPreferencesKey("premium_temp_until")
        val REWARDED_AI_USES = intPreferencesKey("rewarded_ai_uses")
        val AI_USED_COUNT = intPreferencesKey("ai_used_count")
        val AI_USED_DATE = stringPreferencesKey("ai_used_date")
        val DAILY_DATE = stringPreferencesKey("daily_date")
        val DAILY_STATE = stringPreferencesKey("daily_state")
        val MILESTONES = stringPreferencesKey("milestones")
        val LESSONS = stringPreferencesKey("lessons_done")
        val REMINDER = booleanPreferencesKey("reminder_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = p[K.THEME] ?: "dark",
            firstRunDone = p[K.FIRST_RUN] ?: false,
            channelName = p[K.CHANNEL_NAME] ?: "",
            channelHandle = p[K.CHANNEL_HANDLE] ?: "",
            channelNiche = p[K.CHANNEL_NICHE] ?: "",
            youtubeApiKey = (p[K.YT_KEY] ?: "").ifBlank { context.getString(com.youtubeyar.app.R.string.youtube_api_key).trim() },
            aiProvider = p[K.AI_PROVIDER] ?: "offline",
            aiApiKey = p[K.AI_KEY] ?: "",
            bazaarRsa = p[K.BAZAAR_RSA] ?: "",
            myketRsa = p[K.MYKET_RSA] ?: "",
            premiumActive = p[K.PREMIUM] ?: false,
            premiumTempUntil = p[K.PREMIUM_TEMP] ?: 0,
            rewardedAiUses = p[K.REWARDED_AI_USES] ?: 0,
            aiUsedToday = p[K.AI_USED_COUNT] ?: 0,
            aiUsedDate = p[K.AI_USED_DATE] ?: "",
            dailyDate = p[K.DAILY_DATE] ?: "",
            dailyState = p[K.DAILY_STATE] ?: "0,0,0,0,0,0",
            milestones = p[K.MILESTONES] ?: "",
            lessonsDone = p[K.LESSONS] ?: "",
            reminderEnabled = p[K.REMINDER] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun setTheme(v: String) = context.dataStore.edit { it[K.THEME] = v }
    suspend fun setFirstRunDone() = context.dataStore.edit { it[K.FIRST_RUN] = true }
    suspend fun setChannel(name: String, handle: String, niche: String) = context.dataStore.edit {
        it[K.CHANNEL_NAME] = name
        it[K.CHANNEL_HANDLE] = handle
        it[K.CHANNEL_NICHE] = niche
    }
    suspend fun setYoutubeKey(v: String) = context.dataStore.edit { it[K.YT_KEY] = v.trim() }
    suspend fun setAiProvider(v: String) = context.dataStore.edit { it[K.AI_PROVIDER] = v }
    suspend fun setAiKey(v: String) = context.dataStore.edit { it[K.AI_KEY] = v.trim() }
    suspend fun setBazaarRsa(v: String) = context.dataStore.edit { it[K.BAZAAR_RSA] = v.trim() }
    suspend fun setMyketRsa(v: String) = context.dataStore.edit { it[K.MYKET_RSA] = v.trim() }
    suspend fun setReminderEnabled(v: Boolean) = context.dataStore.edit { it[K.REMINDER] = v }

    suspend fun setPremium(active: Boolean, token: String = "") = context.dataStore.edit {
        it[K.PREMIUM] = active
        if (active && token.isNotEmpty()) it[K.PREMIUM_TOKEN] = token
        if (!active) it[K.PREMIUM_TOKEN] = ""
    }

    /** هر تبلیغ جایزه‌ای موفق، دقیقاً دو بار استفاده از ابزار AI می‌دهد. */
    suspend fun grantRewardedUnlock() = context.dataStore.edit { p ->
        p[K.REWARDED_AI_USES] = ((p[K.REWARDED_AI_USES] ?: 0) + REWARDED_USES_PER_AD).coerceAtMost(100)
    }

    /**
     * ثبت یک بار استفاده از ابزار AI با شمارنده روزانه.
     * برای کاربر رایگان روزی ۵ بار مجاز است؛ پرمیوم نامحدود.
     */
    suspend fun consumeAiUse(): Boolean {
        val todayStr = today()
        var allowed = false
        context.dataStore.edit { p ->
            val count = if (p[K.AI_USED_DATE] == todayStr) p[K.AI_USED_COUNT] ?: 0 else 0
            val premiumNow = p[K.PREMIUM] ?: false
            val rewardedUses = p[K.REWARDED_AI_USES] ?: 0
            if (premiumNow) {
                allowed = true
            } else if (rewardedUses > 0) {
                p[K.REWARDED_AI_USES] = rewardedUses - 1
                allowed = true
            } else if (count < FREE_DAILY_LIMIT) {
                p[K.AI_USED_COUNT] = count + 1
                p[K.AI_USED_DATE] = todayStr
                allowed = true
            }
        }
        return allowed
    }

    suspend fun remainingAiUses(): Int {
        val s = current()
        if (s.isPremium()) return Int.MAX_VALUE
        val count = if (s.aiUsedDate == today()) s.aiUsedToday else 0
        return (FREE_DAILY_LIMIT - count).coerceAtLeast(0)
    }

    suspend fun setDailyState(date: String, state: List<Boolean>) =
        context.dataStore.edit {
            it[K.DAILY_DATE] = date
            it[K.DAILY_STATE] = state.joinToString(",") { b -> if (b) "1" else "0" }
        }

    suspend fun toggleLesson(id: Int) = context.dataStore.edit { p ->
        val current = (p[K.LESSONS] ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        p[K.LESSONS] = current.joinToString(",")
    }

    suspend fun toggleMilestone(index: Int) = context.dataStore.edit { p ->
        val current = (p[K.MILESTONES] ?: "").split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableSet()
        if (current.contains(index)) current.remove(index) else current.add(index)
        p[K.MILESTONES] = current.joinToString(",")
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    companion object {
        const val FREE_DAILY_LIMIT = 5
        const val REWARDED_USES_PER_AD = 2
    }
}
