package com.nguyendevs.ecolens.managers

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SpeakerManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "SpeakerManager"
        private const val RATE_NORMAL = 1.0f
        private const val RATE_VIETNAMESE = 1.05f

        private val REGEX_PARENTHESES_VN = Regex("\\s*\\([^)]*[ạ-ỹĂăÂâĐđÊêÔôƠơƯư][^)]*\\)")
        private val REGEX_TAXONOMY = Regex("\\s*\\([^)]*[Họ|Chi|Loài][^)]*\\)")
        private val REGEX_SYMBOLS = Regex("[*#_~]")
        private val REGEX_SPECIAL_CHARS = Regex("[\\p{So}]")
        private val REGEX_WHITESPACE = Regex("\\s+")
        private val REGEX_SENTENCE_SPLIT = Regex("(?<=[.!?\\n])\\s+")
    }

    private var currentRawText: String? = null
    private var currentSentenceIndex = 0
    private var isPaused = false
    private var isLoaded = false
    private var sentenceList: List<String> = emptyList()
    private var textToSpeech: TextToSpeech? = null

    var onSpeechFinished: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(appContext, this)
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
            val languageManager = LanguageManager(appContext)
            val currentLang = languageManager.getLanguage()
            setLanguage(currentLang)
            isLoaded = true
        } else {
            Log.e(TAG, "Khởi tạo TTS thất bại")
        }
    }

    fun setLanguage(langCode: String) {
        val locale = Locale(langCode)
        val result = textToSpeech?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language $langCode not supported")
        } else {
            if (langCode == "vi") {
                textToSpeech?.setSpeechRate(RATE_VIETNAMESE)
                Log.d(TAG, "Đã set tốc độ $RATE_VIETNAMESE cho Tiếng Việt")
            } else {
                textToSpeech?.setSpeechRate(RATE_NORMAL)
                Log.d(TAG, "Đã set tốc độ $RATE_NORMAL cho ngôn ngữ khác")
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

        result = result.replace(REGEX_PARENTHESES_VN, "")
            .replace(REGEX_TAXONOMY, "")
            .replace(REGEX_SYMBOLS, "")
            .replace(REGEX_SPECIAL_CHARS, "")
            .replace(REGEX_WHITESPACE, " ")
            
        return result.trim()
    }

    private fun splitTextToSentences(text: String): List<String> {
        return text.split(REGEX_SENTENCE_SPLIT)
            .filter { it.isNotBlank() }
    }
}