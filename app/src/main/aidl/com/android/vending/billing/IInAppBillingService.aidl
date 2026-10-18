/*
 * رابط In-App Billing نسخه ۳
 * این فایل AIDL استاندارد است که هم کافه‌بازار و هم مایکت از آن پشتیبانی می‌کنند.
 * محل صحیح این فایل: app/src/main/aidl/com/android/vending/billing/
 */
package com.android.vending.billing;

import android.os.Bundle;

interface IInAppBillingService {
    int isBillingSupported(int apiVersion, String packageName, String type);
    Bundle getSkuDetails(int apiVersion, String packageName, String type, in Bundle skusBundle);
    Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload);
    Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken);
    int consumePurchase(int apiVersion, String packageName, String purchaseToken);
    Bundle getBuyIntentToReplaceSkus(int apiVersion, String packageName, in List<String> oldSkus, String newSku, String type, String developerPayload);
    Bundle getBuyIntentExtraParams(int apiVersion, String packageName, String sku, String type, String developerPayload, in Bundle extraParams);
    int acknowledgePurchase(int apiVersion, String packageName, String purchaseToken);
}
