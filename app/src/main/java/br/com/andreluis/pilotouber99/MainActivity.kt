package br.com.andreluis.pilotouber99

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PilotPreferences
    private lateinit var tvStatusBadge: TextView
    private lateinit var btnTogglePilot: Button
    private lateinit var switchCalibration: Switch
    private lateinit var switchVoice: Switch
    private lateinit var switchCityFilter: Switch
    private lateinit var etMinRate: EditText
    private lateinit var tvStats: TextView
    private lateinit var tvLogPreview: TextView
    private lateinit var tvPermAccessibilityStatus: TextView
    private lateinit var tvPermOverlayStatus: TextView
    private lateinit var tvDiagnosticTts: TextView
    private lateinit var tvDiagnosticLastApp: TextView
    private lateinit var btnSimulateTest: Button
    private lateinit var tvTestFeedback: TextView
    private var testVoice: VoiceAnnouncer? = null
    private val cityCheckboxes = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PilotPreferences(this)
        testVoice = VoiceAnnouncer(this)

        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        btnTogglePilot = findViewById(R.id.btnTogglePilot)
        switchCalibration = findViewById(R.id.switchCalibration)
        switchVoice = findViewById(R.id.switchVoice)
        switchCityFilter = findViewById(R.id.switchCityFilter)
        etMinRate = findViewById(R.id.etMinRate)
        tvStats = findViewById(R.id.tvStats)
        tvLogPreview = findViewById(R.id.tvLogPreview)
        tvPermAccessibilityStatus = findViewById(R.id.tvPermAccessibilityStatus)
        tvPermOverlayStatus = findViewById(R.id.tvPermOverlayStatus)
        tvDiagnosticTts = findViewById(R.id.tvDiagnosticTts)
        tvDiagnosticLastApp = findViewById(R.id.tvDiagnosticLastApp)
        btnSimulateTest = findViewById(R.id.btnSimulateTest)
        tvTestFeedback = findViewById(R.id.tvTestFeedback)

        switchCalibration.isChecked = prefs.calibrationMode
        switchVoice.isChecked = prefs.voiceEnabled
        switchCityFilter.isChecked = prefs.cityFilterEnabled
        etMinRate.setText(String.format(Locale.US, "%.2f", prefs.minRatePerKm))

        buildCityCheckboxes()

        btnTogglePilot.setOnClickListener {
            val hasOverlay = Settings.canDrawOverlays(this)
            val hasAccessibility = isAccessibilityEnabled(this, PilotAccessibilityService::class.java)

            if (!hasOverlay) {
                Toast.makeText(this, "Atenção: Ative a permissão 'Sobrepor Outros Apps' primeiro!", Toast.LENGTH_LONG).show()
                val uri = Uri.parse("package:$packageName")
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
                return@setOnClickListener
            }

            if (!hasAccessibility) {
                Toast.makeText(this, "Atenção: Ative o 'Piloto Uber/99' em Acessibilidade!", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            prefs.pilotEnabled = !prefs.pilotEnabled
            if (prefs.pilotEnabled) {
                try {
                    startForegroundService(Intent(this, OverlayService::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao iniciar overlay: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    stopService(Intent(this, OverlayService::class.java))
                } catch (_: Exception) {}
            }
            refreshUi()
        }

        btnSimulateTest.setOnClickListener {
            testVoice?.playBeep()
            val simulatedText = "Boa! Dois e trinta o km, setenta e cinco a hora"
            testVoice?.speak(simulatedText)
            val ttsStatus = VoiceAnnouncer.realStatus
            Toast.makeText(this, "TTS: $ttsStatus", Toast.LENGTH_LONG).show()
            tvTestFeedback.text = "✅ Bip tocado! Status TTS: $ttsStatus"
            val timeNow = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
            prefs.lastDecision = "ACEITAR"
            prefs.lastRateText = "R$ 2,30/km  ·  R$ 75/h"
            prefs.lastDetailsText = "99 · R$ 42,00 · 18,2 km · 33 min [$timeNow]"
            prefs.lastReadSummary = "SIMULAÇÃO: 99 · R$ 2,30/km · R$ 75/h [$timeNow]"
            if (Settings.canDrawOverlays(this)) {
                try {
                    startService(Intent(this, OverlayService::class.java))
                } catch (_: Exception) {}
            }
            refreshUi()
        }

        switchCalibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.calibrationMode = isChecked
            refreshUi()
        }

        switchVoice.setOnCheckedChangeListener { _, isChecked ->
            prefs.voiceEnabled = isChecked
            val msg = if (isChecked) "Voz ATIVADA" else "Voz DESLIGADA (Modo Silencioso com Passageiro)"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        switchCityFilter.setOnCheckedChangeListener { _, isChecked ->
            prefs.cityFilterEnabled = isChecked
        }

        findViewById<Button>(R.id.btnRateMinus).setOnClickListener {
            val next = maxOf(0.50, prefs.minRatePerKm - 0.10)
            prefs.minRatePerKm = next
            etMinRate.setText(String.format(Locale.US, "%.2f", next))
        }

        findViewById<Button>(R.id.btnRatePlus).setOnClickListener {
            val next = prefs.minRatePerKm + 0.10
            prefs.minRatePerKm = next
            etMinRate.setText(String.format(Locale.US, "%.2f", next))
        }

        findViewById<Button>(R.id.btnSaveRate).setOnClickListener {
            val parsed = etMinRate.text.toString().replace(",", ".").toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                prefs.minRatePerKm = parsed
                Toast.makeText(this, "Mínimo salvo: R$ %.2f/km".format(parsed), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnMarkAllCities).setOnClickListener {
            cityCheckboxes.values.forEach { it.isChecked = true }
            saveCitiesFromCheckboxes()
        }

        findViewById<Button>(R.id.btnClearAllCities).setOnClickListener {
            cityCheckboxes.values.forEach { it.isChecked = false }
            saveCitiesFromCheckboxes()
        }

        findViewById<Button>(R.id.btnResetStats).setOnClickListener {
            prefs.resetDay()
            refreshUi()
        }

        findViewById<Button>(R.id.btnPermAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnPermOverlay).setOnClickListener {
            val uri = Uri.parse("package:$packageName")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
        }

        findViewById<Button>(R.id.btnPermBattery).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun buildCityCheckboxes() {
        val container = findViewById<LinearLayout>(R.id.cityCheckboxContainer)
        val saved = prefs.allowedCities
        for (city in CityFilter.ALL_CITIES) {
            val cb = CheckBox(this).apply {
                text = city
                setTextColor(Color.parseColor("#F8FAFC"))
                isChecked = city in saved
                setOnCheckedChangeListener { _, _ -> saveCitiesFromCheckboxes() }
            }
            cityCheckboxes[city] = cb
            container.addView(cb)
        }
    }

    private fun saveCitiesFromCheckboxes() {
        val selected = cityCheckboxes.filterValues { it.isChecked }.keys.toSet()
        prefs.allowedCities = selected
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onDestroy() {
        testVoice?.shutdown()
        testVoice = null
        super.onDestroy()
    }

    private fun isAccessibilityEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true) ||
                componentName.contains(context.packageName)
            ) {
                return true
            }
        }
        return false
    }

    private fun refreshUi() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityEnabled(this, PilotAccessibilityService::class.java)
        val serviceActive = PilotAccessibilityService.isConnected

        if (hasAccessibility && serviceActive) {
            tvPermAccessibilityStatus.text = "🟢 Acessibilidade: CONECTADA E ATIVA"
            tvPermAccessibilityStatus.setTextColor(Color.parseColor("#4ADE80"))
        } else if (hasAccessibility && !serviceActive) {
            tvPermAccessibilityStatus.text = "🟡 Acessibilidade: PERMITIDA (Aguardando serviço iniciar)"
            tvPermAccessibilityStatus.setTextColor(Color.parseColor("#FACC15"))
        } else {
            tvPermAccessibilityStatus.text = "🔴 Acessibilidade: DESATIVADA (Clique abaixo para ativar)"
            tvPermAccessibilityStatus.setTextColor(Color.parseColor("#F87171"))
        }

        if (hasOverlay) {
            tvPermOverlayStatus.text = "🟢 Sobreposição (Overlay): PERMITIDA"
            tvPermOverlayStatus.setTextColor(Color.parseColor("#4ADE80"))
        } else {
            tvPermOverlayStatus.text = "🔴 Sobreposição (Overlay): BLOQUEADA (Clique abaixo para permitir)"
            tvPermOverlayStatus.setTextColor(Color.parseColor("#F87171"))
        }

        tvDiagnosticTts.text = "🔊 Motor TTS: ${VoiceAnnouncer.realStatus}"
        tvDiagnosticLastApp.text = "📱 Último app visto: ${prefs.lastPackageSeen}"

        val state = if (prefs.pilotEnabled) "LIGADO" else "DESLIGADO"
        val mode = if (prefs.calibrationMode) "MODO CALIBRAÇÃO (NÃO ACEITA)" else "ACEITE AUTOMÁTICO"
        tvStatusBadge.text = "$state · $mode"
        btnTogglePilot.text = if (prefs.pilotEnabled) "DESLIGAR PILOTO" else "LIGAR PILOTO"
        tvStats.text = String.format(
            Locale("pt", "BR"),
            "Aceitas: %d | Recusadas: %d\nTotal do dia: R$ %.2f | Média: R$ %.2f/km",
            prefs.acceptedCount,
            prefs.rejectedCount,
            prefs.estimatedEarnings,
            prefs.averageRate()
        )
        val history = prefs.getRideHistory()
        val historyText = if (history.isEmpty()) {
            "Nenhuma corrida registrada hoje ainda."
        } else {
            history.take(5).joinToString("\n") { h ->
                val status = if (h.accepted) "✅ ACEITA" else "❌ NEGADA"
                "[${h.timestamp}] ${h.appName}: R$ ${String.format(Locale("pt", "BR"), "%.2f", h.fareBrl)} (${String.format(Locale("pt", "BR"), "%.1f", h.totalKm)} km, ${h.totalMinutes} min) -> R$ ${String.format(Locale("pt", "BR"), "%.2f", h.ratePerKm)}/km · $status"
            }
        }
        tvLogPreview.text = "Última leitura: ${prefs.lastReadSummary}\n\n--- HISTÓRICO RECENTE ---\n$historyText"
    }
}
