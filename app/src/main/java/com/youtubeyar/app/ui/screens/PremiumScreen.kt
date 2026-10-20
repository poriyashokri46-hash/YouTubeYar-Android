package com.youtubeyar.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.youtubeyar.app.R
import com.youtubeyar.app.ads.TapsellAdsManager
import com.youtubeyar.app.billing.BillingManager
import com.youtubeyar.app.data.SettingsRepository
import com.youtubeyar.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    val billing: BillingManager,
    val adsManager: TapsellAdsManager
) : ViewModel() {

    val settings = settingsRepository.settings

    data class UiState(
        val prices: Map<String, String> = emptyMap(),
        val message: String? = null,
        val busy: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    init {
        billing.loadPrices { prices ->
            _ui.value = _ui.value.copy(prices = prices)
        }
    }

    fun buy(activity: Activity, sku: String) {
        _ui.value = _ui.value.copy(busy = true, message = null)
        billing.startPurchase(activity, sku) { ok, code ->
            _ui.value = _ui.value.copy(
                busy = false,
                message = if (ok) "OK" else code
            )
        }
    }

    fun restore() {
        _ui.value = _ui.value.copy(busy = true, message = null)
        billing.restorePurchases { found, _ ->
            _ui.value = _ui.value.copy(busy = false, message = if (found) "RESTORED" else "NONE")
        }
    }

    fun watchRewarded(activity: Activity) {
        _ui.value = _ui.value.copy(busy = true, message = null)
        adsManager.showRewarded(
            activity,
            onReward = {
                viewModelScope.launch {
                    settingsRepository.grantRewardedUnlock()
                    _ui.value = _ui.value.copy(busy = false, message = "REWARD_OK")
                }
            },
            onFail = { error ->
                _ui.value = _ui.value.copy(busy = false, message = error)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(navController: NavController, mainViewModel: com.youtubeyar.app.ui.MainViewModel, vm: PremiumViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val settings by vm.settings.collectAsState(initial = null)
    val context = LocalContext.current
    var showThanks by remember { mutableStateOf(false) }

    val premium = settings?.isPremium() ?: false
    val market = remember { vm.billing.detectMarket() }
    val rewardedConfigured = vm.adsManager.isRewardedConfigured()

    LaunchedEffect(ui.message) {
        when (ui.message) {
            "OK", "RESTORED", "REWARD_OK" -> showThanks = true
            else -> Unit
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.premium_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.width(42.dp).height(42.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.pr_headline),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                stringResource(R.string.pr_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (premium) {
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.pr_active),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                item {
                    SectionCard {
                        listOf(
                            stringResource(R.string.pr_feature_1),
                            stringResource(R.string.pr_feature_2),
                            stringResource(R.string.pr_feature_3),
                            stringResource(R.string.pr_feature_4)
                        ).forEach { f ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.height(18.dp).width(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(f, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (!premium) {
                item {
                    SectionCard(title = stringResource(R.string.pr_buy)) {
                        if (market == null) {
                            Text(
                                stringResource(R.string.pr_store_missing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                vm.billing.marketName(market),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))

                            PriceButton(
                                title = stringResource(R.string.pr_month),
                                price = ui.prices[BillingManager.SKU_MONTH] ?: stringResource(R.string.pr_price_unavailable),
                                enabled = !ui.busy,
                                busy = ui.busy,
                                onClick = { (context as? Activity)?.let { vm.buy(it, BillingManager.SKU_MONTH) } }
                            )
                            Spacer(Modifier.height(8.dp))
                            PriceButton(
                                title = stringResource(R.string.pr_quarter),
                                price = ui.prices[BillingManager.SKU_QUARTER] ?: stringResource(R.string.pr_price_unavailable),
                                enabled = !ui.busy,
                                busy = false,
                                onClick = { (context as? Activity)?.let { vm.buy(it, BillingManager.SKU_QUARTER) } }
                            )
                            Spacer(Modifier.height(8.dp))
                            PriceButton(
                                title = stringResource(R.string.pr_year),
                                price = ui.prices[BillingManager.SKU_YEAR] ?: stringResource(R.string.pr_price_unavailable),
                                enabled = !ui.busy,
                                busy = false,
                                onClick = { (context as? Activity)?.let { vm.buy(it, BillingManager.SKU_YEAR) } }
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { vm.restore() }, shape = RoundedCornerShape(12.dp), enabled = !ui.busy) {
                                Text(stringResource(R.string.pr_restore))
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.pr_rewarded_title)) {
                        Text(
                            stringResource(R.string.pr_rewarded_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        if (!rewardedConfigured) {
                            Text(
                                stringResource(R.string.pr_ads_not_configured),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { (context as? Activity)?.let { vm.watchRewarded(it) } },
                            enabled = !ui.busy && rewardedConfigured,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(stringResource(R.string.pr_watch_ad)) }
                    }
                }
            }

            ui.message?.takeIf { it !in listOf("OK", "RESTORED", "REWARD_OK") }?.let { code ->
                item {
                    Text(
                        when (code) {
                            "CANCELLED" -> stringResource(R.string.pr_cancelled)
                            "NO_MARKET", "SERVICE_UNAVAILABLE" -> stringResource(R.string.pr_store_missing)
                            "RSA_KEY_MISSING" -> stringResource(R.string.pr_rsa_missing)
                            "SIGNATURE_INVALID" -> stringResource(R.string.pr_signature_invalid)
                            "INVALID_PURCHASE", "INVALID_PRODUCT" -> stringResource(R.string.pr_purchase_invalid)
                            "NOT_CONFIGURED", "REWARDED_ZONE_MISSING" -> stringResource(R.string.pr_ads_not_configured)
                            "NO_AD", "AD_CLOSED_NO_REWARD", "AD_ERROR", "SHOW_FAILED", "REQUEST_FAILED" -> stringResource(R.string.pr_no_ad_available)
                            else -> stringResource(R.string.pr_error, code)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showThanks) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThanks = false },
            title = { Text(stringResource(R.string.pr_thanks)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showThanks = false }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@Composable
private fun PriceButton(title: String, price: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(price, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
