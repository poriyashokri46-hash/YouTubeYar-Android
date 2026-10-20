package com.youtubeyar.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.youtubeyar.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.tapsell.sdk.Tapsell
import ir.tapsell.sdk.TapsellAdRequestListener
import ir.tapsell.sdk.TapsellAdRequestOptions
import ir.tapsell.sdk.TapsellAdShowListener
import ir.tapsell.sdk.TapsellShowOptions
import ir.tapsell.sdk.bannerads.TapsellBannerType
import ir.tapsell.sdk.bannerads.TapsellBannerView
import ir.tapsell.sdk.bannerads.TapsellBannerViewEventListener
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

/** Tapsell SDK 4.9.x integration using the current request/show listener API. */
@Singleton
class TapsellAdsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var initialized = false
    private var interstitialAdId: String? = null
    private var interstitialZoneId: String? = null
    private var interstitialRequestInFlight = false
    private var successfulFreeAiGenerations = 0

    private fun appKey(): String = context.getString(R.string.tapsell_app_key).trim()
    private fun bannerZone(): String = context.getString(R.string.tapsell_zone_banner).trim()
    private fun interstitialZone(): String = context.getString(R.string.tapsell_zone_interstitial).trim()
    private fun rewardedZone(): String = context.getString(R.string.tapsell_zone_rewarded).trim()

    private fun isConfigured(): Boolean = appKey().isNotBlank()
    fun isRewardedConfigured(): Boolean = isConfigured() && rewardedZone().isNotBlank()

    @Synchronized
    fun initIfNeeded() {
        if (initialized || !isConfigured()) return
        val application = context.applicationContext as? Application ?: return
        try {
            Tapsell.initialize(application, appKey())
            initialized = true
        } catch (_: Exception) {
            // An unavailable ad SDK must not prevent the rest of the app from working.
            initialized = false
        }
    }

    fun preloadInterstitial() {
        val zone = interstitialZone()
        if (!isConfigured() || zone.isBlank() || interstitialRequestInFlight || !interstitialAdId.isNullOrBlank()) return
        initIfNeeded()
        interstitialRequestInFlight = true
        try {
            Tapsell.requestAd(
                context,
                zone,
                TapsellAdRequestOptions(),
                object : TapsellAdRequestListener() {
                    override fun onAdAvailable(adId: String) {
                        interstitialAdId = adId
                        interstitialZoneId = zone
                        interstitialRequestInFlight = false
                    }

                    override fun onError(message: String) {
                        interstitialRequestInFlight = false
                        interstitialAdId = null
                        interstitialZoneId = null
                    }
                }
            )
        } catch (_: Exception) {
            interstitialRequestInFlight = false
            interstitialAdId = null
            interstitialZoneId = null
        }
    }

    fun showInterstitial(activity: Activity) {
        val adId = interstitialAdId
        val zone = interstitialZoneId
        if (!isConfigured() || adId.isNullOrBlank() || zone.isNullOrBlank()) {
            preloadInterstitial()
            return
        }
        interstitialAdId = null
        interstitialZoneId = null
        try {
            Tapsell.showAd(
                activity,
                zone,
                adId,
                TapsellShowOptions(),
                object : TapsellAdShowListener() {
                    override fun onOpened() = Unit
                    override fun onClosed() { preloadInterstitial() }
                    override fun onError(message: String) { preloadInterstitial() }
                    override fun onAdClicked() = Unit
                    override fun onRewarded(completed: Boolean) = Unit
                }
            )
        } catch (_: Exception) {
            preloadInterstitial()
        }
    }

    /**
     * Respect the user's registered ad formats without interrupting every generation:
     * show an interstitial on the 3rd free success and an instant video on the 6th.
     */
    fun recordSuccessfulAiGeneration(activity: Activity, premium: Boolean) {
        // Begin loading early so the first scheduled interstitial can actually be shown.
        if (!premium && interstitialAdId.isNullOrBlank()) preloadInterstitial()
        val adType = synchronized(this) {
            if (premium) {
                null
            } else {
                successfulFreeAiGenerations = (successfulFreeAiGenerations % 6) + 1
                when (successfulFreeAiGenerations) {
                    3 -> "interstitial"
                    6 -> "instant_video"
                    else -> null
                }
            }
        }
        when (adType) {
            "interstitial" -> showInterstitial(activity)
            "instant_video" -> showInstantVideo(activity)
        }
    }

    /** Requests and displays the registered instant-video zone when one is due. */
    fun showInstantVideo(activity: Activity) {
        val zone = context.getString(R.string.tapsell_zone_video_instant).trim()
        if (!isConfigured() || zone.isBlank() || activity.isFinishing || activity.isDestroyed) return
        initIfNeeded()
        try {
            Tapsell.requestAd(
                activity,
                zone,
                TapsellAdRequestOptions(),
                object : TapsellAdRequestListener() {
                    override fun onAdAvailable(adId: String) {
                        if (activity.isFinishing || activity.isDestroyed) return
                        try {
                            Tapsell.showAd(
                                activity,
                                zone,
                                adId,
                                TapsellShowOptions(),
                                object : TapsellAdShowListener() {
                                    override fun onOpened() = Unit
                                    override fun onClosed() = Unit
                                    override fun onError(message: String) = Unit
                                    override fun onAdClicked() = Unit
                                    override fun onRewarded(completed: Boolean) = Unit
                                }
                            )
                        } catch (_: Exception) {
                            // Ads must never interrupt the core content-generation flow.
                        }
                    }

                    override fun onError(message: String) = Unit
                }
            )
        } catch (_: Exception) {
            // Ad network or SDK failures must not interrupt content generation.
        }
    }

    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onFail: (String) -> Unit
    ) {
        if (!isConfigured()) {
            onFail("NOT_CONFIGURED")
            return
        }
        val zone = rewardedZone()
        if (zone.isBlank()) {
            onFail("REWARDED_ZONE_MISSING")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            onFail("SHOW_FAILED")
            return
        }

        initIfNeeded()
        val finished = AtomicBoolean(false)
        val rewardReceived = AtomicBoolean(false)
        val showStarted = AtomicBoolean(false)
        try {
            Tapsell.requestAd(
                activity,
                zone,
                TapsellAdRequestOptions(),
                object : TapsellAdRequestListener() {
                    override fun onAdAvailable(adId: String) {
                        if (finished.get() || !showStarted.compareAndSet(false, true)) return
                        if (activity.isFinishing || activity.isDestroyed) {
                            if (finished.compareAndSet(false, true)) onFail("SHOW_FAILED")
                            return
                        }
                        try {
                            Tapsell.showAd(
                                activity,
                                zone,
                                adId,
                                TapsellShowOptions(),
                                object : TapsellAdShowListener() {
                                    override fun onOpened() = Unit
                                    override fun onClosed() {
                                        // Some SDK versions deliver the reward callback just after close.
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            if (!rewardReceived.get() && finished.compareAndSet(false, true)) {
                                                onFail("AD_CLOSED_NO_REWARD")
                                            }
                                        }, 350L)
                                    }
                                    override fun onError(message: String) {
                                        if (finished.compareAndSet(false, true)) onFail("SHOW_FAILED")
                                    }
                                    override fun onAdClicked() = Unit
                                    override fun onRewarded(isRewarded: Boolean) {
                                        if (isRewarded) rewardReceived.set(true)
                                        if (!finished.compareAndSet(false, true)) return
                                        if (isRewarded) onReward() else onFail("NO_REWARD")
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            if (finished.compareAndSet(false, true)) onFail(e.message ?: "SHOW_FAILED")
                        }
                    }

                    override fun onError(message: String) {
                        if (finished.compareAndSet(false, true)) onFail("AD_ERROR")
                    }
                }
            )
        } catch (error: Exception) {
            if (finished.compareAndSet(false, true)) onFail(error.message ?: "AD_ERROR")
        }
    }
}

@Composable
fun TapsellBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val appKey = stringResource(R.string.tapsell_app_key).trim()
    val zone = stringResource(R.string.tapsell_zone_banner).trim()
    if (activity == null || appKey.isBlank() || zone.isBlank()) return

    val bannerView = remember(activity, zone) {
        LayoutInflater.from(activity).inflate(R.layout.tapsell_banner, null, false) as TapsellBannerView
    }
    DisposableEffect(bannerView, appKey, zone) {
        try {
            val application = activity.application
            Tapsell.initialize(application, appKey)
            bannerView.setEventListener(object : TapsellBannerViewEventListener {
                override fun onNoAdAvailable() { bannerView.visibility = View.GONE }
                override fun onNoNetwork() { bannerView.visibility = View.GONE }
                override fun onError(errorMessage: String) { bannerView.visibility = View.GONE }
                override fun onRequestFilled() { bannerView.visibility = View.VISIBLE }
                override fun onHideBannerView() { bannerView.visibility = View.GONE }
                override fun onAdClicked() = Unit
            })
            bannerView.loadAd(activity, zone, TapsellBannerType.BANNER_320x50)
        } catch (_: Exception) {
            bannerView.visibility = View.GONE
        }
        onDispose { runCatching { bannerView.destroy() } }
    }
    AndroidView(
        factory = { bannerView },
        modifier = modifier.fillMaxWidth().height(50.dp)
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
