package com.prowllabs.prowl.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.prowllabs.prowl.core.runtime.ProwlRuntime

data class ProwlEnvironmentInfo(
    val appName: String,
    val appVersion: String,
    val minimumOs: String,
    val osVersion: String,
    val screenSize: String,
)

object ProwlEnvironment {
    fun collect(fallbackContext: Context): ProwlEnvironmentInfo {
        val context = ProwlRuntime.hostApplicationContext() ?: fallbackContext.applicationContext
        val pm = context.packageManager
        val pkg = context.packageName
        val appInfo = pm.getApplicationInfo(pkg, 0)
        val appName = pm.getApplicationLabel(appInfo).toString()
        val version = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
        }.getOrNull()
        val versionName = version?.versionName ?: "1.0"
        val build = if (version != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            version.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            version?.versionCode?.toString() ?: "1"
        }

        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val screen = "${metrics.widthPixels} x ${metrics.heightPixels}"

        return ProwlEnvironmentInfo(
            appName = appName,
            appVersion = "$versionName ($build)",
            minimumOs = minimumOsLabel(appInfo.minSdkVersion),
            osVersion = "Android ${Build.VERSION.RELEASE}",
            screenSize = screen,
        )
    }

    private fun minimumOsLabel(minSdk: Int): String {
        val release = when (minSdk) {
            21 -> "5.0"
            22 -> "5.1"
            23 -> "6.0"
            24 -> "7.0"
            25 -> "7.1"
            26 -> "8.0"
            27 -> "8.1"
            28 -> "9"
            29 -> "10"
            30 -> "11"
            31 -> "12"
            32 -> "12L"
            33 -> "13"
            34 -> "14"
            35 -> "15"
            else -> null
        }
        return if (release != null) "Android $release (API $minSdk)" else "API $minSdk"
    }
}
