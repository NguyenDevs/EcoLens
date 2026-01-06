package com.nguyendevs.ecolens.managers

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manager quản lý Text-to-Speech (TTS)
 * Hỗ trợ đọc text từng câu với pause/resume và cleanup text
 * Tự động điều chỉnh tốc độ đọc theo ngôn ngữ
 */
class SpeakerManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "SpeakerManager"
        private const val RATE_NORMAL = 1.0f
        private const val RATE_VIETNAMESE = 1.05f
        private const val SILENCE_DURATION = 300L

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
        initializeTextToSpeech()
    }

    // ==================== INITIALIZATION ====================

    /**
     * Khởi tạo TextToSpeech engine và setup listeners
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(appContext, this)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                handleUtteranceDone(utteranceId)
            }

            override fun onError(utteranceId: String?) {}
        })
    }

    /**
     * Callback khi TTS engine khởi tạo xong
     */
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

    // ==================== LANGUAGE CONFIGURATION ====================

    /**
     * Set ngôn ngữ cho TTS
     * Tự động điều chỉnh speech rate cho tiếng Việt (1.05x)
     */
    fun setLanguage(langCode: String) {
        val locale = Locale(langCode)
        val result = textToSpeech?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language $langCode not supported")
        } else {
            applySpeechRate(langCode)
        }
    }

    /**
     * Áp dụng speech rate phù hợp với ngôn ngữ
     */
    private fun applySpeechRate(langCode: String) {
        if (langCode == "vi") {
            textToSpeech?.setSpeechRate(RATE_VIETNAMESE)
            Log.d(TAG, "Đã set tốc độ $RATE_VIETNAMESE cho Tiếng Việt")
        } else {
            textToSpeech?.setSpeechRate(RATE_NORMAL)
            Log.d(TAG, "Đã set tốc độ $RATE_NORMAL cho ngôn ngữ khác")
        }
    }

    // ==================== SPEECH CONTROL ====================

    /**
     * Đọc text
     * Nếu đang pause và text giống nhau, sẽ resume từ câu hiện tại
     * Nếu text mới, sẽ cleanup và split thành sentences
     */
    fun speak(text: String) {
        if (!isLoaded) return

        if (isPaused && text == currentRawText) {
            resumeSpeech()
            return
        }

        startNewSpeech(text)
    }

    /**
     * Resume speech từ câu hiện tại
     */
    private fun resumeSpeech() {
        isPaused = false
        speakCurrentSentence(TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Bắt đầu đọc text mới
     * Cleanup text, split thành sentences và bắt đầu từ câu đầu tiên
     */
    private fun startNewSpeech(text: String) {
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

        playSilencePrefix()
        speakCurrentSentence(TextToSpeech.QUEUE_ADD)
    }

    /**
     * Play silence trước khi đọc (để tránh bị cắt đầu câu)
     */
    private fun playSilencePrefix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.playSilentUtterance(
                SILENCE_DURATION,
                TextToSpeech.QUEUE_FLUSH,
                "SILENCE_PREFIX"
            )
        }
    }

    /**
     * Pause speech hiện tại
     */
    fun pause() {
        if (isSpeaking()) {
            isPaused = true
            textToSpeech?.stop()
        }
    }

    /**
     * Kiểm tra xem có đang đọc không
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    /**
     * Shutdown TTS engine
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    // ==================== UTTERANCE HANDLING ====================

    /**
     * Xử lý khi một utterance đọc xong
     * Tự động chuyển sang câu tiếp theo hoặc kết thúc
     */
    private fun handleUtteranceDone(utteranceId: String?) {
        if (utteranceId == "SILENCE_PREFIX") return

        currentSentenceIndex++
        if (currentSentenceIndex < sentenceList.size) {
            if (!isPaused) {
                speakCurrentSentence(TextToSpeech.QUEUE_FLUSH)
            }
        } else {
            finishSpeech()
        }
    }

    /**
     * Kết thúc speech và reset state
     */
    private fun finishSpeech() {
        currentSentenceIndex = 0
        onSpeechFinished?.invoke()
    }

    /**
     * Đọc câu hiện tại
     */
    private fun speakCurrentSentence(queueMode: Int) {
        if (currentSentenceIndex < sentenceList.size) {
            val sentence = sentenceList[currentSentenceIndex]
            textToSpeech?.speak(
                sentence,
                queueMode,
                null,
                "ID_SENTENCE_$currentSentenceIndex"
            )
        }
    }

    // ==================== TEXT PROCESSING ====================

    /**
     * Cleanup text trước khi đọc
     * Loại bỏ parentheses tiếng Việt, taxonomy terms, symbols, etc.
     */
    private fun cleanupForSpeech(text: String): String {
        var result = text

        result = result.replace(REGEX_PARENTHESES_VN, "")
            .replace(REGEX_TAXONOMY, "")
            .replace(REGEX_SYMBOLS, "")
            .replace(REGEX_SPECIAL_CHARS, "")
            .replace(REGEX_WHITESPACE, " ")

        return result.trim()
    }

    /**
     * Split text thành sentences dựa trên dấu câu
     */
    private fun splitTextToSentences(text: String): List<String> {
        return text.split(REGEX_SENTENCE_SPLIT)
            .filter { it.isNotBlank() }
    }
}