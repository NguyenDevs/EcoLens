package com.nguyendevs.ecolens.managers

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SpeakerManager(context: Context) : TextToSpeech.OnInitListener {

    private val RATE_NORMAL = 1.0f
    private val RATE_VIETNAMESE = 1.05f

    private var currentRawText: String? = null
    private var currentSentenceIndex = 0
    private var isPaused = false
    private var isLoaded = false
    private var sentenceList: List<String> = emptyList()
    private var textToSpeech: TextToSpeech? = null

    var onSpeechFinished: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context, this)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == "SILENCE_PREFIX") return

                currentSentenceIndex++
                if (currentSentenceIndex < sentenceList.size) {
                    if (!isPaused) {
                        speakCurrentSentence(TextToSpeech.QUEUE_FLUSH)
                    }
                } else {
                    currentSentenceIndex = 0
                    onSpeechFinished?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {}
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setLanguage("vi")
            isLoaded = true
        } else {
            Log.e("SpeakerManager", "Init failed")
        }
    }

    fun setLanguage(langCode: String) {
        val locale = Locale(langCode)
        val result = textToSpeech?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("SpeakerManager", "Language $langCode not supported")
        } else {
            if (langCode == "vi") {
                textToSpeech?.setSpeechRate(RATE_VIETNAMESE)
            } else {
                textToSpeech?.setSpeechRate(RATE_NORMAL)
            }
        }
    }

    fun speak(text: String) {
        if (!isLoaded) return
        if (isPaused && text == currentRawText) {
            isPaused = false
            speakCurrentSentence(TextToSpeech.QUEUE_FLUSH)
            return
        }

        isPaused = false
        currentRawText = text

        val cleanedText = cleanupForSpeech(text)
        val newSentences = splitTextToSentences(cleanedText)

        sentenceList = newSentences
        currentSentenceIndex = 0

        if (sentenceList.isEmpty()) {
            onSpeechFinished?.invoke()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.playSilentUtterance(300, TextToSpeech.QUEUE_FLUSH, "SILENCE_PREFIX")
        }
        speakCurrentSentence(TextToSpeech.QUEUE_ADD)
    }

    fun pause() {
        if (isSpeaking()) {
            isPaused = true
            textToSpeech?.stop()
        }
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    private fun speakCurrentSentence(queueMode: Int) {
        if (currentSentenceIndex < sentenceList.size) {
            val sentence = sentenceList[currentSentenceIndex]
            textToSpeech?.speak(sentence, queueMode, null, "ID_SENTENCE_$currentSentenceIndex")
        }
    }

    private fun cleanupForSpeech(text: String): String {
        var result = text

        result = result.replace(Regex("\\s*\\([^)]*[ạ-ỹĂăÂâĐđÊêÔôƠơƯư][^)]*\\)"), "")
            .replace(Regex("\\s*\\([^)]*[Họ|Chi|Loài][^)]*\\)"), "")

        result = result.replace(Regex("[*#_~]"), "")

        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun splitTextToSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?\\n])\\s+"))
            .filter { it.isNotBlank() }
    }
}