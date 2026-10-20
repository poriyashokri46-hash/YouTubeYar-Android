package com.youtubeyar.app.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * سرویس هوش مصنوعی آنلاین (اختیاری):
 * کاربر می‌تواند در تنظیمات یکی از سرویس‌های Gemini / OpenAI / DeepSeek را
 * با کلید خودش فعال کند تا کیفیت خروجی بالاتر برود. پیش‌فرض اپ موتور آفلاین است.
 */
@Singleton
class RemoteAiProvider @Inject constructor(private val client: OkHttpClient) {

    data class ProviderInfo(val id: String, val name: String, val keyHint: String)

    val providers: List<ProviderInfo> = listOf(
        ProviderInfo("offline", "آفلاین (داخل اپ)", "نیاز به کلید ندارد"),
        ProviderInfo("gemini", "Google Gemini", "کلید رایگان از aistudio.google.com"),
        ProviderInfo("openai", "OpenAI", "کلید از platform.openai.com"),
        ProviderInfo("deepseek", "DeepSeek", "کلید از platform.deepseek.com")
    )

    suspend fun generate(provider: String, apiKey: String, prompt: String): Result<String> {
        if (apiKey.isBlank()) return Result.failure(IllegalStateException("NO_KEY"))
        return withContext(Dispatchers.IO) {
            try {
                val text = when (provider) {
                    "gemini" -> callGemini(apiKey, prompt)
                    "openai" -> callOpenAiCompatible("https://api.openai.com/v1/chat/completions", apiKey, "gpt-4o-mini", prompt)
                    "deepseek" -> callOpenAiCompatible("https://api.deepseek.com/chat/completions", apiKey, "deepseek-chat", prompt)
                    else -> return@withContext Result.failure(IllegalArgumentException("UNKNOWN_PROVIDER"))
                }
                if (text.isBlank()) Result.failure(IllegalStateException("EMPTY_RESPONSE"))
                else Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun callGemini(key: String, prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        }
        val request = Request.Builder().url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            val json = JSONObject(resp.body?.string().orEmpty())
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: ${json.optJSONObject("error")?.optString("message")}")
            return json.getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text")
        }
    }

    private suspend fun callOpenAiCompatible(url: String, key: String, model: String, prompt: String): String {
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.8)
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
        }
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            val json = JSONObject(resp.body?.string().orEmpty())
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}: ${json.optJSONObject("error")?.optString("message")}")
            return json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }

    /** ساخت پرامپت حرفه‌ای فارسی بر اساس ابزار انتخابی */
    fun buildPrompt(tool: AiTool, topic: String, niche: String, tone: AiTone): String {
        val toneText = when (tone) {
            AiTone.EDUCATIONAL -> "آموزشی و صمیمی"
            AiTone.ENTERTAINING -> "سرگرم‌کننده و بامزه"
            AiTone.HYPE -> "هیجانی و پرانرژی"
            AiTone.PROFESSIONAL -> "حرفه‌ای و مستند"
        }
        val base = "تو یک متخصص سئو و رشد کانال یوتیوب هستی. موضوع ویدیو: «$topic». حوزه کانال: «$niche». لحن خواسته‌شده: $toneText. پاسخ را فقط به فارسی روان و بدون توضیح اضافه بده.\n\n"
        return when (tool) {
            AiTool.TITLE -> base + "۸ عنوان سئوشده برای این ویدیو پیشنهاد بده. هر عنوان زیر ۶۰ کاراکتر، جذاب و کلیک‌خور باشد. سپس بهترین عنوان را با دلیل کوتاه مشخص کن."
            AiTool.DESCRIPTION -> base + "یک توضیحات کامل و حرفه‌ای یوتیوب برای این ویدیو بنویس: پاراگراف جذاب اول با کلیدواژه، بخش «چه چیزهایی یاد می‌گیرید» با بولت، زمان‌بندی تایم‌استمپ‌دار، فراخوان لایک و سابسکرایب و ۵ هشتگ."
            AiTool.TAGS -> base + "۱۵ تگ یوتیوب برای این ویدیو پیشنهاد بده. تگ‌ها را با ویرگول جدا کن. ترکیبی از تگ‌های کوتاه و بلند و مرتبط با جستجوهای فارسی باشد."
            AiTool.SCRIPT -> base + "یک اسکریپت کامل ویدیوی ۱۰ دقیقه‌ای بنویس: هوک ۱۵ ثانیه‌ای، معرفی کوتاه، سه بخش اصلی با نکات قابل اجرا، و پایان با CTA. زمان‌بندی هر بخش را هم بنویس."
            AiTool.IDEAS -> base + "۱۰ ایده ویدیو برای این کانال پیشنهاد بده که با موضوع اصلی مرتبط باشد و پتانسیل جذب بازدید داشته باشد. برای هر ایده یک جمله توضیح کوتاه اضافه کن."
        }
    }
}
