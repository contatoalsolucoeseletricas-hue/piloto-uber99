package br.com.andreluis.pilotouber99

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Environment
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PilotAccessibilityService : AccessibilityService() {
    private lateinit var prefs: PilotPreferences
    private lateinit var voice: VoiceAnnouncer
    private var lastSignature: String = ""
    private var lastOfferTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PilotPreferences(this)
        voice = VoiceAnnouncer(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (!prefs.pilotEnabled) return

        val rootNodes = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { rootNodes.add(it) }
        try {
            for (w in windows) {
                w.root?.let { wRoot ->
                    if (rootNodes.none { it == wRoot }) {
                        rootNodes.add(wRoot)
                    }
                }
            }
        } catch (_: Exception) {}

        if (rootNodes.isEmpty()) return

        for (r in rootNodes) {
            handlePendingSwitchIfNeeded(pkg, r)
        }

        val nodes = mutableListOf<AccessibilityNodeInfo>()
        val texts = mutableListOf<String>()
        for (r in rootNodes) {
            collectNodes(r, nodes, texts)
        }

        if (texts.isEmpty()) return

        val isUber = pkg.contains("uber", ignoreCase = true)
        val is99 = pkg.contains("99", ignoreCase = true) || pkg.contains("didi", ignoreCase = true)
        if (!isUber && !is99) return

        val appName = if (isUber) "Uber" else "99"
        val hasOfferButtons = texts.any {
            it.equals("Aceitar", true) || it.equals("Selecionar", true)
        }
        if (!hasOfferButtons) return

        val signature = "$pkg|${texts.joinToString("|")}"
        val now = System.currentTimeMillis()
        if (signature == lastSignature && (now - lastOfferTimestamp) < 12000L) return
        lastSignature = signature
        lastOfferTimestamp = now

        if (prefs.calibrationMode) {
            saveCalibrationLog(appName, texts)
        }

        val offer = RideOfferParser.parseNodes(appName, texts)
        val result = RideRuleEvaluator.evaluate(
            offer = offer,
            minRatePerKm = prefs.minRatePerKm,
            cityFilterEnabled = prefs.cityFilterEnabled,
            allowedCities = prefs.allowedCities
        )

        if (offer == null) {
            if (prefs.calibrationMode) {
                prefs.lastReadSummary = "$appName: leitura incompleta"
                voice.speak("Calibração $appName, leitura incompleta")
            }
            return
        }

        val ptBr = Locale("pt", "BR")
        val ratePt = String.format(ptBr, "%.2f", result.ratePerKm)
        val hourInt = result.ratePerHour.toInt()
        val farePt = String.format(ptBr, "%.2f", offer.fareBrl)
        val hourPartUi = if (hourInt > 0) " · R$ $hourInt/h" else ""
        val timeNow = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        prefs.lastReadSummary = "[$timeNow] $appName R$ $farePt · R$ $ratePt/km$hourPartUi"
        startService(Intent(this, OverlayService::class.java))

        val spokenRate = formatSpokenRate(result.ratePerKm)
        val hourPartSpeech = if (hourInt > 0) ", $hourInt a hora" else ""

        val historyEntry = RideHistoryEntry(
            timestamp = timeNow,
            appName = appName,
            fareBrl = offer.fareBrl,
            totalKm = offer.totalKm,
            totalMinutes = offer.totalMinutes,
            ratePerKm = result.ratePerKm,
            ratePerHour = result.ratePerHour,
            accepted = result.accepted,
            reason = result.reason
        )
        prefs.addRideHistory(historyEntry)

        if (prefs.calibrationMode) {
            val cityNote = if (result.blockedCity != null) ", fora da rota ${result.blockedCity}" else ""
            voice.speak("Calibração $appName. $spokenRate o km$hourPartSpeech$cityNote.")
            return
        }

        prefs.recordOffer(result.accepted, offer.fareBrl, result.ratePerKm)
        if (result.accepted) {
            val clicked = clickActionButton(nodes, listOf("Aceitar", "Selecionar"))
            if (clicked) {
                voice.speak("Boa! $spokenRate o km$hourPartSpeech.")
                val otherPkg = AppSwitcher.resolveOtherAppPackage(this, pkg)
                if (otherPkg != null) {
                    AppSwitcher.switchApp(this, otherPkg, "GO_OFFLINE", prefs)
                }
            }
        } else {
            if (result.blockedCity != null) {
                voice.speak("Fora da rota! ${result.blockedCity}.")
            } else {
                voice.speak("Fraca. $spokenRate o km$hourPartSpeech.")
            }
        }
    }

    private fun formatSpokenRate(rate: Double): String {
        val rounded = String.format(Locale("pt", "BR"), "%.2f", rate)
        return rounded.replace(",", " e ")
    }

    private fun handlePendingSwitchIfNeeded(pkg: String, root: AccessibilityNodeInfo) {
        val pendingPkg = prefs.pendingSwitchPackage ?: return
        val action = prefs.pendingSwitchAction ?: return
        if (pkg != pendingPkg) return

        val nodes = mutableListOf<AccessibilityNodeInfo>()
        val texts = mutableListOf<String>()
        collectNodes(root, nodes, texts)

        val targets = if (action == "GO_OFFLINE") {
            listOf("Ficar offline", "Offline", "Desconectar")
        } else {
            listOf("Conectar", "Ficar online", "Iniciar", "Online")
        }
        if (clickActionButton(nodes, targets)) {
            prefs.pendingSwitchPackage = null
            prefs.pendingSwitchAction = null
        }
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        outNodes: MutableList<AccessibilityNodeInfo>,
        outTexts: MutableList<String>
    ) {
        outNodes.add(node)
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { outTexts.add(it) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, outNodes, outTexts) }
        }
    }

    private fun clickActionButton(nodes: List<AccessibilityNodeInfo>, labels: List<String>): Boolean {
        val target = nodes.firstOrNull { n ->
            val t = n.text?.toString()?.trim() ?: ""
            labels.any { label -> t.equals(label, ignoreCase = true) }
        } ?: return false

        var clickable: AccessibilityNodeInfo? = target
        while (clickable != null && !clickable.isClickable) {
            clickable = clickable.parent
        }
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        val rect = Rect()
        target.getBoundsInScreen(rect)
        if (rect.width() <= 0 || rect.height() <= 0) return false
        val path = Path().apply { moveTo(rect.centerX().toFloat(), rect.centerY().toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun saveCalibrationLog(appName: String, texts: List<String>) {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PilotoUber99")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "calibracao.log")
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            file.appendText("[$ts][$appName] ${texts.joinToString(" | ")}\n")
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        voice.shutdown()
        super.onDestroy()
    }
}
