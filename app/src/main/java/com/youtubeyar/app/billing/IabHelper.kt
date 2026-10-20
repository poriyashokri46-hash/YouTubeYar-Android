package com.youtubeyar.app.billing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import com.android.vending.billing.IInAppBillingService
import org.json.JSONObject

/**
 * helper سبک برای In-App Billing v3.
 * هر دو کافه‌بازار و مایکت پروتکل استاندارد IAB v3 را پیاده‌سازی کرده‌اند؛
 * تنها تفاوت، نام پکیج فروشگاه (marketPackage) است.
 */
class IabHelper(private val context: Context, private val marketPackage: String) {

    interface Listener {
        fun onReady(service: IInAppBillingService)
        fun onError(message: String)
    }

    private var service: IInAppBillingService? = null
    private var bound = false
    private var connection: ServiceConnection? = null

    fun connect(listener: Listener) {
        try {
            val intent = Intent("$marketPackage.InAppBillingService.BIND")
            intent.setPackage(marketPackage)
            val infos = context.packageManager.queryIntentServices(intent, 0)
            if (infos == null || infos.isEmpty()) {
                listener.onError("SERVICE_UNAVAILABLE")
                return
            }
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    service = IInAppBillingService.Stub.asInterface(binder)
                    bound = true
                    listener.onReady(service!!)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    service = null
                    bound = false
                }
            }
            connection = serviceConnection
            bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) listener.onError("BIND_FAILED")
        } catch (e: Exception) {
            listener.onError(e.message ?: "CONNECT_ERROR")
        }
    }

    fun unbind() {
        try {
            if (bound && connection != null) {
                context.unbindService(connection!!)
                bound = false
            }
        } catch (_: Exception) {
        }
    }

    /** آیا فروشگاه از اشتراک (subs) پشتیبانی می‌کند؟ */
    fun isBillingSupported(type: String = "subs"): Boolean = try {
        service?.isBillingSupported(3, context.packageName, type) == 0
    } catch (_: RemoteException) {
        false
    }

    /** دریافت جزئیات قیمت محصولات */
    fun getSkuDetails(skus: List<String>, type: String = "subs"): List<SkuInfo> {
        val out = mutableListOf<SkuInfo>()
        val bundle = Bundle().apply { putStringArrayList("ITEM_ID_LIST", ArrayList(skus)) }
        val response = service?.getSkuDetails(3, context.packageName, type, bundle) ?: return out
        if (response.getInt("RESPONSE_CODE") != 0) return out
        val list = response.getStringArrayList("DETAILS_LIST") ?: return out
        for (json in list) {
            try {
                val o = JSONObject(json)
                out += SkuInfo(
                    sku = o.optString("productId"),
                    price = o.optString("price"),
                    title = o.optString("title"),
                    description = o.optString("description"),
                    type = type
                )
            } catch (_: Exception) {
            }
        }
        return out
    }

    /** گرفتن PendingIntent خرید برای شروع دیالوگ پرداخت */
    fun getBuyIntent(sku: String, payload: String, type: String = "subs"): android.app.PendingIntent? {
        val bundle = service?.getBuyIntent(3, context.packageName, sku, type, payload) ?: return null
        return if (bundle.getInt("RESPONSE_CODE") == 0) {
            bundle.getParcelable("BUY_INTENT")
        } else null
    }

    /** لیست خریدها/اشتراک‌های فعال کاربر */
    fun getPurchases(type: String = "subs"): List<PurchaseInfo> {
        val out = mutableListOf<PurchaseInfo>()
        val response = service?.getPurchases(3, context.packageName, type, null)
            ?: throw IllegalStateException("BILLING_SERVICE_UNAVAILABLE")
        if (response.getInt("RESPONSE_CODE") != 0) {
            throw IllegalStateException("PURCHASE_QUERY_FAILED_${response.getInt("RESPONSE_CODE")}")
        }
        val dataList = response.getStringArrayList("INAPP_PURCHASE_DATA_LIST")
            ?: response.getStringArrayList("INAPP_PURCHASE_DATA")?.let { arrayListOf(it) }
            ?: return out
        val signatures = response.getStringArrayList("INAPP_DATA_SIGNATURE_LIST").orEmpty()
        dataList.forEachIndexed { index, json ->
            try {
                val o = JSONObject(json)
                out += PurchaseInfo(
                    sku = o.optString("productId"),
                    token = o.optString("purchaseToken"),
                    packageName = o.optString("packageName"),
                    purchaseTime = o.optLong("purchaseTime"),
                    payload = o.optString("developerPayload"),
                    autoRenewing = o.optBoolean("autoRenewing", false),
                    signature = signatures.getOrNull(index).orEmpty(),
                    rawData = json
                )
            } catch (_: Exception) {
            }
        }
        return out
    }

    fun acknowledge(token: String): Boolean = try {
        service?.acknowledgePurchase(3, context.packageName, token) == 0
    } catch (_: Exception) {
        false
    }

    fun consume(token: String): Boolean = try {
        service?.consumePurchase(3, context.packageName, token) == 0
    } catch (_: Exception) {
        false
    }

    data class SkuInfo(val sku: String, val price: String, val title: String, val description: String, val type: String)
    data class PurchaseInfo(
        val sku: String,
        val token: String,
        val packageName: String,
        val purchaseTime: Long,
        val payload: String,
        val autoRenewing: Boolean,
        val signature: String = "",
        val rawData: String = ""
    )

    companion object {
        /** استخراج خرید از داده‌های برگشتی onActivityResult */
        fun parsePurchaseData(data: android.content.Intent?): PurchaseInfo? {
            if (data == null) return null
            val code = data.getIntExtra("RESPONSE_CODE", -1)
            if (code != 0) return null
            return try {
                val json = data.getStringExtra("INAPP_PURCHASE_DATA") ?: return null
                val signature = data.getStringExtra("INAPP_DATA_SIGNATURE").orEmpty()
                val o = JSONObject(json)
                // Reject cancelled/unpurchased records and malformed purchase payloads.
                if (o.optInt("purchaseState", -1) != 0) return null
                val sku = o.optString("productId")
                val token = o.optString("purchaseToken")
                val packageName = o.optString("packageName")
                if (sku.isBlank() || token.isBlank() || packageName.isBlank()) return null
                PurchaseInfo(
                    sku = sku,
                    token = token,
                    packageName = packageName,
                    purchaseTime = o.optLong("purchaseTime"),
                    payload = o.optString("developerPayload"),
                    autoRenewing = o.optBoolean("autoRenewing", false),
                    signature = signature,
                    rawData = json
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
