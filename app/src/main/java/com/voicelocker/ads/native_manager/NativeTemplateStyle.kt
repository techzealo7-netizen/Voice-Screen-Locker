package com.voicelocker.ads.native_manager

import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable

class NativeTemplateStyle private constructor(builder: Builder) {

    val callToActionBackgroundColor: ColorDrawable? = builder.callToActionBackgroundColor
    val primaryTextBackgroundColor: ColorDrawable? = builder.primaryTextBackgroundColor
    val secondaryTextBackgroundColor: ColorDrawable? = builder.secondaryTextBackgroundColor
    val tertiaryTextBackgroundColor: ColorDrawable? = builder.tertiaryTextBackgroundColor
    val mainBackgroundColor: ColorDrawable? = builder.mainBackgroundColor
    val primaryTextTypeface: Typeface? = builder.primaryTextTypeface
    val secondaryTextTypeface: Typeface? = builder.secondaryTextTypeface
    val tertiaryTextTypeface: Typeface? = builder.tertiaryTextTypeface
    val callToActionTextTypeface: Typeface? = builder.callToActionTextTypeface
    val primaryTextTypefaceColor: Int? = builder.primaryTextTypefaceColor
    val secondaryTextTypefaceColor: Int? = builder.secondaryTextTypefaceColor
    val tertiaryTextTypefaceColor: Int? = builder.tertiaryTextTypefaceColor
    val callToActionTypefaceColor: Int? = builder.callToActionTypefaceColor
    val callToActionTextSize: Float = builder.callToActionTextSize
    val primaryTextSize: Float = builder.primaryTextSize
    val secondaryTextSize: Float = builder.secondaryTextSize
    val tertiaryTextSize: Float = builder.tertiaryTextSize

    class Builder {
        var callToActionBackgroundColor: ColorDrawable? = null
        var primaryTextBackgroundColor: ColorDrawable? = null
        var secondaryTextBackgroundColor: ColorDrawable? = null
        var tertiaryTextBackgroundColor: ColorDrawable? = null
        var mainBackgroundColor: ColorDrawable? = null
        var primaryTextTypeface: Typeface? = null
        var secondaryTextTypeface: Typeface? = null
        var tertiaryTextTypeface: Typeface? = null
        var callToActionTextTypeface: Typeface? = null
        var primaryTextTypefaceColor: Int? = null
        var secondaryTextTypefaceColor: Int? = null
        var tertiaryTextTypefaceColor: Int? = null
        var callToActionTypefaceColor: Int? = null
        var callToActionTextSize: Float = 0f
        var primaryTextSize: Float = 0f
        var secondaryTextSize: Float = 0f
        var tertiaryTextSize: Float = 0f

        fun withCallToActionBackgroundColor(color: ColorDrawable) = apply { this.callToActionBackgroundColor = color }
        fun withPrimaryTextBackgroundColor(color: ColorDrawable) = apply { this.primaryTextBackgroundColor = color }
        fun withSecondaryTextBackgroundColor(color: ColorDrawable) = apply { this.secondaryTextBackgroundColor = color }
        fun withTertiaryTextBackgroundColor(color: ColorDrawable) = apply { this.tertiaryTextBackgroundColor = color }
        fun withMainBackgroundColor(color: ColorDrawable) = apply { this.mainBackgroundColor = color }
        fun withPrimaryTextTypeface(typeface: Typeface) = apply { this.primaryTextTypeface = typeface }
        fun withSecondaryTextTypeface(typeface: Typeface) = apply { this.secondaryTextTypeface = typeface }
        fun withTertiaryTextTypeface(typeface: Typeface) = apply { this.tertiaryTextTypeface = typeface }
        fun withCallToActionTextTypeface(typeface: Typeface) = apply { this.callToActionTextTypeface = typeface }
        fun withPrimaryTextTypefaceColor(color: Int) = apply { this.primaryTextTypefaceColor = color }
        fun withSecondaryTextTypefaceColor(color: Int) = apply { this.secondaryTextTypefaceColor = color }
        fun withTertiaryTextTypefaceColor(color: Int) = apply { this.tertiaryTextTypefaceColor = color }
        fun withCallToActionTypefaceColor(color: Int) = apply { this.callToActionTypefaceColor = color }
        fun withCallToActionTextSize(size: Float) = apply { this.callToActionTextSize = size }
        fun withPrimaryTextSize(size: Float) = apply { this.primaryTextSize = size }
        fun withSecondaryTextSize(size: Float) = apply { this.secondaryTextSize = size }
        fun withTertiaryTextSize(size: Float) = apply { this.tertiaryTextSize = size }

        fun build() = NativeTemplateStyle(this)
    }
}
