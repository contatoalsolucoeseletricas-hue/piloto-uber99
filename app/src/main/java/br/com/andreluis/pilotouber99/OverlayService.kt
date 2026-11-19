package br.com.andreluis.pilotouber99

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var prefs: PilotPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = PilotPreferences(this)
        startForegroundNotification()
        showOverlayIfPermitted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            showOverlayIfPermitted()
        } else {
            updateBadge()
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        try {
            val channelId = "piloto_uber99_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(channelId, "Piloto Uber/99", NotificationManager.IMPORTANCE_LOW)
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
            }
            val notif = Notification.Builder(this, channelId)
                .setContentTitle("Piloto Uber/99 ativo")
                .setContentText("Monitorando ofertas em segundo plano")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build()
            startForeground(101, notif)
        } catch (e: Exception) {
            Log.e("OverlayService", "Falha startForeground: ${e.message}")
        }
    }

    private fun showOverlayIfPermitted() {
        if (!Settings.canDrawOverlays(this) || overlayView != null) return
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager ?: return
            val view = LayoutInflater.from(this).inflate(R.layout.overlay_badge, null)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 24
                y = 180
            }
            view.setOnClickListener {
                if (!PilotAccessibilityService.isConnected) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } else {
                    prefs.pilotEnabled = !prefs.pilotEnabled
                    updateBadge()
                }
            }
            windowManager?.addView(view, params)
            overlayView = view
            updateBadge()
        } catch (e: Exception) {
            Log.e("OverlayService", "Erro seguro ao adicionar overlay no POCO/MIUI: ${e.message}")
        }
    }

    private fun updateBadge() {
        try {
            val tvDecision = overlayView?.findViewById<TextView>(R.id.tvOverlayDecision) ?: return
            val tvRate = overlayView?.findViewById<TextView>(R.id.tvOverlayRate) ?: return
            val tvDetails = overlayView?.findViewById<TextView>(R.id.tvOverlayDetails) ?: return
            val tvTotal = overlayView?.findViewById<TextView>(R.id.tvOverlayTotal) ?: return

            if (!PilotAccessibilityService.isConnected) {
                tvDecision.text = "⚠️ ACESSIBILIDADE OFF"
                tvDecision.setTextColor(Color.parseColor("#EF4444"))
                tvRate.text = "Toque para reativar"
                tvRate.setTextColor(Color.parseColor("#FCA5A5"))
                tvDetails.text = "Serviço pausado pelo sistema"
            } else {
                when (prefs.lastDecision) {
                    "ACEITAR" -> {
                        tvDecision.text = "✅ BOA · ACEITAR"
                        tvDecision.setTextColor(Color.parseColor("#22C55E"))
                    }
                    "RECUSAR" -> {
                        tvDecision.text = "❌ FRACA · RECUSAR"
                        tvDecision.setTextColor(Color.parseColor("#EF4444"))
                    }
                    else -> {
                        val state = if (prefs.pilotEnabled) "PILOTO ON" else "PILOTO OFF"
                        tvDecision.text = "$state · MONITORANDO"
                        tvDecision.setTextColor(Color.parseColor("#FACC15"))
                    }
                }
                tvRate.text = prefs.lastRateText
                tvRate.setTextColor(Color.parseColor("#FFFFFF"))
                tvDetails.text = prefs.lastDetailsText
            }

            val mode = if (prefs.calibrationMode) "CAL" else "AUTO"
            val totalCount = prefs.acceptedCount + prefs.rejectedCount
            tvTotal.text = String.format(Locale("pt", "BR"), "%s · Total dia: R$ %.2f (%d aceitas de %d)", mode, prefs.estimatedEarnings, prefs.acceptedCount, totalCount)
        } catch (e: Exception) {
            Log.e("OverlayService", "Erro updateBadge: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.e("OverlayService", "Erro seguro onDestroy removeView: ${e.message}")
        }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
