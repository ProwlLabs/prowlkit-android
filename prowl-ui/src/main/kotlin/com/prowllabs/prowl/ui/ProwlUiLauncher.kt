package com.prowllabs.prowl.ui

import android.content.Context
import android.content.Intent
import com.prowllabs.prowl.ui.internal.ProwlMainActivity

object ProwlUiLauncher {
    fun createIntent(context: Context): Intent =
        Intent(context, ProwlMainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun show(context: Context) {
        context.startActivity(createIntent(context))
    }
}
