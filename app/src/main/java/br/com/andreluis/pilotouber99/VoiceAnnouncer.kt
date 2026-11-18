package br.com.andreluis.pilotouber99

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pendingQueue = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("pt", "BR"))
            ready = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED)
            if (ready) {
                synchronized(pendingQueue) {
                    for (msg in pendingQueue) {
                        speakInternal(msg)
                    }
                    pendingQueue.clear()
                }
            }
        } else {
            Log.e("VoiceAnnouncer", "Erro ao inicializar TTS status: $status")
        }
    }

    fun speak(text: String) {
        if (ready) {
            speakInternal(text)
        } else {
            synchronized(pendingQueue) {
                if (pendingQueue.size < 5) {
                    pendingQueue.add(text)
                }
            }
        }
    }

    private fun speakInternal(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "piloto_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("VoiceAnnouncer", "Falha speak: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
