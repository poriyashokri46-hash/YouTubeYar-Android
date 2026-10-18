package com.youtubeyar.app.ai

/** ابزارهای هوش مصنوعی موجود در اپ */
enum class AiTool(val id: String) {
    TITLE("TITLE"),
    DESCRIPTION("DESCRIPTION"),
    TAGS("TAGS"),
    SCRIPT("SCRIPT"),
    IDEAS("IDEAS")
}

/** لحن محتوا */
enum class AiTone { EDUCATIONAL, ENTERTAINING, HYPE, PROFESSIONAL }

/** یک بخش از خروجی AI (برای نمایش جداگانه و کپی) */
data class AiSection(val title: String, val content: String)

/** خروجی کامل یک تولید */
data class AiResult(
    val tool: AiTool,
    val topic: String,
    val sections: List<AiSection>,
    val engine: String  // offline / gemini / openai / deepseek
)
