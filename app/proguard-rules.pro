# قوانین ProGuard برای یوتیوب‌یار
# چون minifyEnabled=false است این فایل فقط برای انتشار بهینه‌شده آینده استفاده می‌شود.

# Tapsell
-keep class ir.tapsell.sdk.** { *; }
-dontwarn ir.tapsell.sdk.**

# In-App Billing
-keep class com.android.vending.billing.** { *; }

# Retrofit / Gson
-keep class com.youtubeyar.app.youtube.** { *; }
-keep class com.youtubeyar.app.ai.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt
-keep class dagger.hilt.** { *; }
