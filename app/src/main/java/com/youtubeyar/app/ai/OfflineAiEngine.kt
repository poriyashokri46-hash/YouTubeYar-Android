package com.youtubeyar.app.ai

import kotlin.random.Random

/**
 * موتور هوش مصنوعی آفلاین — کاملاً روی دستگاه بدون اینترنت و بدون کلید API کار می‌کند.
 * با فرمول‌های سئوی یوتیوب، بانک الگوهای فارسی/انگلیسی و تحلیل کلمات کلیدی موضوع،
 * عنوان، توضیحات، تگ، اسکریپت و ایده ویدیو تولید می‌کند.
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

    private val powerWordsFa = listOf(
        "راز", "رایگان", "سریع", "شگفت‌انگیز", "حرفه‌ای", "نابود", "هیچ‌کس", "موفق", "رشد",
        "بهترین", "آسان", "واقعی", "قطعی", "محرمانه", "قطعی"
    )

    /* ------------------------------- عنوان‌ها ------------------------------ */

    private fun faTitles(topic: String, niche: String, tone: AiTone, rnd: Random): List<String> {
        val n = 5 + rnd.nextInt(7)          // 5..11
        val d = 14 + rnd.nextInt(45)        // 14..58 روز
        val kw = keywords(topic).firstOrNull() ?: topic
        val hypePrefixes = listOf("شوکه‌کننده! ", "سرانجام: ", "باورنکردنی! ")
        val proPrefixes = listOf("راهنمای جامع ", "مستند ", "بررسی کامل ")
        val funPrefixes = listOf("😂 ", "🔥 ", "😱 ")

        val base = listOf(
            "$n ترفند $topic که هیچ‌کس به شما نمی‌گوید",
            "چرا $topic شما جواب نمی‌دهد؟ (راه‌حل واقعی)",
            "چطور در $d روز روی $topic تسلط پیدا کنیم؟",
            "$n اشتباه مرگبار در $topic که ۹۰٪ افراد می‌کنند",
            "راز $topic که فقط حرفه‌ای‌ها می‌دانند!",
            "$topic: روش کلاسیک یا مدرن؟ انتخاب درست",
            "من $d روز فقط $topic انجام دادم؛ نتیجه شگفت‌زده‌ام کرد",
            "آموزش کامل $topic از صفر تا صد (مخصوص مبتدی‌ها)",
            "بهترین روش‌های $topic در سال جدید (آپدیت کامل)",
            "$n ابزار رایگان که $topic را ۱۰ برابر بهتر می‌کند",
            "همه‌چیز درباره $kw در یک ویدیو — $niche",
            "از صفر تا حرفه‌ای در $topic: نقشه راه واقعی"
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
        val d = 14 + rnd.nextInt(45)
        return listOf(
            "$n $topic secrets nobody tells you",
            "Why your $topic isn't working (the real fix)",
            "Master $topic in $d days — step by step",
            "$n deadly $topic mistakes beginners make",
            "The hidden truth about $topic",
            "$topic: classic vs modern, which one wins?",
            "I did $topic for $d days — here's what happened",
            "The complete $topic guide for beginners ($niche)"
        ).shuffled(rnd)
    }

    /** امتیاز سئوی یک عنوان از ۰ تا ۱۰ */
    fun seoScore(title: String): Int {
        if (title.isBlank()) return 1
        var score = 4
        val len = title.length
        if (len in 25..60) score += 2 else if (len in 18..70) score += 1 else if (len > 75) score -= 2
        if (title.any { it.isDigit() }) score += 2
        if (powerWordsFa.any { title.contains(it) }) score += 2
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
            "نکات حرفه‌ای که زمان شما را نصف می‌کند",
            "منابع رایگان برای ادامه مسیر یادگیری"
        ).shuffled(rnd).take(4)

        return buildString {
            appendLine("در این ویدیو همه‌چیز درباره $topic را می‌آموزید؛ از پایه‌ها تا نکات حرفه‌ای. اگر در مسیر $niche هستید، این ویدیو دقیقاً برای شما ساخته شده است.")
            appendLine()
            appendLine("🔥 در این ویدیو یاد می‌گیرید:")
            bullets.forEach { appendLine("• $it") }
            appendLine()
            appendLine("⏱ زمان‌بندی ویدیو:")
            appendLine("0:00 شروع و معرفی موضوع")
            appendLine("1:10 توضیح مفاهیم اصلی")
            appendLine("4:30 اجرای عملی و قدم‌به‌قدم")
            appendLine("8:00 اشتباهات رایج")
            appendLine("10:15 جمع‌بندی و قدم بعدی")
            appendLine()
            appendLine("📌 اگر این ویدیو برای شما مفید بود، لایک کنید و کانال را سابسکرایب کنید تا ویدیوهای بعدی $topic را از دست ندهید! 🔔")
            appendLine()
            appendLine("💬 سؤال شما درباره $topic چیست؟ در کامنت‌ها بنویسید؛ به همه پاسخ می‌دهم.")
            appendLine()
            appendLine("#$topic #آموزش_$topic #$niche")
            appendLine("کلیدواژه‌ها: $topic، $kwLine، آموزش $topic")
        }
    }

    private fun enDescription(topic: String, niche: String): String = buildString {
        appendLine("In this video you'll learn everything about $topic — from basics to pro tips. If you're into $niche, this one is for you.")
        appendLine()
        appendLine("🔥 What you'll learn:")
        appendLine("• A simple, practical intro to $topic")
        appendLine("• Step-by-step walkthrough")
        appendLine("• Common mistakes to avoid")
        appendLine("• Free resources to keep learning")
        appendLine()
        appendLine("📌 If this helped, like & subscribe for more $topic videos! 🔔")
        appendLine("#$topic #$niche")
    }

    /* -------------------------------- تگ‌ها ------------------------------- */

    private fun faTags(topic: String, niche: String): List<String> {
        val kw = keywords(topic).ifEmpty { listOf(topic) }
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
        val tags = linkedSetOf<String>()
        tags.add(topic)
        tags.add("آموزش $topic")
        tags.add("$topic برای مبتدی‌ها")
        tags.add("بهترین $topic")
        tags.add("ترفند $topic")
        tags.add("$topic پیشرفته")
        tags.add("$topic $year")
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
        val hook1 = "اگر تا حالا برای $topic زحمت کشیده‌اید ولی نتیجه نگرفته‌اید، تا پایان این ویدیو همراه من بمانید؛ چون دقیقاً همان چیزی را نشان می‌دهم که کسی به شما نمی‌گوید."
        val hook2 = "این ویدیو را می‌توانید دو حاله داشته باشید: یا ادامه بدهید و $topic را برای همیشه یاد بگیرید، یا ببندید و همان فرصت را از دست بدهید!"
        val hook = if (tone == AiTone.ENTERTAINING) hook2 else pick(listOf(hook1, hook2), rnd)

        val sections = mutableListOf<AiSection>()
        sections += AiSection(
            "هوک شروع (0:00 تا 0:15)",
            hook + "\n\nنکته: نتیجه نهایی را در ۵ ثانیه اول نشان دهید تا مخاطب بداند در پایان چه چیز منتظرش است."
        )
        sections += AiSection(
            "معرفی کوتاه (0:15 تا 0:40)",
            "سلام! من [نام شما] هستم و در کانال [نام کانال] درباره $niche حرف می‌زنیم. در این ویدیو قدم‌به‌قدم $topic را با هم جلو می‌بریم؛ بدون حاشیه و با مثال‌های واقعی. (دقت کنید: درخواست سابسکرایب را بعد از اولین ارزش واقعی ویدیو بگویید، نه اینجا!)"
        )
        sections += AiSection(
            "بخش ۱ — چیست و چرا مهم است؟ (0:40 تا 3:00)",
            "در این بخش $topic را با یک مثال ساده و ملموس تعریف کنید. به این سؤال پاسخ بدهید که «چرا الان برای مخاطب مهم است؟». یک عدد یا آمار جالب درباره $topic بگویید تا باورپذیری بالا برود. مهلت این بخش: حداکثر ۲ دقیقه؛ از حاشیه بپرهیزید."
        )
        sections += AiSection(
            "بخش ۲ — قدم‌به‌قدم اجرا (3:00 تا 7:00)",
            "سه قدم اصلی $topic را مرحله‌به‌مرحله نشان دهید:\n1) آماده‌سازی و پیش‌نیازها\n2) اجرای اصلی با نمایش عملی روی صفحه\n3) نتیجه و چک کردن درستی کار\nهر قدم را با «چرا» همراه کنید، نه فقط «چطور». روی صفحه کلیدواژه‌ها را به‌صورت متن نمایش بدهید."
        )
        sections += AiSection(
            "بخش ۳ — اشتباهات و نکات طلایی (7:00 تا 9:00)",
            "سه اشتباه رایجی که ۹۰٪ افراد در $topic می‌کنند را بگویید و راه‌حل هرکدام را سریع توضیح دهید. یک نکته مخصوص به بینندگان واقعی اضافه کنید که در هیچ‌جای دیگر پیدایش نمی‌کنند؛ این کار سهم ویدیو را در کامنت‌ها بالا می‌برد."
        )
        sections += AiSection(
            "پایان و CTA (9:00 تا 9:40)",
            "جمع‌بندی سه نکته اصلی در ۲۰ ثانیه. سپس: «اگر این ویدیو برایتان مفید بود، لایک کنید و برای ویدیوهای بعدی $niche سابسکرایب کنید». در انتها سؤال مشخص بپرسید: «شما کدام قدم را زودتر امتحان می‌کنید؟ در کامنت‌ها بنویسید.» و ویدیوی بعدی مرتبط را پیشنهاد بدهید."
        )
        return sections
    }

    private fun enScript(topic: String, niche: String): List<AiSection> = listOf(
        AiSection("Hook (0:00-0:15)", "If you've tried $topic without results, watch till the end — I'll show you exactly what nobody tells you. Show the final result in the first 5 seconds."),
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
            "نقشه راه $topic برای سال جدید — آپدیت‌شده",
            "مصاحبه با یک حرفه‌ای درباره $topic",
            "پشت پرده: روند واقعی کار من در $topic"
        ).shuffled(rnd).take(10)
        return listOf(
            AiSection("ایده‌های ویدیو", ideas.mapIndexed { i, it -> "${i + 1}. $it" }.joinToString("\n")),
            AiSection("نکته اجرا", "برای هر ایده، اول مخاطب هدف و وعده اصلی را در یک جمله بنویسید. ایده‌هایی که نتوانید وعده‌شان را در یک جمله بگویید را فعلاً کنار بگذارید. از ابزار «عنوان و سئو» همین اپ برای ساخت عنوان هر ایده استفاده کنید.")
        )
    }

    private fun enIdeas(topic: String, niche: String, rnd: Random): List<AiSection> = listOf(
        AiSection("Video Ideas", listOf(
            "Complete $topic guide from zero",
            "$d-day $topic challenge with real results",
            "Comparing $n tools for $topic",
            "Beginner mistakes in $topic",
            "One week living with $topic (vlog/doc)",
            "Q&A: answering your $topic questions",
            "$topic in 60 seconds (Shorts)",
            "Case study: success in $niche"
        ).shuffled(rnd).mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n"))
    )

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
                val scored = titles.map { it to seoScore(it) }.sortedByDescending { it.second }
                val list = scored.joinToString("\n") { (t, s) -> "[$s/10]  $t" }
                listOf(
                    AiSection("عنوان‌های پیشنهادی (مرتب بر اساس امتیاز سئو)", list),
                    AiSection("تحلیل سئو", "امتیاز بر اساس: طول بهینه (۲۵ تا ۶۰ کاراکتر)، داشتن عدد، کلمات قدرتمند و ساختار سؤالی. عنوان با امتیاز ۸ به بالا را انتخاب کنید و حتماً با وعده ویدیو هم‌خوان باشد.")
                )
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
