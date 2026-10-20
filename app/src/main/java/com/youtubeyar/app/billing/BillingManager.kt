package com.youtubeyar.app.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import com.youtubeyar.app.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدیریت اشتراک فروشگاهی. فعال‌سازی محلی فقط پس از اعتبارسنجی امضای خرید انجام می‌شود.
 * کلید عمومی فروشگاه باید در تنظیمات وارد شود؛ بدون آن خرید به‌صورت ناامن پذیرفته نمی‌شود.
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
        const val SKU_MONTH = "youtubeyar_vip_monthly"
        const val SKU_QUARTER = "youtubeyar_vip_3months"
        const val SKU_YEAR = "youtubeyar_vip_yearly"
        val ALL_SKUS = listOf(SKU_MONTH, SKU_QUARTER, SKU_YEAR)
        const val TYPE_SUBS = "subs"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingHelper: IabHelper? = null
    private var pendingSku: String? = null
    private var pendingMarket: String? = null
    private var onPurchaseDone: ((Boolean, String) -> Unit)? = null

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

    @Suppress("DEPRECATION")
    private fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Exception) {
        false
    }

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
                    helper.getSkuDetails(ALL_SKUS).associate { it.sku to it.price }
                } catch (_: Exception) {
                    emptyMap()
                }
                helper.unbind()
                onResult(prices)
            }

            override fun onError(message: String) {
                helper.unbind()
                onResult(emptyMap())
            }
        })
    }

    fun startPurchase(activity: Activity, sku: String, onResult: (Boolean, String) -> Unit) {
        if (sku !in ALL_SKUS) {
            onResult(false, "INVALID_PRODUCT")
            return
        }
        val market = detectMarket()
        if (market == null) {
            onResult(false, "NO_MARKET")
            return
        }
        scope.launch {
            val storeSettings = runCatching { settings.current() }.getOrNull()
            val publicKey = when (market) {
                BAZAAR_PACKAGE -> storeSettings?.bazaarRsa?.takeIf { it.isNotBlank() } ?: context.getString(com.youtubeyar.app.R.string.bazaar_rsa_public_key).trim()
                MYKET_PACKAGE -> storeSettings?.myketRsa.orEmpty()
                else -> ""
            }
            if (publicKey.isBlank()) {
                onResult(false, "RSA_KEY_MISSING")
                return@launch
            }
            beginPurchase(activity, sku, market, onResult)
        }
    }

    private fun beginPurchase(activity: Activity, sku: String, market: String, onResult: (Boolean, String) -> Unit) {
        pendingHelper?.unbind()
        pendingHelper = null
        pendingSku = sku
        pendingMarket = market
        onPurchaseDone = onResult
        val helper = IabHelper(context, market)
        pendingHelper = helper
        helper.connect(object : IabHelper.Listener {
            override fun onReady(service: com.android.vending.billing.IInAppBillingService) {
                try {
                    if (!helper.isBillingSupported(TYPE_SUBS)) {
                        finishPurchase(false, "SUBS_NOT_SUPPORTED", helper)
                        return
                    }
                    val buyIntent = helper.getBuyIntent(sku, "youtubeyar-${System.currentTimeMillis()}", TYPE_SUBS)
                    if (buyIntent == null) {
                        finishPurchase(false, "BUY_INTENT_FAILED", helper)
                        return
                    }
                    activity.startIntentSenderForResult(
                        buyIntent.intentSender, PURCHASE_REQUEST_CODE, Intent(), 0, 0, 0
                    )
                } catch (e: Exception) {
                    finishPurchase(false, e.message ?: "PURCHASE_ERROR", helper)
                }
            }

            override fun onError(message: String) {
                finishPurchase(false, message, helper)
            }
        })
    }

    private fun finishPurchase(success: Boolean, message: String, helper: IabHelper? = pendingHelper) {
        helper?.unbind()
        if (pendingHelper === helper) pendingHelper = null
        val callback = onPurchaseDone
        onPurchaseDone = null
        pendingSku = null
        pendingMarket = null
        callback?.invoke(success, message)
    }

    fun handlePurchaseResult(resultCode: Int, data: Intent?) {
        val helper = pendingHelper ?: return
        val expectedSku = pendingSku.orEmpty()
        if (resultCode != Activity.RESULT_OK) {
            finishPurchase(false, "CANCELLED", helper)
            return
        }
        val purchase = IabHelper.parsePurchaseData(data)
        if (purchase == null || purchase.packageName != context.packageName ||
            purchase.sku != expectedSku || purchase.sku !in ALL_SKUS || purchase.token.isBlank()) {
            finishPurchase(false, "INVALID_PURCHASE", helper)
            return
        }

        scope.launch {
            val storeSettings = runCatching { settings.current() }.getOrNull()
            val publicKey = when (pendingMarket) {
                BAZAAR_PACKAGE -> storeSettings?.bazaarRsa.orEmpty()
                MYKET_PACKAGE -> storeSettings?.myketRsa.orEmpty()
                else -> ""
            }
            if (publicKey.isBlank()) {
                finishPurchase(false, "RSA_KEY_MISSING", helper)
                return@launch
            }
            if (!verifyPurchase(purchase.rawData, purchase.signature, publicKey)) {
                finishPurchase(false, "SIGNATURE_INVALID", helper)
                return@launch
            }
            // Never grant entitlement until a valid store signature has been checked.
            settings.setPremium(true, purchase.token)
            runCatching { helper.acknowledge(purchase.token) }
            finishPurchase(true, "OK", helper)
        }
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        val market = detectMarket()
        if (market == null) {
            scope.launch { settings.setPremium(false) }
            onResult(false, "NO_MARKET")
            return
        }
        val helper = IabHelper(context, market)
        helper.connect(object : IabHelper.Listener {
            override fun onReady(service: com.android.vending.billing.IInAppBillingService) {
                scope.launch {
                    try {
                        val storeSettings = settings.current()
                        val publicKey = when (market) {
                            BAZAAR_PACKAGE -> storeSettings.bazaarRsa.takeIf { it.isNotBlank() } ?: context.getString(com.youtubeyar.app.R.string.bazaar_rsa_public_key).trim()
                            MYKET_PACKAGE -> storeSettings.myketRsa
                            else -> ""
                        }
                        if (publicKey.isBlank()) {
                            settings.setPremium(false)
                            onResult(false, "RSA_KEY_MISSING")
                            return@launch
                        }
                        val purchases = helper.getPurchases(TYPE_SUBS)
                        val valid = purchases.filter { p ->
                            p.packageName == context.packageName &&
                                p.token.isNotBlank() && p.sku in ALL_SKUS &&
                                p.rawData.isNotBlank() &&
                                verifyPurchase(p.rawData, p.signature, publicKey)
                        }
                        if (valid.isNotEmpty()) {
                            valid.forEach { runCatching { helper.acknowledge(it.token) } }
                            settings.setPremium(true, valid.maxByOrNull { it.purchaseTime }!!.token)
                            onResult(true, "OK")
                        } else {
                            // The store's active-subscription query is authoritative for restoration.
                            settings.setPremium(false)
                            onResult(false, "NOT_FOUND")
                        }
                    } catch (e: Exception) {
                        onResult(false, e.message ?: "RESTORE_ERROR")
                    } finally {
                        helper.unbind()
                    }
                }
            }

            override fun onError(message: String) {
                helper.unbind()
                onResult(false, message)
            }
        })
    }

    private fun verifyPurchase(rawData: String, signatureText: String, encodedPublicKey: String): Boolean {
        if (rawData.isBlank() || signatureText.isBlank() || encodedPublicKey.isBlank()) return false
        return try {
            val keyText = encodedPublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .filterNot { it.isWhitespace() }
            val keyBytes = Base64.decode(keyText, Base64.DEFAULT)
            val signatureBytes = Base64.decode(signatureText, Base64.DEFAULT)
            val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            Signature.getInstance("SHA1withRSA").run {
                initVerify(key)
                update(rawData.toByteArray(Charsets.UTF_8))
                verify(signatureBytes)
            }
        } catch (_: Exception) {
            false
        }
    }
}
