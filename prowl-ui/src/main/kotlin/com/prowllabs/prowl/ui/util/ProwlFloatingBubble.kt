package com.prowllabs.prowl.ui.util

import android.app.Activity
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import com.prowllabs.prowl.ui.ProwlUiLauncher
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.internal.ProwlMainActivity
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

/** Draggable circular FAB to open Prowl without notification tap. */
object ProwlFloatingBubble {
    private const val FAB_SIZE_DP = 56f
    private const val FAB_ELEVATION_DP = 10f
    private const val FAB_STROKE_DP = 2f
    private const val ICON_PADDING_DP = 11f
    private const val BRAND_PURPLE = 0xFF6C5CE7.toInt()

    private var installed = false
    private var hostApplication: Application? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private val bubbles = WeakHashMap<Activity, View>()

    fun install(application: Application) {
        if (installed) return
        installed = true
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = maybeShow(activity)
            override fun onActivityPaused(activity: Activity) = hide(activity)
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                bubbles.remove(activity)
            }
        }
        lifecycleCallbacks = callbacks
        hostApplication = application
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    fun uninstall() {
        if (!installed) return
        lifecycleCallbacks?.let { hostApplication?.unregisterActivityLifecycleCallbacks(it) }
        lifecycleCallbacks = null
        hostApplication = null
        installed = false
        bubbles.keys.toList().forEach(::hide)
        bubbles.clear()
    }

    fun refresh(activity: Activity) {
        hide(activity)
        maybeShow(activity)
    }

    private fun maybeShow(activity: Activity) {
        if (activity is ProwlMainActivity) return
        if (!ProwlUiPreferences.isFloatingBubbleEnabled(activity)) {
            hide(activity)
            return
        }

        hide(activity)

        val decor = activity.window?.decorView as? ViewGroup ?: return
        val density = activity.resources.displayMetrics.density
        val sizePx = (FAB_SIZE_DP * density).roundToInt()
        val paddingPx = (ICON_PADDING_DP * density).roundToInt()
        val strokePx = (FAB_STROKE_DP * density).roundToInt().coerceAtLeast(1)

        val bubble = ImageView(activity).apply {
            setImageResource(R.drawable.prowl_kit_white)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(BRAND_PURPLE)
                setStroke(strokePx, 0xFFFFFFFF.toInt())
            }
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = FAB_ELEVATION_DP * density
            translationZ = FAB_ELEVATION_DP * density
            contentDescription = context.getString(R.string.prowl_floating_bubble)
            setOnClickListener { ProwlUiLauncher.show(activity) }
            attachDrag(this)
        }

        val params = FrameLayout.LayoutParams(sizePx, sizePx).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            bottomMargin = (72f * density).roundToInt()
            marginEnd = (16f * density).roundToInt()
        }
        decor.addView(bubble, params)
        bubbles[activity] = bubble
    }

    private fun hide(activity: Activity) {
        val bubble = bubbles.remove(activity) ?: return
        (bubble.parent as? ViewGroup)?.removeView(bubble)
    }

    private fun attachDrag(view: View) {
        var downX = 0f
        var downY = 0f
        var startX = 0f
        var startY = 0f
        var moved = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = v.x
                    startY = v.y
                    moved = false
                    v.alpha = 0.92f
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    if (!moved) v.performClick()
                    true
                }
                else -> false
            }
        }
    }
}
