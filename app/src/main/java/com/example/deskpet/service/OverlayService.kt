package com.example.deskpet.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.BuildVersion�mort android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import com.example.deskpet.DeskPetApp
import com.example.deskpet.MainActivity
import com.example.deskpet.R
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var webView: WebView
    private lateinit var engine: PetEngine

    private var params: WindowManager.LayoutParams? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialX = 0
    private var initialY = 0
    private var touchStartTime = 0L
    private var touchCount = 0
    private var lastTouchTime = 0L
    private var isDragging = false

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        engine = PetEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::overlayView.isInitialized) {
            createOverlay()
        }
        startForeground(NOTIFICATION_ID, createNotification())
        engine.start()
        return START_STICKY
    }

    @SuppressLint("ClickAbleViewAccessibility", "SetJavaScriptEnabled")
    private fun createOverlay() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        overlayView = View.inflate(this, R.layout.overlay_pet, null)
        webView = overlayView.findViewById(R.id.pet_webview)

        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
        }

        webView.addJavascriptInterface(PetJsBridge(engine), "Android")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                engine.onWebViewReady()
            }
        }

        val petHtmlPath = "file:///android_asset/pet.html"
        webView.loadUrl(petHtmlPath)

        webView.setOnTouchListener { _view, event ->
            handleTouch(event)
            true
        }

        val petSize = (screenWidth * 0.2f).toInt().coerceIn(120, 200)

        params = WindowManager.LayoutParams(
            petSize,
            petSize,
            if (BuildVersion.SDK_INT >= BuildVersion.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - petSize - 20
            y = (screenHeight * 0.15f).toInt()
        }

        windowManager.addView(overlayView, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouch(event: MotionEvent): Boolean {
        params ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                initialX = params!!.x
                initialY = params!!.y
                touchStartTime = System.currentTimeMillis()
                isDragging = false

                val now = System.currentTimeMillis()
                if (now - lastTouchTime < 500) {
                    touchCount++
                } else {
                    touchCount = 1
                }
                lastTouchTime = now
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastTouchX
                val dy = event.rawY - lastTouchY
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    isDragging = true
                    params!!.x = (initialX + dx).toInt()
                    params!!.y = (initialY + dy).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val elapsed = System.currentTimeMillis() - touchStartTime
                    if (elapsed < 300 && touchCount >= 3) {
                        engine.onPetTouched(touchCount)
                        touchCount = 0
                    }
                }
            }
        }
        return false
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, DeskPetApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("挄首尔物")
            .setContentText("小特版正在盘用")
            .setSmallIcon(R.drawable.ic_pet_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        engine.stop()
        if (this::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}