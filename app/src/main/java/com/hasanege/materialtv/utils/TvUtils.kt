package com.hasanege.materialtv.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object TvUtils {

    /**
     * Returns true if the physical hardware is an Android TV / Leanback device.
     */
    fun isHardwareTv(context: Context): Boolean {
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return true
        }
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Determines whether TV mode should be active based on user setting ("AUTO", "FORCE_TV", "FORCE_MOBILE")
     * and hardware capabilities.
     */
    fun isTvMode(context: Context, userPref: String): Boolean {
        return when (userPref.uppercase()) {
            "FORCE_TV" -> true
            "FORCE_MOBILE" -> {
                // If it's real Leanback hardware, safety guard prevents trapping the user without touch controls
                if (isHardwareTv(context)) true else false
            }
            else -> isHardwareTv(context) // AUTO mode
        }
    }
}
