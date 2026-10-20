package com.youtubeyar.app.ads

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.youtubeyar.app.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.tapsell.sdk.Tapsell
import ir.tapsell.sdk.TapsellAd
import ir.tapsell.sdk.TapsellAdRequestConfiguration
import ir.tapsell.sdk.TapsellAdStateListener
import ir.tapsell.sdk.TapsellError
import ir.tapsell.sdk.TapsellShowOptions
import ir.tapsell.sdk.bannerads.BannerSize
import ir.tapsell.sdk.bannerads.TapsellBannerAd
import ir.tapsell.sdk.bannerads.TapsellBannerAdListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیریت تبلیغات تپسل (بنر + میان‌پرده + جایزه‌ای).
 *
 * ⚠️ راه‌اندازی (خیلی مهم):
 * کلیدهای واقعی را در app/src/main/res/values/strings.xml جای‌گذاری کنید:
 *   tapsell_app_key            → کلید اپلیکیشن از پنل تپسل (tapsell.ir)
 *   tapsell_zone_banner        → زون بنر
 *   tapsell_zone_interstitial  → زون میان‌پرده
 *   tapsell_zone_rewarded      → زون جایزه‌ای
 * تا وقتی کلیدها خالی باشند، تبلیغات کاملاً غیرفعال است و اپ بدون خطا کار می‌کند.
 *
 * اگر نسخه SDK تپسل که Gradle دانلود می‌کند امضای متدها را کمی تغییر داده باشد،
 * فقط همین فایل را مطابق مستندات رسمی docs.tapsell.ir به‌روز کنید.
 */
@Singleton
class TapsellAdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {

    private var initialized = false
    private var interstitialReady: TapsellAd? = null
    private val _rewardGranted = MutableStateFlow(false)
    val rewardGranted: StateFlow<Boolean> = _rewardGranted

    companion object {
        // کلیدها از strings.xml خوانده می‌شوند (بخش بالای فایل values/strings.xml)
        fun appKey(ctx: Context) = ctx.getString(com.youtubeyar.app.R.string.tapsell_app_key)
        fun bannerZone(ctx: Context) = ctx.getString(com.youtubeyar.app.R.string.tapsell_zone_banner)
        fun interstitialZone(ctx: Context) = ctx.getString(com.youtubeyar.app.R.string.tapsell_zone_interstitial)
        fun rewardedZone(ctx: Context) = ctx.getString(com.youtubeyar.app.R.string.tapsell_zone_rewarded)
    }

    private fun isConfigured(): Boolean = appKey(context).isNotBlank()

    fun isRewardedConfigured(): Boolean = isConfigured() && rewardedZone(context).isNotBlank()

    fun initIfNeeded() {
        if (initialized || !isConfigured()) return
        try {
            Tapsell.initialize(context, appKey(context))
            initialized = true
        } catch (_: Exception) {
        }
    }

    /** آیا تبلیغ نشان داده شود؟ (پرمیوم‌ها تبلیغ نمی‌بینند) */
    fun adsEnabled(premiumActive: Boolean): Boolean = !premiumActive && isConfigured()

    /* ------------------------------ میان‌پرده ------------------------------ */

    fun preloadInterstitial() {
        if (!isConfigured() || interstitialZone(context).isBlank()) return
        initIfNeeded()
        try {
            val config = TapsellAdRequestConfiguration(context, interstitialZone(context))
            Tapsell.requestAd(config, object : TapsellAdStateListener {
                override fun onAdRequested() {}
                override fun onAdFilled(ad: TapsellAd) {
                    interstitialReady = ad
                }
                override fun onAdError(error: TapsellError) {}
                override fun onAdNotAvailable() {}
                override fun onAdShowed(ad: TapsellAd) {}
                override fun onAdOpened(ad: TapsellAd) {}
                override fun onAdClosed(ad: TapsellAd) {
                    interstitialReady = null
                    preloadInterstitial() // بارگذاری بعدی
                }
                override fun onAdRewarded(ad: TapsellAd, rewards: Map<String, String>) {}
            })
        } catch (_: Exception) {
        }
    }

    fun showInterstitial(activity: Activity) {
        if (!isConfigured() || interstitialZone(context).isBlank()) return
        val ad = interstitialReady ?: run {
            preloadInterstitial()
            return
        }
        interstitialReady = null
        try {
            ad.show(activity, TapsellShowOptions())
        } catch (_: Exception) {
            preloadInterstitial()
        }
    }

    /* ------------------------------ جایزه‌ای ------------------------------ */

    /**
     * پخش ویدیوی جایزه‌ای: بعد از تماشای کامل، آنلاک ۲۴ ساعته ویژه فعال می‌شود.
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onFail: (String) -> Unit) {
        if (!isConfigured()) {
            onFail("NOT_CONFIGURED")
            return
        }
        if (rewardedZone(context).isBlank()) {
            onFail("REWARDED_ZONE_MISSING")
            return
        }
        initIfNeeded()
        var completed = false
        var adShown = false
        fun failOnce(message: String) {
            if (!completed) {
                completed = true
                onFail(message)
            }
        }
        try {
            val config = TapsellAdRequestConfiguration(context, rewardedZone(context))
            Tapsell.requestAd(config, object : TapsellAdStateListener {
                override fun onAdRequested() {}
                override fun onAdFilled(ad: TapsellAd) {
                    if (completed || adShown) return
                    adShown = true
                    try {
                        ad.show(activity, TapsellShowOptions())
                    } catch (e: Exception) {
                        failOnce(e.message ?: "SHOW_FAILED")
                    }
                }
                override fun onAdError(error: TapsellError) = failOnce(error.message ?: "AD_ERROR")
                override fun onAdNotAvailable() = failOnce("NO_AD")
                override fun onAdShowed(ad: TapsellAd) {}
                override fun onAdOpened(ad: TapsellAd) {}
                override fun onAdClosed(ad: TapsellAd) {
                    if (!completed) failOnce("AD_CLOSED_NO_REWARD")
                }
                override fun onAdRewarded(ad: TapsellAd, rewards: Map<String, String>) {
                    if (completed) return
                    completed = true
                    _rewardGranted.value = true
                    onReward()
                }
            })
        } catch (e: Exception) {
            failOnce(e.message ?: "REQUEST_FAILED")
        }
    }
}

/**
 * بنر تپسل به‌صورت Composable — در انتهای صفحات اصلی استفاده می‌شود.
 * اگر زون بنر خالی باشد، هیچ ویویی رندر نمی‌شود.
 */
@Composable
fun TapsellBanner(
    premiumActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val zone = TapsellAdsManager.bannerZone(context)
    val appKey = TapsellAdsManager.appKey(context)
    if (premiumActive || zone.isBlank() || appKey.isBlank()) return

    val container = remember { FrameLayout(context) }
    DisposableEffect(Unit) {
        var ad: TapsellBannerAd? = null
        try {
            Tapsell.initialize(context, appKey)
            ad = TapsellBannerAd(context, zone, BannerSize.BANNER_320x50, object : TapsellBannerAdListener {
                override fun onAdRequested() {}
                override fun onAdFilled(view: android.view.ViewGroup) {
                    container.removeAllViews()
                    container.addView(
                        view,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }
                override fun onAdError(message: String) {}
                override fun onAdNoBid() {}
            })
        } catch (_: Exception) {
        }
        onDispose { ad = null }
    }
    AndroidView(
        factory = { container },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
