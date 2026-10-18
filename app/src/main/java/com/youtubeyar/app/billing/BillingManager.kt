package com.youtubeyar.app.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import com.youtubeyar.app.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیریت خرید اشتراک ویژه از کافه‌بازار و مایکت.
 * هر دو فروشگاه پروتکل IAB v3 را پشتیبانی می‌کنند؛ پکیج فروشگاه در زمان اجرا تشخیص داده می‌شود.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    companion object {
        const val PURCHASE_REQUEST_CODE = 7001
        const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
        const val MYKET_PACKAGE = "ir.mservices.market"
        const val SKU_MONTH = "premium_month"
        const val SKU_YEAR = "premium_year"
        val ALL_SKUS = listOf(SKU_MONTH, SKU_YEAR)
        const val TYPE_SUBS = "subs"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var pendingHelper: IabHelper? = null
    private var pendingSku: String? = null
    private var onPurchaseDone: ((Boolean, String) -> Unit)? = null

    /** تشخیص فروشگاه نصب‌شده روی گوشی کاربر */
    fun detectMarket(): String? = when {
        isInstalled(BAZAAR_PACKAGE) -> BAZAAR_PACKAGE
        isInstalled(MYKET_PACKAGE) -> MYKET_PACKAGE
        else -> null
    }

    fun marketName(market: String?): String = when (market) {
        BAZAAR_PACKAGE -> "کافه‌بازار"
        MYKET_PACKAGE -> "مایکت"
        else -> "—"
    }

    private fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0) != null
    } catch (_: Exception) {
        false
    }

    /**
     * دریافت قیمت اشتراک‌ها از فروشگاه (برای نمایش در صفحه اشتراک).
     * خروجی: نقشه sku → متن قیمت
     */
    fun loadPrices(onResult: (Map<String, String>) -> Unit) {
        val market = detectMarket()
        if (market == null) {
            onResult(emptyMap())
            return
        }
        val helper = IabHelper(context, market)
        helper.connect(object : IabHelper.Listener {
            override fun onReady(service: com.android.vending.billing.IInAppBillingService) {
                val prices = try {
                    val details = helper.getSkuDetails(ALL_SKUS)
                    details.associate { it.sku to it.price }
                } catch (_: Exception) {
                    emptyMap()
                }
                onResult(prices)
                helper.unbind()
            }

            override fun onError(message: String) {
                onResult(emptyMap())
                helper.unbind()
            }
        })
    }

    /**
     * شروع فرایند خرید اشتراک.
     * نتیجه در handlePurchaseResult (که MainActivity آن را پاس می‌دهد) دریافت می‌شود.
     */
    fun startPurchase(activity: Activity, sku: String, onResult: (Boolean, String) -> Unit) {
        val market = detectMarket()
        if (market == null) {
            onResult(false, "NO_MARKET")
            return
        }
        onPurchaseDone = onResult
        pendingSku = sku
        val helper = IabHelper(context, market)
        pendingHelper = helper
        helper.connect(object : IabHelper.Listener {
            override fun onReady(service: com.android.vending.billing.IInAppBillingService) {
                try {
                    if (!helper.isBillingSupported(TYPE_SUBS)) {
                        onResult(false, "SUBS_NOT_SUPPORTED")
                        helper.unbind()
                        pendingHelper = null
                        return
                    }
                    val payload = "ytt-" + System.currentTimeMillis()
                    val buyIntent = helper.getBuyIntent(sku, payload, TYPE_SUBS)
                    if (buyIntent == null) {
                        onResult(false, "BUY_INTENT_FAILED")
                        helper.unbind()
                        pendingHelper = null
                        return
                    }
                    activity.startIntentSenderForResult(
                        buyIntent.intentSender,
                        PURCHASE_REQUEST_CODE,
                        Intent(), 0, 0, 0
                    )
                } catch (e: Exception) {
                    onResult(false, e.message ?: "PURCHASE_ERROR")
                    helper.unbind()
                    pendingHelper = null
                }
            }

            override fun onError(message: String) {
                onResult(false, message)
                helper.unbind()
                pendingHelper = null
            }
        })
    }

    /** از MainActivity صدا زده می‌شود بعد از بسته شدن دیالوگ پرداخت */
    fun handlePurchaseResult(resultCode: Int, data: Intent?) {
        val helper = pendingHelper ?: return
        val sku = pendingSku ?: ""
        var success = false
        var message = "CANCELLED"

        val purchase = IabHelper.parsePurchaseData(data)
        if (resultCode == Activity.RESULT_OK && purchase != null &&
            purchase.packageName == context.packageName &&
            purchase.sku == sku && ALL_SKUS.contains(purchase.sku) && purchase.token.isNotBlank()) {
            try {
                helper.acknowledge(purchase.token)
            } catch (_: Exception) {
            }
            scope.launch {
                settings.setPremium(true, purchase.token)
            }
            success = true
            message = "OK"
        }

        onPurchaseDone?.invoke(success, message)
        onPurchaseDone = null
        pendingSku = null
        helper.unbind()
        pendingHelper = null
    }

    /**
     * بازیابی خریدها — اگر کاربر اشتراک فعال داشته باشد، پرمیوم روشن می‌شود.
     */
    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        val market = detectMarket()
        if (market == null) {
            onResult(false, "NO_MARKET")
            return
        }
        val helper = IabHelper(context, market)
        helper.connect(object : IabHelper.Listener {
            override fun onReady(service: com.android.vending.billing.IInAppBillingService) {
                scope.launch {
                    var found = false
                    try {
                        val subs = helper.getPurchases(TYPE_SUBS)
                        found = subs.any { p ->
                            p.packageName == context.packageName &&
                                p.token.isNotBlank() && ALL_SKUS.contains(p.sku)
                        }
                        if (found) {
                            subs.filter {
                                it.packageName == context.packageName &&
                                    it.token.isNotBlank() && ALL_SKUS.contains(it.sku)
                            }.forEach { p ->
                                runCatching { helper.acknowledge(p.token) }
                            }
                            settings.setPremium(true, subs.first {
                                it.packageName == context.packageName &&
                                    it.token.isNotBlank() && ALL_SKUS.contains(it.sku)
                            }.token)
                        }
                    } catch (_: Exception) {
                    }
                    onResult(found, if (found) "OK" else "NOT_FOUND")
                    helper.unbind()
                }
            }

            override fun onError(message: String) {
                onResult(false, message)
                helper.unbind()
            }
        })
    }
}
