package br.com.andreluis.pilotouber99

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
    private lateinit var switchCityFilter: Switch
    private lateinit var etMinRate: EditText
    private lateinit var tvStats: TextView
    private lateinit var tvLogPreview: TextView
    private val cityCheckboxes = mutableMapOf<String, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PilotPreferences(this)

        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        btnTogglePilot = findViewById(R.id.btnTogglePilot)
        switchCalibration = findViewById(R.id.switchCalibration)
        switchCityFilter = findViewById(R.id.switchCityFilter)
        etMinRate = findViewById(R.id.etMinRate)
        tvStats = findViewById(R.id.tvStats)
        tvLogPreview = findViewById(R.id.tvLogPreview)

        switchCalibration.isChecked = prefs.calibrationMode
        switchCityFilter.isChecked = prefs.cityFilterEnabled
        etMinRate.setText(String.format(Locale.US, "%.2f", prefs.minRatePerKm))

        buildCityCheckboxes()

        btnTogglePilot.setOnClickListener {
            prefs.pilotEnabled = !prefs.pilotEnabled
            if (prefs.pilotEnabled) {
                startForegroundService(Intent(this, OverlayService::class.java))
            } else {
                stopService(Intent(this, OverlayService::class.java))
            }
            refreshUi()
        }

        switchCalibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.calibrationMode = isChecked
            refreshUi()
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

    private fun refreshUi() {
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
        tvLogPreview.text = "Última leitura: ${prefs.lastReadSummary}"
    }
}
