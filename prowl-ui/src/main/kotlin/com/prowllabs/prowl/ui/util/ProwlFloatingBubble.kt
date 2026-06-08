package com.prowllabs.prowl.ui.util

import android.app.Activity
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.prowllabs.prowl.ui.ProwlUiLauncher
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.internal.ProwlMainActivity
import java.util.WeakHashMap
import kotlin.math.abs

/** Draggable in-app bubble to open Prowl without notification tap. */
object ProwlFloatingBubble {
    private var installed = false
    private val bubbles = WeakHashMap<Activity, View>()

    fun install(application: Application) {
        if (installed) return
        installed = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = maybeShow(activity)
            override fun onActivityPaused(activity: Activity) = hide(activity)
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                bubbles.remove(activity)
            }
        })
    }

    fun refresh(activity: Activity) {
        hide(activity)
        maybeShow(activity)
    }

    private fun maybeShow(activity: Activity) {
        if (activity is ProwlMainActivity) return
        if (!ProwlUiPreferences.isFloatingBubbleEnabled(activity)) return
        if (bubbles.containsKey(activity)) return

        val decor = activity.window?.decorView as? ViewGroup ?: return
        val density = activity.resources.displayMetrics.density
        val sizePx = (52 * density).toInt()

        val bubble = ImageView(activity).apply {
            setImageResource(R.drawable.ic_prowl_notification)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF6C5CE7.toInt())
            }
            elevation = 10 * density
            contentDescription = context.getString(R.string.prowl_floating_bubble)
            setOnClickListener { ProwlUiLauncher.show(activity) }
            attachDrag(activity, this)
        }

        val params = FrameLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            bottomMargin = (72 * density).toInt()
            marginEnd = (16 * density).toInt()
        }
        decor.addView(bubble, params)
        bubbles[activity] = bubble
    }

    private fun hide(activity: Activity) {
        val bubble = bubbles.remove(activity) ?: return
        (bubble.parent as? ViewGroup)?.removeView(bubble)
    }

    private fun attachDrag(activity: Activity, view: View) {
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var moved = false

        view.setOnTouchListener { v, event ->
            val params = v.layoutParams as FrameLayout.LayoutParams
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = v.x
                    startY = v.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    v.x = startX + dx
                    v.y = startY + dy
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) v.performClick()
                    true
                }
                else -> false
            }
        }
    }
}
