package com.infinityapps.myreviewsample

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private  var billingClient: BillingClient?=null
    private  var skuDetails: SkuDetails?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpBillingClient()
        initListeners()
    }

    private fun initListeners() {
        txt_product_buy?.setOnClickListener {
            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            skuDetails?.let {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
                billingClient?.launchBillingFlow(this, billingFlowParams)?.responseCode
            }?:noSKUMessage()

        }
    }

    private fun noSKUMessage() {

    }

    private fun setUpBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.v("TAG_INAPP","Setup Billing Done")
                    queryAvaliableProducts()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun queryAvaliableProducts() {
        val skuList = ArrayList<String>()
        skuList.add("test.sample")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            // Process the result.
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                for (skuDetails in skuDetailsList) {
                    Log.v("TAG_INAPP","skuDetailsList : ${skuDetailsList}")
                    //This list should contain the products added above
                    updateUI(skuDetails)
                }
            }
        }
    }

    private fun updateUI(skuDetails: SkuDetails?) {
        skuDetails?.let {
            this.skuDetails = it
            txt_product_name?.text = skuDetails.title
            txt_product_description?.text = skuDetails.description
            showUIElements()
        }
    }

    private fun showUIElements() {
        txt_product_name?.visibility = View.VISIBLE
        txt_product_description?.visibility = View.VISIBLE
        txt_product_buy?.visibility = View.VISIBLE
    }

    private val purchaseUpdateListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                Log.v("TAG_INAPP","billingResult responseCode : ${billingResult.responseCode}")

                if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
//                        handlePurchase(purchase)
                        handleConsumedPurchases(purchase)
                    }
                } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }
            }

    private fun handleConsumedPurchases(purchase: Purchase) {
            Log.d("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is $purchase")
            val params =
                ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient?.consumeAsync(params) { billingResult, purchaseToken ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // Update the appropriate tables/databases to grant user the items
                        Log.d(
                            "TAG_INAPP",
                            " Update the appropriate tables/databases to grant user the items"
                        )
                    }
                    else -> {
                        Log.w("TAG_INAPP", billingResult.debugMessage)
                    }
                }
            }
    }

    private fun handleNonConcumablePurchase(purchase: Purchase) {
        Log.v("TAG_INAPP","handlePurchase : ${purchase}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    val billingResponseCode = billingResult.responseCode
                    val billingDebugMessage = billingResult.debugMessage

                    Log.v("TAG_INAPP","response code: $billingResponseCode")
                    Log.v("TAG_INAPP","debugMessage : $billingDebugMessage")

                }
            }
        }
    }


}