package br.com.andreluis.pilotouber99

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceAnnouncer(private val context: Context) : TextToSpeech.OnInitListener {
    companion object {
        var realStatus: String = "INICIALIZANDO"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pendingQueue = mutableListOf<String>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var res = tts?.setLanguage(Locale("pt", "BR"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                val defLocale = Locale.getDefault()
                res = tts?.setLanguage(defLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    realStatus = "ERRO: idioma não suportado"
                    ready = false
                } else {
                    realStatus = "PRONTO (${defLocale.language})"
                    ready = true
                }
            } else {
                realStatus = "PRONTO (pt-BR)"
                ready = true
            }

            if (ready) {
                // Força canal de mídia (STREAM_MUSIC) para falar mesmo no silencioso/Não Perturbe
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                tts?.setSpeechRate(1.25f) // Fala rápida e sem enrolação

                synchronized(pendingQueue) {
                    for (msg in pendingQueue) {
                        speakInternal(msg)
                    }
                    pendingQueue.clear()
                }
            }
        } else {
            realStatus = "ERRO: init status=$status"
            Log.e("VoiceAnnouncer", "Erro ao inicializar TTS status: $status")
        }
    }

    fun playBeep() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}
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
            requestTransientFocus()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "piloto_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("VoiceAnnouncer", "Falha speak: ${e.message}")
        }
    }

    private fun requestTransientFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}
