package com.lvngx.app

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.provider.Settings
import android.util.TypedValue

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragged = false

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Float, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius.toInt()).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun showBubble() {
        if (bubble != null) return
        val view = TextView(this).apply {
            text = "LX"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(28, 13, 24))
                setStroke(dp(2), Color.rgb(242, 13, 80))
            }
            elevation = dp(8).toFloat()
        }
        val params = WindowManager.LayoutParams(
            dp(54), dp(54),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(220) }
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragged = false; downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > dp(4) || kotlin.math.abs(dy) > dp(4)) dragged = true
                    params.x = (startX + dx).coerceIn(0, resources.displayMetrics.widthPixels - dp(54))
                    params.y = (startY + dy).coerceIn(dp(24), resources.displayMetrics.heightPixels - dp(100))
                    wm.updateViewLayout(view, params); true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) togglePanel()
                    else {
                        val midpoint = resources.displayMetrics.widthPixels / 2
                        params.x = if (params.x + dp(27) < midpoint) 0 else resources.displayMetrics.widthPixels - dp(54)
                        wm.updateViewLayout(view, params)
                    }
                    true
                }
                else -> true
            }
        }
        bubble = view; bubbleParams = params
        try { wm.addView(view, params) } catch (_: Exception) { bubble = null; stopSelf() }
    }

    private fun togglePanel() {
        if (panel != null) { removePanel(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = bg(Color.rgb(12, 15, 25), 24f, Color.rgb(105, 31, 58))
        }
        val header = TextView(this).apply {
            text = "LVNG -X"; setTextColor(Color.WHITE); textSize = 19f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(header)
        val names = listOf("GAME SPACE", "MACRO ENGINE PRO", "DISPLAY & VISUAL", "INPUT & CONTROL",
            "PERFORMANCE", "GAME TOOLS", "SMART FEATURES", "BACKUP & RESTORE", "DIAGNOSTICS", "SETTINGS")
        names.forEach { name ->
            val item = TextView(this).apply {
                text = name; textSize = 12f; setTextColor(Color.WHITE)
                setPadding(dp(12), dp(14), dp(8), dp(14))
                background = bg(Color.rgb(24, 29, 44), 13f)
                val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(8)
                layoutParams = lp
                setOnClickListener {
                    // Modules are exposed as navigation entries; unfinished engines are not falsely activated.
                    text = "$name  ·  OPEN IN APP"
                }
            }
            root.addView(item)
        }
        val close = TextView(this).apply {
            text = "TUTUP"; gravity = Gravity.CENTER; textSize = 11f
            setTextColor(Color.rgb(242, 75, 112)); setPadding(0, dp(12), 0, dp(6))
            setOnClickListener { removePanel() }
        }
        root.addView(close)
        val params = WindowManager.LayoutParams(
            dp(290), WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; x = 0; y = 0 }
        panel = root
        try { wm.addView(root, params) } catch (_: Exception) { panel = null }
    }

    private fun removePanel() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
    }

    override fun onDestroy() {
        removePanel()
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
