package com.voicelocker.app.util

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

/**
 * Display a short toast message.
 */
fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

/**
 * Traverses context wrappers to find the enclosing FragmentActivity.
 */
fun Context.findActivity(): FragmentActivity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is FragmentActivity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Format seconds into mm:ss display string.
 */
fun fmtTime(s: Int): String = "%02d:%02d".format(s / 60, s % 60)

