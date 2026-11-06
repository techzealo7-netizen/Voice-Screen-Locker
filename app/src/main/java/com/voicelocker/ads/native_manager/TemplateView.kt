package com.voicelocker.ads.native_manager

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.voicelocker.app.R
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class TemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var templateType: Int = R.layout.ads_google_large_native
    private var styles: NativeTemplateStyle? = null
    private var nativeAd: NativeAd? = null
    private var nativeAdView: NativeAdView? = null
    private var primaryView: TextView? = null
    private var secondaryView: TextView? = null
    private var ratingBar: RatingBar? = null
    private var iconView: ImageView? = null
    private var mediaView: MediaView? = null
    private var callToActionView: Button? = null
    private var background: ConstraintLayout? = null

    init {
        initView(context, attrs)
    }

    fun setStyles(styles: NativeTemplateStyle) {
        this.styles = styles
        applyStyles()
    }

    fun getNativeAdView(): NativeAdView? = nativeAdView

    private fun applyStyles() {
        val s = styles ?: return

        s.mainBackgroundColor?.let {
            background?.background = it
            primaryView?.background = it
            secondaryView?.background = it
        }

        s.primaryTextTypeface?.let { primaryView?.typeface = it }
        s.secondaryTextTypeface?.let { secondaryView?.typeface = it }
        s.callToActionTextTypeface?.let { callToActionView?.typeface = it }

        s.primaryTextTypefaceColor?.let { primaryView?.setTextColor(it) }
        s.secondaryTextTypefaceColor?.let { secondaryView?.setTextColor(it) }
        s.callToActionTypefaceColor?.let { callToActionView?.setTextColor(it) }

        if (s.callToActionTextSize > 0) callToActionView?.textSize = s.callToActionTextSize
        if (s.primaryTextSize > 0) primaryView?.textSize = s.primaryTextSize
        if (s.secondaryTextSize > 0) secondaryView?.textSize = s.secondaryTextSize

        s.callToActionBackgroundColor?.let { callToActionView?.background = it }
        s.primaryTextBackgroundColor?.let { primaryView?.background = it }
        s.secondaryTextBackgroundColor?.let { secondaryView?.background = it }

        invalidate()
        requestLayout()
    }

    fun setNativeAd(ad: NativeAd) {
        this.nativeAd = ad

        val nav = nativeAdView ?: return

        val headline = ad.headline
        val cta = ad.callToAction
        val starRating = ad.starRating
        val icon = ad.icon

        callToActionView?.let { nav.callToActionView = it }
        primaryView?.let { nav.headlineView = it }

        secondaryView?.let { sv ->
            nav.bodyView = sv
            sv.text = ad.body ?: ""
        }

        headline?.let { primaryView?.text = it }
        cta?.let { callToActionView?.text = it }

        if (starRating != null && starRating > 0) {
            ratingBar?.apply {
                visibility = VISIBLE
                rating = starRating.toFloat()
                nav.starRatingView = this
            }
        } else {
            ratingBar?.visibility = GONE
        }

        if (icon?.drawable != null) {
            iconView?.apply {
                visibility = VISIBLE
                setImageDrawable(icon.drawable)
            }
        } else {
            iconView?.visibility = GONE
        }

        mediaView?.visibility = VISIBLE
        try {
            android.util.Log.d("TemplateView", "▶️ [Native:TemplateView] Show requested (registering native ad)")
            nav.registerNativeAd(ad, mediaView)
            android.util.Log.d("TemplateView", "🎉 [Native:TemplateView] Show success (native ad registered)")
        } catch (e: Exception) {
            android.util.Log.e("TemplateView", "❌ [Native:TemplateView] Failed to show: ${e.message}", e)
        }
    }

    fun destroyNativeAd() {
        nativeAd?.destroy()
    }

    private fun initView(context: Context, attributeSet: AttributeSet?) {
        templateType = R.layout.ads_google_large_native
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(templateType, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        nativeAdView = findViewById<NativeAdView>(R.id.native_ad_view)
        primaryView = findViewById<TextView>(R.id.primary)
        secondaryView = findViewById<TextView>(R.id.secondary)
        ratingBar = findViewById<RatingBar>(R.id.rating_bar)
        ratingBar?.isEnabled = false
        callToActionView = findViewById<Button>(R.id.cta)
        iconView = findViewById<ImageView>(R.id.icon)
        mediaView = findViewById<MediaView>(R.id.media_view)
        background = findViewById<ConstraintLayout>(R.id.background)
    }
}
