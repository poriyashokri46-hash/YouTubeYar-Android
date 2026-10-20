package com.youtubeyar.app.ai

import kotlin.random.Random

/**
 * موتور آفلاین مبتنی بر قواعد و الگوها؛ بدون اینترنت و بدون کلید API.
 * این موتور مدل زبانی محلی نیست؛ خروجی‌ها پیشنهادهای قابل ویرایش هستند و نباید
 * به‌عنوان آمار تأییدشده، تجربه شخصی یا زمان‌بندی واقعی ویدیو تلقی شوند.
 */
object OfflineAiEngine {

    /* ----------------------------- ابزار عمومی ----------------------------- */

    private val faStopwords = setOf(
        "از", "به", "با", "در", "که", "و", "برای", "این", "آن", "است", "هست", "می", "را", "یک",
        "تا", "هم", "ولی", "یا", "اگر", "چون", "پس", "بشه", "کنیم", "کنید", "درباره", "چگونه",
        "چطور", "بهترین", "خیلی", "بیشتر", "کمتر", "دارد", "دارم", "دارید", "بود", "شد", "شده"
    )
    private val enStopwords = setOf(
        "the", "a", "an", "for", "with", "of", "to", "how", "why", "in", "on", "is", "are",
        "and", "or", "my", "your", "about", "best", "very", "more", "less"
    )

    private fun keywords(topic: String): List<String> =
        topic.split(Regex("[\\s،,.:؛()\\-+]+"))
            .map { it.trim() }
            .filter { it.length > 1 && !faStopwords.contains(it) && !enStopwords.contains(it.lowercase()) }
            .distinct()

    private fun pick(list: List<String>, rnd: Random): String = list[rnd.nextInt(list.size)]

    private fun hashtag(value: String): String =
        value.trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^\\p{L}\\p{N}_]"), "")
            .trim('_')

    private val powerWordsFa = listOf(
        "رایگان", "سریع", "حرفه‌ای", "موفق", "رشد", "بهترین", "آسان", "آموزش", "راهنما", "مرحله‌به‌مرحله"
    )
    private val powerWordsEn = listOf(
        "free", "guide", "step-by-step", "beginner", "how to", "practical", "tips", "learn"
    )

    /* ------------------------------- عنوان‌ها ------------------------------ */

    private fun faTitles(topic: String, niche: String, tone: AiTone, rnd: Random): List<String> {
        val n = 5 + rnd.nextInt(7)          // 5..11
        val kw = keywords(topic).firstOrNull() ?: topic
        val hypePrefixes = listOf("شوکه‌کننده! ", "سرانجام: ", "باورنکردنی! ")
        val proPrefixes = listOf("راهنمای جامع ", "مستند ", "بررسی کامل ")
        val funPrefixes = listOf("😂 ", "🔥 ", "😱 ")

        val base = listOf(
            "$n نکته کاربردی برای شروع $topic",
            "چطور مشکلات رایج در $topic را بررسی کنیم؟",
            "راهنمای مرحله‌به‌مرحله برای یادگیری $topic",
            "$n اشتباه رایج هنگام کار با $topic",
            "نکته‌های مهم برای پیشرفت در $topic",
            "$topic: روش کلاسیک یا مدرن؟ انتخاب درست",
            "چه چیزهایی هنگام تمرین $topic باید بررسی کنید؟",
            "راهنمای شروع $topic برای مبتدی‌ها",
            "روش‌های کاربردی برای شروع $topic",
            "$n ابزار و منبع برای شروع کار با $topic",
            "مروری بر مفاهیم اصلی $kw در موضوع $niche",
            "نقشه راه یادگیری $topic از سطح مقدماتی"
        ).shuffled(rnd)

        val decorated = base.take(8).map { title ->
            when (tone) {
                AiTone.HYPE -> if (rnd.nextBoolean()) pick(hypePrefixes, rnd) + title else title
                AiTone.PROFESSIONAL -> if (rnd.nextBoolean()) pick(proPrefixes, rnd) + title else title
                AiTone.ENTERTAINING -> if (rnd.nextBoolean()) pick(funPrefixes, rnd) + title else title
                AiTone.EDUCATIONAL -> title
            }
        }
        return decorated
    }

    private fun enTitles(topic: String, niche: String, rnd: Random): List<String> {
        val n = 5 + rnd.nextInt(7)
        return listOf(
            "$n useful ideas about $topic",
            "How to troubleshoot common $topic problems",
            "A step-by-step guide to learning $topic",
            "$n common mistakes beginners make with $topic",
            "Key concepts to understand about $topic",
            "$topic: classic vs modern, which one wins?",
            "What to review when practicing $topic",
            "Beginner’s guide to $topic ($niche)"
        ).shuffled(rnd)
    }

    /** امتیاز تخمینی عنوان از ۱ تا ۱۰؛ معیار رسمی رتبه‌بندی یوتیوب نیست. */
    fun seoScore(title: String, isEnglish: Boolean = false): Int {
        var score = 4
        val len = title.length
        if (len in 25..60) score += 2 else if (len in 18..70) score += 1 else if (len > 75) score -= 2
        if (title.any { it.isDigit() }) score += 2
        val powerWords = if (isEnglish) powerWordsEn else powerWordsFa
        if (powerWords.any { title.contains(it, ignoreCase = isEnglish) }) score += 2
        if (title.contains("?") || title.contains("؟")) score += 1
        return score.coerceIn(1, 10)
    }

    /* ------------------------------ توضیحات ------------------------------- */

    private fun faDescription(topic: String, niche: String, rnd: Random): String {
        val kw = keywords(topic)
        val kwLine = kw.take(4).joinToString("، ")
        val bullets = listOf(
            "تعریف ساده و کاربردی $topic برای شروع",
            "قدم‌به‌قدم انجام دادن ${pick(kw.ifEmpty { listOf(topic) }, rnd)} بدون سردرگمی",
            "اشتباهات رایج که باید از اول از آن‌ها دوری کنید",
            "نکات تکمیلی برای تمرین بهتر",
            "منابع رایگان برای ادامه مسیر یادگیری"
        ).shuffled(rnd).take(4)

        return buildString {
            appendLine("این یک قالب پیشنهادی برای توضیحات ویدیویی درباره $topic است؛ بخش‌ها را متناسب با محتوای واقعی ویدیو ویرایش کنید. اگر موضوع کانال شما $niche است، نکات را با مثال‌های واقعی خودتان تکمیل کنید.")
            appendLine()
            appendLine("🔥 در این ویدیو یاد می‌گیرید:")
            bullets.forEach { appendLine("• $it") }
            appendLine()
            appendLine("⏱ زمان‌بندی پیشنهادی (پس از تدوین ویدیو اصلاح شود):")
            appendLine("0:00 شروع و معرفی موضوع")
            appendLine("1:10 توضیح مفاهیم اصلی")
            appendLine("4:30 اجرای عملی و قدم‌به‌قدم")
            appendLine("8:00 اشتباهات رایج")
            appendLine("10:15 جمع‌بندی و قدم بعدی")
            appendLine()
            appendLine("📌 اگر این ویدیو برای شما مفید بود، لایک کنید و کانال را سابسکرایب کنید تا ویدیوهای بعدی $topic را از دست ندهید! 🔔")
            appendLine()
            appendLine("💬 سؤال‌تان درباره $topic را در کامنت‌ها بنویسید.")
            appendLine()
            appendLine("#${hashtag(topic)} #آموزش_${hashtag(topic)} #${hashtag(niche)}")
            appendLine("کلیدواژه‌ها: $topic، $kwLine، آموزش $topic")
        }
    }

    private fun enDescription(topic: String, niche: String): String = buildString {
        appendLine("Editable description template for a video about $topic. Adjust the sections to match what the video actually covers. For a $niche channel, include examples from your own content.")
        appendLine()
        appendLine("🔥 What you'll learn:")
        appendLine("• A simple, practical intro to $topic")
        appendLine("• Step-by-step walkthrough")
        appendLine("• Common mistakes to avoid")
        appendLine("• Free resources to keep learning")
        appendLine()
        appendLine("📌 If this helped, like & subscribe for more $topic videos! 🔔")
        appendLine("#${hashtag(topic)} #${hashtag(niche)}")
    }

    /* -------------------------------- تگ‌ها ------------------------------- */

    private fun faTags(topic: String, niche: String): List<String> {
        val kw = keywords(topic).ifEmpty { listOf(topic) }
        val tags = linkedSetOf<String>()
        tags.add(topic)
        tags.add("آموزش $topic")
        tags.add("$topic برای مبتدی‌ها")
        tags.add("بهترین $topic")
        tags.add("ترفند $topic")
        tags.add("$topic پیشرفته")
        tags.add(niche)
        tags.add("آموزش ${niche}")
        for (w in kw.take(3)) {
            tags.add(w)
            tags.add("آموزش $w")
            tags.add("$w رایگان")
        }
        tags.add("آموزش یوتیوب")
        tags.add("رشد کانال")
        return tags.filter { it.length <= 30 }.take(15)
    }

    private fun enTags(topic: String, niche: String): List<String> {
        val tags = linkedSetOf<String>()
        tags.add(topic.lowercase())
        tags.add("${topic.lowercase()} tutorial")
        tags.add("${topic.lowercase()} for beginners")
        tags.add("learn ${topic.lowercase()}")
        tags.add(niche.lowercase())
        tags.add("youtube growth")
        return tags.take(12)
    }

    /* ------------------------------- اسکریپت ------------------------------ */

    private fun faScript(topic: String, niche: String, tone: AiTone, rnd: Random): List<AiSection> {
        val hook1 = "اگر می‌خواهید با $topic آشنا شوید، در این ویدیو چند روش عملی برای بررسی و تمرین آن را مرور می‌کنیم."
        val hook2 = "در این ویدیو یک مسیر پیشنهادی برای یادگیری $topic را مرحله‌به‌مرحله مرور می‌کنیم."
        val hook = if (tone == AiTone.ENTERTAINING) hook2 else pick(listOf(hook1, hook2), rnd)

        val sections = mutableListOf<AiSection>()
        sections += AiSection(
            "هوک شروع (0:00 تا 0:15)",
            hook + "\n\nنکته: اگر نتیجه نهایی قابل نمایش است، پیش‌نمایشی واقعی از آن را در ۵ ثانیه اول نشان دهید."
        )
        sections += AiSection(
            "معرفی کوتاه (0:15 تا 0:40)",
            "سلام! من [نام شما] هستم و در کانال [نام کانال] درباره $niche صحبت می‌کنیم. در این ویدیو $topic را قدم‌به‌قدم مرور می‌کنیم؛ مثال‌هایی را اضافه کنید که واقعاً در ویدیو نمایش می‌دهید. درخواست سابسکرایب را بعد از ارائه اولین نکته کاربردی مطرح کنید."
        )
        sections += AiSection(
            "بخش ۱ — چیست و چرا مهم است؟ (0:40 تا 3:00)",
            "در این بخش $topic را با یک مثال ساده تعریف کنید و توضیح دهید چرا برای مخاطب مهم است. فقط در صورت داشتن منبع معتبر، آمار مرتبط ذکر کنید؛ در غیر این صورت از ادعای عددی استفاده نکنید. این بخش را کوتاه و روشن نگه دارید."
        )
        sections += AiSection(
            "بخش ۲ — قدم‌به‌قدم اجرا (3:00 تا 7:00)",
            "سه قدم اصلی $topic را مرحله‌به‌مرحله نشان دهید:\n1) آماده‌سازی و پیش‌نیازها\n2) اجرای اصلی با نمایش عملی روی صفحه\n3) نتیجه و چک کردن درستی کار\nهر قدم را با «چرا» همراه کنید، نه فقط «چطور». روی صفحه کلیدواژه‌ها را به‌صورت متن نمایش بدهید."
        )
        sections += AiSection(
            "بخش ۳ — اشتباهات و نکات طلایی (7:00 تا 9:00)",
            "چند اشتباه رایج در $topic را توضیح دهید و برای هرکدام راه‌حل قابل بررسی ارائه کنید. فقط مواردی را مطرح کنید که بتوانید با تجربه واقعی یا منبع معتبر پشتیبانی کنید؛ از آمار تأییدنشده و ادعای انحصاری پرهیز کنید."
        )
        sections += AiSection(
            "پایان و CTA (9:00 تا 9:40)",
            "جمع‌بندی سه نکته اصلی در ۲۰ ثانیه. سپس: «اگر این ویدیو برایتان مفید بود، لایک کنید و برای ویدیوهای بعدی $niche سابسکرایب کنید». در انتها سؤال مشخص بپرسید: «شما کدام قدم را زودتر امتحان می‌کنید؟ در کامنت‌ها بنویسید.» و ویدیوی بعدی مرتبط را پیشنهاد بدهید."
        )
        return sections
    }

    private fun enScript(topic: String, niche: String): List<AiSection> = listOf(
        AiSection("Hook (0:00-0:15)", "Introduce the actual topic and the practical outcome this video can deliver. Show a real preview in the first 5 seconds, and adjust the promise to match the finished video."),
        AiSection("Intro (0:15-0:40)", "Hey, I'm [Your Name] and on [Channel] we talk about $niche. Today: $topic step by step."),
        AiSection("Part 1 — What & Why (0:40-3:00)", "Define $topic with a concrete example. Answer: why does this matter right now?"),
        AiSection("Part 2 — Step by Step (3:00-7:00)", "1) Setup 2) Main walkthrough on screen 3) Verify the result. Always explain the why."),
        AiSection("Part 3 — Mistakes (7:00-9:00)", "Three common $topic mistakes and quick fixes for each."),
        AiSection("Outro (9:00-9:40)", "Recap in 20 seconds, ask for like/subscribe, then ask one specific question and suggest the next video.")
    )

    /* -------------------------------- ایده‌ها ------------------------------ */

    private fun faIdeas(topic: String, niche: String, rnd: Random): List<AiSection> {
        val n = 5 + rnd.nextInt(7)
        val d = 14 + rnd.nextInt(45)
        val ideas = listOf(
            "آموزش کامل $topic از صفر — یک ویدیو، همه‌چیز",
            "چالش $d روزه $topic با گزارش نتیجه واقعی",
            "بررسی و مقایسه $n ابزار/روش برای $topic",
            "اشتباهات بزرگی که مبتدی‌های $topic مرتکب می‌شوند",
            "زندگی من با $topic در یک هفته (مستند شخصی)",
            "پاسخ به سؤالات رایج درباره $topic (Q&A)",
            "$topic در ۶۰ ثانیه — نسخه Shorts",
            "تحلیل یک مورد موفق در $niche و درس‌های آن",
            "روزی که با $topic همه‌چیز را خراب کردم؛ درس‌ها",
            "نقشه راه یادگیری $topic با نقاط عطف قابل اندازه‌گیری",
            "مصاحبه با یک حرفه‌ای درباره $topic",
            "پشت پرده: روند واقعی کار من در $topic"
        ).shuffled(rnd).take(10)
        return listOf(
            AiSection("ایده‌های ویدیو", ideas.mapIndexed { i, it -> "${i + 1}. $it" }.joinToString("\n")),
            AiSection("نکته اجرا", "برای هر ایده، اول مخاطب هدف و وعده اصلی را در یک جمله بنویسید. ایده‌هایی که نتوانید وعده‌شان را در یک جمله بگویید را فعلاً کنار بگذارید. از ابزار «عنوان و سئو» همین اپ برای ساخت عنوان هر ایده استفاده کنید.")
        )
    }

    private fun enIdeas(topic: String, niche: String, rnd: Random): List<AiSection> {
        val n = 5 + rnd.nextInt(7)
        val d = 14 + rnd.nextInt(45)
        return listOf(
        AiSection("Video Ideas", listOf(
            "Complete $topic guide from zero",
            "$d-day $topic challenge with real results",
            "Comparing  ${n} tools for $topic",
            "Beginner mistakes in $topic",
            "One week living with $topic (vlog/doc)",
            "Q&A: answering your $topic questions",
            "$topic in 60 seconds (Shorts)",
            "Case study: success in $niche"
        ).shuffled(rnd).mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"))
        )
    }

    /* ---------------------------- نقطه ورود اصلی --------------------------- */

    fun generate(
        tool: AiTool,
        topic: String,
        niche: String,
        tone: AiTone,
        isEnglish: Boolean,
        seed: Long = System.nanoTime()
    ): AiResult {
        val rnd = Random(seed)
        val sections: List<AiSection> = when (tool) {
            AiTool.TITLE -> {
                val titles = if (isEnglish) enTitles(topic, niche, rnd) else faTitles(topic, niche, tone, rnd)
                val scored = titles.map { it to seoScore(it, isEnglish) }.sortedByDescending { it.second }
                val list = scored.joinToString("\n") { (t, s) -> "[$s/10]  $t" }
                if (isEnglish) {
                    listOf(
                        AiSection("Suggested titles (heuristic score, not an official YouTube metric)", list),
                        AiSection("Scoring method", "This simple estimate uses title length, digits, selected descriptive words, and question structure. It does not measure YouTube ranking or predict performance. Pick a title that accurately matches the finished video.")
                    )
                } else {
                    listOf(
                        AiSection("عنوان‌های پیشنهادی (امتیاز تخمینی و غیررسمی)", list),
                        AiSection("روش امتیازدهی", "این امتیاز یک تخمین ساده بر اساس طول عنوان، عدد، واژه‌های توصیفی و ساختار پرسشی است؛ معیار رسمی یوتیوب نیست و رتبه یا بازدید را پیش‌بینی نمی‌کند. عنوانی را انتخاب کنید که با محتوای واقعی ویدیو هم‌خوان باشد.")
                    )
                }
            }
            AiTool.DESCRIPTION -> if (isEnglish)
                listOf(AiSection("Description", enDescription(topic, niche)))
            else
                listOf(AiSection("توضیحات ویدیو", faDescription(topic, niche, rnd)))
            AiTool.TAGS -> {
                val tags = if (isEnglish) enTags(topic, niche) else faTags(topic, niche)
                listOf(AiSection("تگ‌های پیشنهادی", tags.joinToString(", ")))
            }
            AiTool.SCRIPT -> if (isEnglish) enScript(topic, niche) else faScript(topic, niche, tone, rnd)
            AiTool.IDEAS -> if (isEnglish) enIdeas(topic, niche, rnd) else faIdeas(topic, niche, rnd)
        }
        return AiResult(tool = tool, topic = topic, sections = sections, engine = "offline")
    }
}
