package br.com.andreluis.pilotouber99

import android.content.Context
import android.content.Intent

object AppSwitcher {
    const val PKG_UBER = "com.ubercab.driver"
    const val PKG_99_MAIN = "com.app99.driver"
    const val PKG_99_ALT = "com.taxis99.driver"

    fun resolveOtherAppPackage(context: Context, acceptedPackage: String): String? {
        val pm = context.packageManager
        return if (acceptedPackage == PKG_UBER) {
            if (pm.getLaunchIntentForPackage(PKG_99_MAIN) != null) PKG_99_MAIN
            else if (pm.getLaunchIntentForPackage(PKG_99_ALT) != null) PKG_99_ALT
            else null
        } else {
            if (pm.getLaunchIntentForPackage(PKG_UBER) != null) PKG_UBER else null
        }
    }

    fun switchApp(context: Context, targetPackage: String, action: String, prefs: PilotPreferences) {
        prefs.pendingSwitchPackage = targetPackage
        prefs.pendingSwitchAction = action
        val intent = context.packageManager.getLaunchIntentForPackage(targetPackage) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
    }
}
