package com.nguyendevs.ecolens.managers

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SpeakerManager(context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isLoaded = false

    private var sentenceList: List<String> = emptyList()
    private var currentSentenceIndex = 0
    private var isPaused = false

    private val RATE_NORMAL = 1.0f
    private val RATE_VIETNAMESE = 1.05f

    var onSpeechFinished: (() -> Unit)? = null

    fun setLanguage(langCode: String) {
        val locale = Locale(langCode)
        val result = textToSpeech?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("SpeakerManager", "Language $langCode not supported")
        } else {
            if (langCode == "vi") {
                textToSpeech?.setSpeechRate(RATE_VIETNAMESE)
                Log.d("SpeakerManager", "Đã set tốc độ $RATE_VIETNAMESE cho Tiếng Việt")
            } else {
                textToSpeech?.setSpeechRate(RATE_NORMAL)
                Log.d("SpeakerManager", "Đã set tốc độ $RATE_NORMAL cho ngôn ngữ khác")
            }
        }
    }

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
            // Mặc định khởi tạo là tiếng Việt
            setLanguage("vi")
            isLoaded = true
        } else {
            Log.e("SpeakerManager", "Khởi tạo TTS thất bại")
        }
    }

    fun speak(text: String) {
        if (!isLoaded) return

        isPaused = false
        val cleanedText = removeVietnameseInParentheses(text)
        val newSentences = splitTextToSentences(cleanedText)

        sentenceList = newSentences
        currentSentenceIndex = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.playSilentUtterance(300, TextToSpeech.QUEUE_FLUSH, "SILENCE_PREFIX")
        }
        speakCurrentSentence(TextToSpeech.QUEUE_ADD)
    }

    fun pause() {
        isPaused = true
        textToSpeech?.stop()
    }

    private fun speakCurrentSentence(queueMode: Int) {
        if (currentSentenceIndex < sentenceList.size) {
            val sentence = sentenceList[currentSentenceIndex]
            textToSpeech?.speak(sentence, queueMode, null, "ID_SENTENCE_$currentSentenceIndex")
        }
    }

    private fun removeVietnameseInParentheses(text: String): String {
        return text.replace(Regex("\\s*\\([^)]*[ạ-ỹĂăÂâĐđÊêÔôƠơƯư][^)]*\\)"), "")
            .replace(Regex("\\s*\\([^)]*[Họ|Chi|Loài][^)]*\\)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun splitTextToSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?\\n])\\s+"))
            .filter { it.isNotBlank() }
    }

    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}