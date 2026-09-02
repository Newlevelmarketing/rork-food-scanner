package com.rork.calzyandroid.data

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreTransaction
import com.rork.calzyandroid.BuildConfig
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Billing term shown on the paywall. */
enum class PlanTerm(val label: String, val unitNoun: String) {
    WEEKLY("Weekly", "week"),
    MONTHLY("Monthly", "month"),
    YEARLY("Yearly", "year"),
}

/**
 * One purchasable row on the paywall.
 *
 * [rcPackage] is null only for the debug-only design preview, which is why every
 * purchase path checks it before charging anything.
 */
data class SubscriptionPlan(
    val id: String,
    val term: PlanTerm,
    /** Localized total charged each period, e.g. "€17.99". */
    val price: String,
    /** Localized equivalent unit price for longer terms, e.g. "€7.42 / month". */
    val perUnit: String?,
    val badge: String?,
    val rcPackage: Package?,
)

data class StoreState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val plans: List<SubscriptionPlan> = emptyList(),
    val isSubscribed: Boolean = false,
    val purchasingId: String? = null,
    val isRestoring: Boolean = false,
    val message: String? = null,
    val loadFailed: Boolean = false,
)

/**
 * RevenueCat wrapper for ModernBody Pro.
 *
 * The SDK is only configured when [PurchaseConfig] carries a real key. Without
 * one the manager still exposes state so the UI can explain itself, but it never
 * touches the billing stack and never claims a subscription is active.
 *
 * Every price shown comes from a live store product. There is deliberately no
 * hardcoded fallback pricing: if the offering cannot be loaded the paywall says
 * so rather than displaying a figure that is not what would be charged.
 */
object PurchaseManager {

    private val _state = MutableStateFlow(StoreState(isConfigured = PurchaseConfig.isConfigured))
    val state: StateFlow<StoreState> = _state.asStateFlow()

    fun configure(context: Context) {
        if (!PurchaseConfig.isConfigured) {
            _state.value = _state.value.copy(isConfigured = false, plans = emptyList())
            return
        }
        if (Purchases.isConfigured) return

        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR
        Purchases.configure(
            PurchasesConfiguration.Builder(context, PurchaseConfig.apiKey).build(),
        )
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
            _state.value = _state.value.copy(isSubscribed = info.isPro())
        }
        _state.value = _state.value.copy(isConfigured = true)
        refreshCustomerInfo()
    }

    private fun CustomerInfo.isPro(): Boolean =
        entitlements[PurchaseConfig.ENTITLEMENT_ID]?.isActive == true

    fun refreshCustomerInfo() {
        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.getCustomerInfo(
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _state.value = _state.value.copy(isSubscribed = customerInfo.isPro())
                }

                override fun onError(error: PurchasesError) {
                    // Entitlement state is cached by the SDK; a refresh failure is not
                    // surfaced because it would interrupt an otherwise working screen.
                }
            },
        )
    }

    /** Loads the current offering and maps it onto the three paywall rows. */
    fun refreshOfferings() {
        if (!Purchases.isConfigured) return
        _state.value = _state.value.copy(isLoading = true, loadFailed = false, message = null)
        Purchases.sharedInstance.getOfferings(
            object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val offering = PurchaseConfig.OFFERING_ID
                        .takeIf { it.isNotBlank() }
                        ?.let { offerings[it] }
                        ?: offerings.current
                    val plans = offering?.availablePackages.orEmpty().mapNotNull { it.toPlan() }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        plans = plans.sortedBy { it.term.ordinal },
                        loadFailed = plans.isEmpty(),
                    )
                }

                override fun onError(error: PurchasesError) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        loadFailed = true,
                        message = error.friendlyMessage(),
                    )
                }
            },
        )
    }

    fun purchase(activity: Activity, plan: SubscriptionPlan) {
        val target = plan.rcPackage
        if (!Purchases.isConfigured || target == null) {
            _state.value = _state.value.copy(
                message = "Subscriptions aren't available in this build yet.",
            )
            return
        }
        _state.value = _state.value.copy(purchasingId = plan.id, message = null)
        Purchases.sharedInstance.purchase(
            PurchaseParams.Builder(activity, target).build(),
            object : PurchaseCallback {
                override fun onCompleted(
                    storeTransaction: StoreTransaction,
                    customerInfo: CustomerInfo,
                ) {
                    _state.value = _state.value.copy(
                        purchasingId = null,
                        isSubscribed = customerInfo.isPro(),
                    )
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    _state.value = _state.value.copy(
                        purchasingId = null,
                        // A deliberate cancel is not a failure and gets no alert.
                        message = if (userCancelled) null else error.friendlyMessage(),
                    )
                }
            },
        )
    }

    fun restore() {
        if (!Purchases.isConfigured) {
            _state.value = _state.value.copy(
                message = "Subscriptions aren't available in this build yet.",
            )
            return
        }
        _state.value = _state.value.copy(isRestoring = true, message = null)
        Purchases.sharedInstance.restorePurchases(
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val restored = customerInfo.isPro()
                    _state.value = _state.value.copy(
                        isRestoring = false,
                        isSubscribed = restored,
                        message = if (restored) {
                            "Your subscription has been restored."
                        } else {
                            "No previous purchase was found for this Google account."
                        },
                    )
                }

                override fun onError(error: PurchasesError) {
                    _state.value = _state.value.copy(
                        isRestoring = false,
                        message = error.friendlyMessage(),
                    )
                }
            },
        )
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    // MARK: - Mapping

    private fun Package.toPlan(): SubscriptionPlan? {
        val product = product
        val term = when (packageType) {
            PackageType.WEEKLY -> PlanTerm.WEEKLY
            PackageType.MONTHLY -> PlanTerm.MONTHLY
            PackageType.ANNUAL -> PlanTerm.YEARLY
            else -> product.period?.toTerm()
        } ?: return null

        val micros = product.price.amountMicros
        val currency = product.price.currencyCode
        return SubscriptionPlan(
            id = identifier,
            term = term,
            price = product.price.formatted,
            perUnit = when (term) {
                PlanTerm.YEARLY -> format(micros / 12, currency)?.let { "$it / month" }
                else -> null
            },
            badge = if (term == PlanTerm.YEARLY) "Best value" else null,
            rcPackage = this,
        )
    }

    private fun Period.toTerm(): PlanTerm? = when (unit) {
        Period.Unit.WEEK -> PlanTerm.WEEKLY
        Period.Unit.MONTH -> if (value == 12) PlanTerm.YEARLY else PlanTerm.MONTHLY
        Period.Unit.YEAR -> PlanTerm.YEARLY
        else -> null
    }

    private fun format(amountMicros: Long, currencyCode: String): String? = try {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(currencyCode)
        }.format(amountMicros / 1_000_000.0)
    } catch (error: IllegalArgumentException) {
        null
    }

    private fun PurchasesError.friendlyMessage(): String = when (code) {
        PurchasesErrorCode.NetworkError ->
            "You appear to be offline. Check your connection and try again."
        PurchasesErrorCode.StoreProblemError ->
            "The Play Store is having trouble right now. Please try again in a moment."
        PurchasesErrorCode.PurchaseNotAllowedError ->
            "This Google account isn't allowed to make purchases on this device."
        PurchasesErrorCode.ProductAlreadyPurchasedError ->
            "You already own this subscription. Try Restore Purchases."
        PurchasesErrorCode.PaymentPendingError ->
            "Your purchase is pending approval. Pro unlocks as soon as it clears."
        else -> "Something went wrong with the purchase. Please try again."
    }
}
