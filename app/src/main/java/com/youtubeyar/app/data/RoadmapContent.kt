package com.youtubeyar.app.data

/**
 * محتوای نقشه راه مانیتایز — ۴ فاز، هر فاز ۱۰ وظیفه + ۶ وظیفه روزانه + ۶ مایل‌استون
 * محتوا به دو زبان فارسی و انگلیسی است؛ بر اساس زبان سیستم یکی انتخاب می‌شود.
 */
object RoadmapContent {

    data class Task(val titleFa: String, val titleEn: String, val descFa: String, val descEn: String)

    data class Phase(
        val index: Int,
        val nameFa: String, val nameEn: String,
        val rangeFa: String, val rangeEn: String,
        val tasks: List<Task>
    )

    val phases: List<Phase> = listOf(
        Phase(
            0,
            "پایه‌گذاری", "Foundation",
            "روز ۱ تا ۷", "Day 1-7",
            listOf(
                Task("ساخت حساب گوگل و کانال برند", "Create Google & Brand Account",
                    "یک Brand Account بسازید تا بعداً بتوانید مدیر جدید اضافه کنید و کانال از حساب شخصی شما جدا بماند.",
                    "Create a Brand Account so you can add managers later and keep the channel separate from your personal account."),
                Task("انتخاب نام و هویت کانال", "Pick Channel Name & Identity",
                    "نامی کوتاه، قابل تلفظ و مرتبط با موضوع انتخاب کنید. با ابزار ایده‌پرداز اپ چند گزینه بسازید.",
                    "Choose a short, pronounceable name related to your topic. Use the app idea tool to brainstorm options."),
                Task("طراحی پروفایل و بنر کانال", "Design Profile & Banner",
                    "بنر با سایز ۲۵۶۰×۱۴۴۰ و فضای امن در مرکز. لوگو ساده و خوانا در سایز کوچک.",
                    "Banner 2560x1440 with a safe center area. Logo simple and readable at small sizes."),
                Task("نوشتن توضیحات کانال با کلمات کلیدی", "Write Keyword-Rich Channel Description",
                    "دو خط اول در جستجو نمایش داده می‌شوند؛ موضوع و ارزش کانال را همان بالا بنویسید.",
                    "The first two lines show in search; state your topic and value up front."),
                Task("تأیید هویت کانال با شماره موبایل", "Verify Channel with Phone",
                    "از m.youtube.com/verify تأیید کنید تا ویدیوی طولانی‌تر، کلیک‌سنج سفارشی و استریم فعال شود.",
                    "Verify at m.youtube.com/verify to unlock longer videos, custom thumbnails and streaming."),
                Task("فعال‌سازی تأیید دومرحله‌ای گوگل", "Enable Google 2FA",
                    "الزام رسمی برنامه همکاری یوتیوب است؛ همین امروز فعالش کنید.",
                    "An official YouTube Partner Program requirement; enable it today."),
                Task("انتشار اولین ویدیو", "Publish Your First Video",
                    "کیفیت مهم‌تر از کمال‌گرایی نیست؛ اولین ویدیو را همین امروز ضبط و منتشر کنید.",
                    "Done beats perfect; record and publish your first video today."),
                Task("تنظیم برنامه انتشار ثابت هفتگی", "Set a Weekly Upload Schedule",
                    "حداقل ۱ ویدیو در هفته با روز و ساعت ثابت؛ نظم را الگوریتم دوست دارد.",
                    "At least 1 video per week on a fixed day and time; the algorithm rewards consistency."),
                Task("مطالعه درس‌های پایه در آکادمی", "Read Academy Basics",
                    "درس‌های «راه‌اندازی کانال» و «انتخاب نیش» را کامل بخوانید.",
                    "Read the 'Channel Setup' and 'Choosing a Niche' lessons completely."),
                Task("ثبت آمار فعلی در ردیاب پیشرفت", "Log Current Stats in Tracker",
                    "تا نمودار پیشرفت شما از امروز شروع شود، سابسکرایبر و ساعت واچ فعلی را ثبت کنید.",
                    "Log your current subscribers and watch hours so your progress chart starts today.")
            )
        ),
        Phase(
            1,
            "تثبیت و نظم", "Consistency",
            "هفته ۲ تا ۴", "Week 2-4",
            listOf(
                Task("انتشار حداقل ۴ ویدیو در این ماه", "Publish 4+ Videos This Month",
                    "با کیفیت پایدار؛ هر ویدیو حداقل بهتر از قبلی باشد.",
                    "With steady quality; make each video slightly better than the last."),
                Task("ساخت تریلر کانال", "Create Channel Trailer",
                    "ویدیوی ۳۰ تا ۶۰ ثانیه‌ای که به بازدیدکننده جدید بگوید کانال درباره چیست و چرا سابسکرایب کند.",
                    "A 30-60 second video telling new visitors what the channel is about and why to subscribe."),
                Task("سئوی کامل هر ویدیو با ابزار AI", "Full SEO per Video with AI",
                    "عنوان، توضیحات و تگ هر ویدیو را از ابزار هوش مصنوعی اپ بگیرید و شخصی‌سازی کنید.",
                    "Get title, description and tags for each video from the app's AI tools, then personalize."),
                Task("قالب ثابت تامبنیل طراحی کنید", "Design a Thumbnail Template",
                    "رنگ، فونت و ترکیب یکسان تا کانال در پیشنهادها قابل شناسایی شود.",
                    "Consistent colors, font and layout so your channel is recognizable in suggestions."),
                Task("پاسخ به همه کامنت‌ها", "Reply to All Comments",
                    "تعامل اولیه، مهم‌ترین سیگنال رشد کانال‌های تازه است.",
                    "Early engagement is the strongest growth signal for new channels."),
                Task("تحلیل ۳ کانال موفق هم‌موضوع", "Analyze 3 Successful Competitors",
                    "از تب «تحلیل کانال» اپ استفاده کنید: عنوان‌ها، تناوب انتشار و بازدید میانگینشان را بررسی کنید.",
                    "Use the app's Channel Analysis tab: study titles, cadence and average views."),
                Task("ساخت ۳ پلی‌لیست موضوعی", "Create 3 Topic Playlists",
                    "پلی‌لیست‌ها با عنوان کلمه‌کلیدی‌دار در جستجو هم ظاهر می‌شوند.",
                    "Keyword-titled playlists also rank in search."),
                Task("افزودن کارت و صفحه پایانی", "Add Cards & End Screens",
                    "به همه ویدیوها؛ مسیر تماشای بیشتر و سابسکرایب را هموار کنید.",
                    "On every video; pave the path to more watch time and subscriptions."),
                Task("انتشار اولین Shorts", "Publish Your First Short",
                    "Shorts سریع‌ترین راه جذب سابسکرایبر برای کانال‌های نو است.",
                    "Shorts are the fastest subscriber source for brand-new channels."),
                Task("حضور فعال در انجمن‌های هم‌موضوع", "Be Active in Niche Communities",
                    "ارزش واقعی بدهید و لینک بدهید، نه اسپم؛ ۱۰ به ۱ قانون طلایی است.",
                    "Give real value before links, never spam; the 10:1 rule is golden.")
            )
        ),
        Phase(
            2,
            "رشد و کشف‌پذیری", "Growth & Discovery",
            "ماه ۲ تا ۳", "Month 2-3",
            listOf(
                Task("تحلیل ریتنشن و بازدید هر ویدیو", "Review Retention per Video",
                    "در Studio ببینید مخاطب کجا رها می‌کند و علتش را رفع کنید.",
                    "Find where viewers drop off in Studio and fix that moment."),
                Task("بازطراحی ۵۰ ثانیه اول (هوک)", "Rebuild Your First 50 Seconds",
                    "بیشترین ریزش در شروع ویدیوست؛ با ابزار اسکریپت‌نویس اپ هوک قوی بسازید.",
                    "Most churn happens at the start; use the app's Script tool to craft a strong hook."),
                Task("همکاری با ۱ کانال هم‌اندازه", "Collab with a Similar-Size Channel",
                    "تبادل مخاطب، سریع‌ترین رشد ارگانیک بعد از جستجو است.",
                    "Audience exchange is the fastest organic growth after search."),
                Task("رسیدن به ۱۰۰ سابسکرایبر", "Reach 100 Subscribers",
                    "برای نشانی دستی سفارشی و اعتبار کانال.",
                    "Unlocks a custom handle and channel credibility."),
                Task("انتشار هفته‌ای ۱ ویدیو + ۳ Shorts", "Weekly: 1 Video + 3 Shorts",
                    "ترکیب محتوای بلند و کوتاه، موتور رشد پایدار است.",
                    "Long + short content mix is the sustainable growth engine."),
                Task("آزمون A/B عنوان و تامبنیل", "A/B Test Titles & Thumbnails",
                    "نسخه‌های مختلف بسازید و CTR را در Studio مقایسه کنید.",
                    "Create variants and compare CTR inside Studio."),
                Task("ساخت سری ویدیویی مرحله‌به‌مرحله", "Build a Step-by-Step Series",
                    "سری‌ها واچ‌تایم تجمعی می‌سازند؛ برای هدف ۴۰۰۰ ساعت عالی‌اند.",
                    "Series build cumulative watch time; perfect for the 4,000-hour goal."),
                Task("فعال‌سازی پست گروه و نظرسنجی", "Use Community Posts & Polls",
                    "با مخاطب فعلی در ارتباط بمانید تا الگوریتم شما را فراموش نکند.",
                    "Stay connected with your audience so the algorithm keeps suggesting you."),
                Task("دو برابر کردن بهترین منبع ترافیک", "Double Down on Best Traffic Source",
                    "در Analytics ببینید بازدید از کجاست و همان مسیر را تقویت کنید.",
                    "Check Analytics for your top traffic source and reinforce it."),
                Task("رسیدن به ۴۰٪ مسیر ۱۰۰۰ سابسکرایبر", "Hit 40% Toward 1,000 Subs",
                    "۴۰۰ سابسکرایبر یعنی شما روی مسیر درست هستید.",
                    "400 subscribers means you are on the right track.")
            )
        ),
        Phase(
            3,
            "آمادگی مانیتایز", "Monetization Ready",
            "تا رسیدن به مانیتایز", "Until Monetization",
            listOf(
                Task("بررسی شرایط YPP", "Check YPP Requirements",
                    "۱۰۰۰ سابسکرایبر + ۴۰۰۰ ساعت واچ‌تایم در ۱۲ ماه اخیر (یا ۱۰ میلیون بازدید Shorts در ۹۰ روز).",
                    "1,000 subs + 4,000 public watch hours in 12 months (or 10M Shorts views in 90 days)."),
                Task("پاکسازی محتوای تکراری یا کپی", "Remove Reused/Duplicated Content",
                    "محتوای تکراری و بدون ارزش افزوده مانیتایز نمی‌شود؛ همه ویدیوها را بازبینی کنید.",
                    "Reused content without added value won't monetize; audit all videos."),
                Task("صفر کردن استرایک‌ها و هشدارها", "Zero Strikes & Warnings",
                    "هر استرایک فعال، درخواست مانیتایز را رد می‌کند.",
                    "Any active strike will reject your monetization application."),
                Task("مطالعه کامل قوانین راهنمای یوتیوب", "Study YouTube Policies Fully",
                    "Community Guidelines + AdSense Program Policies را کامل بخوانید.",
                    "Read Community Guidelines and AdSense policies completely."),
                Task("اطلاعات صحیح AdSense و مالیاتی", "Prepare AdSense & Tax Info",
                    "حساب AdSense آماده با اطلاعات درست قبل از درخواست بسازید.",
                    "Have a ready AdSense account with correct details before applying."),
                Task("ارسال درخواست عضویت در YPP", "Apply to the Partner Program",
                    "از بخش «مانیتایز» در YouTube Studio درخواست بدهید.",
                    "Apply from the 'Earn' section in YouTube Studio."),
                Task("ادامه انتشار منظم حین بررسی", "Keep Publishing During Review",
                    "بررسی معمولاً تا ۱ ماه طول می‌کشد؛ هیجان را تبدیل به محتوا کنید.",
                    "Review can take up to a month; turn the wait into content."),
                Task("ساخت ویدیوی درآمدزای اول", "Create Your First Monetizable Video",
                    "ویدیویی که هم ارزش آموزشی دارد و هم زمان تماشای بالا.",
                    "A video with real teaching value and strong watch time."),
                Task("بررسی نهایی با ماشین‌حساب اپ", "Final Check with the App Calculator",
                    "در داشبورد، تخمین روز رسیدن به هر دو شرط را ببینید.",
                    "See your ETA for both requirements on the Dashboard."),
                Task("آماده‌سازی برای درآمد بعد از مانیتایز", "Plan Post-Monetization Income",
                    "اسپانسر، همکاری در فروش و محتوای ممتاز را از حالا طراحی کنید.",
                    "Design sponsorships, affiliates and premium content from now.")
            )
        )
    )

    data class DailyTask(val titleFa: String, val titleEn: String)

    val dailyTasks: List<DailyTask> = listOf(
        DailyTask("بررسی آمار دیروز در YouTube Studio", "Check yesterday's stats in YouTube Studio"),
        DailyTask("پاسخ به تمام کامنت‌های جدید", "Reply to all new comments"),
        DailyTask("ثبت ۳ ایده ویدیوی جدید", "Log 3 new video ideas"),
        DailyTask("۱۵ دقیقه تحقیق کلمات کلیدی", "15 minutes of keyword research"),
        DailyTask("تعامل با ۳ کانال هم‌موضوع", "Engage with 3 channels in your niche"),
        DailyTask("ثبت امروز در ردیاب پیشرفت", "Log today in the progress tracker")
    )

    data class Milestone(val titleFa: String, val titleEn: String)

    val milestones: List<Milestone> = listOf(
        Milestone("ایجاد کانال", "Channel created"),
        Milestone("انتشار ۱۰ ویدیو", "10 videos published"),
        Milestone("۱۰۰ سابسکرایبر اول", "First 100 subscribers"),
        Milestone("فعال شدن تب گروه", "Community tab unlocked"),
        Milestone("۵۰۰ سابسکرایبر", "500 subscribers"),
        Milestone("ارسال درخواست مانیتایز", "Monetization applied")
    )

    /** سید کردن اولیه وظایف نقشه راه در دیتابیس */
    fun seedTasks(languageIsEnglish: Boolean): List<ChecklistTask> {
        val tasks = mutableListOf<ChecklistTask>()
        phases.forEach { phase ->
            phase.tasks.forEachIndexed { i, t ->
                tasks += ChecklistTask(
                    phase = phase.index,
                    orderIndex = i,
                    title = if (languageIsEnglish) t.titleEn else t.titleFa,
                    description = if (languageIsEnglish) t.descEn else t.descFa
                )
            }
        }
        return tasks
    }
}
