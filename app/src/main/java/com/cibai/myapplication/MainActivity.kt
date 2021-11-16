package com.cibai.myapplication

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileAds.initialize(this) {}
        setUpInAppPurchase()
        setUpSubscriptionsPurchase()
        setUpAds()
    }

    private fun setUpAds() {
        val adRequest = AdRequest.Builder().build()
        val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        var mInterstitialAd: InterstitialAd? = null
        var mAdIsLoading: Boolean = false
        fun loadAd() {
            InterstitialAd.load(
                this, AD_UNIT_ID, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        mInterstitialAd = null
                        mAdIsLoading = false
                        val error = "domain: ${adError.domain}, code: ${adError.code}, " +
                                "message: ${adError.message}"
                        Toast.makeText(
                            this@MainActivity,
                            "onAdFailedToLoad() with error $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d(TAG, "Ad was loaded.")
                        mInterstitialAd = interstitialAd
                        mAdIsLoading = false
                        Toast.makeText(this@MainActivity, "onAdLoaded()", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        fun showInterstitial() {
            if (mInterstitialAd != null) {
                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mInterstitialAd = null
                        loadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mInterstitialAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }
                mInterstitialAd?.show(this)
            }
        }

        loadAd()

        val adsButton: Button = findViewById(R.id.adsButton)
        adsButton.setOnClickListener {
            showInterstitial()
        }
    }

    private fun setUpSubscriptionsPurchase() {
        val subscriptionsButton: Button = findViewById(R.id.subscriptionsButton)
        var billingClient: BillingClient? = null
        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                val purchase = purchases?.first()
                if (purchase != null) {
                    val purchaseParams: AcknowledgePurchaseParams = AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(purchaseParams) { billingResults ->

                    }
                }
            }
        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        var skus = listOf<SkuDetails>()
        subscriptionsButton.setOnClickListener {
            if (billingClient.isReady && skus.size == 1) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skus[0])
                    .build()
                billingClient.launchBillingFlow(this, flowParams).responseCode.toString()
            }
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    val skuList = ArrayList<String>()
                    skuList.add("product_2")
                    val params = SkuDetailsParams
                        .newBuilder()
                        .setSkusList(skuList)
                        .setType(BillingClient.SkuType.SUBS)
                        .build()
                    billingClient.querySkuDetailsAsync(params) { billingResultSkuDetails, skuDetailsList ->
                        if (billingResultSkuDetails.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (skuDetailsList != null) {
                                skus = skuDetailsList
                            }
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() {

            }
        })
    }

    private fun setUpInAppPurchase() {
        var billingClient: BillingClient? = null;

        fun allowMultiplePurchases(purchases: MutableList<Purchase>?) {
            val purchase = purchases?.first()
            if (purchase != null) {
                val consumeParams: ConsumeParams = ConsumeParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build();
                billingClient?.consumeAsync(consumeParams) { billingResult, purchaseToken ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        println("AllowMultiplePurchases success, responseCode: ${billingResult.responseCode}")
                    } else {
                        println("Can't allowMultiplePurchases, responseCode: ${billingResult.responseCode}")
                    }
                }
            }
        }

        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                allowMultiplePurchases(purchases)
            }

        billingClient = BillingClient.newBuilder(applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        val inAppPurchaseButton: Button = findViewById(R.id.inAppPurchaseButton)
        var skus = listOf<SkuDetails>()
        inAppPurchaseButton.setOnClickListener {
            if (billingClient.isReady && skus.size == 1) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skus[0])
                    .build()
                billingClient.launchBillingFlow(this, flowParams).responseCode.toString()
            }
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    val skuList = ArrayList<String>()
                    skuList.add("product_3")
                    val params = SkuDetailsParams
                        .newBuilder()
                        .setSkusList(skuList)
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                    billingClient.querySkuDetailsAsync(params) { billingResultSkuDetails, skuDetailsList ->
                        if (billingResultSkuDetails.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (skuDetailsList != null) {
                                skus = skuDetailsList
                            }
                        } else {
                        }
                    }
                } else {
                }
            }
            override fun onBillingServiceDisconnected() {
            }
        })
    }
}