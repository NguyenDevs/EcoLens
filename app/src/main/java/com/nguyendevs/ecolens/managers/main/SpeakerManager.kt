package com.nguyendevs.ecolens.managers.main

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.nguyendevs.ecolens.managers.setting.LanguageManager
import java.util.Locale

/** Quản lý cơ chế đọc văn bản và điều phối tốc độ phát ngôn. */
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

    /** Nạp thư viện TTS cốt lõi và cài đặt bộ lắng nghe ngữ âm. */
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

    /** Thao tác bước kế tiếp khi hệ thống giọng nói được thiết lập sẵn sàng. */
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

    /** Chuyển đổi ngôn ngữ phát và tự động hóa điều hướng vận tốc. */
    fun setLanguage(langCode: String) {
        val locale = Locale(langCode)
        val result = textToSpeech?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language $langCode not supported")
        } else {
            applySpeechRate(langCode)
        }
    }

    /** Phân phối tốc độ đọc riêng biệt tương thích đặc thù vùng. */
    private fun applySpeechRate(langCode: String) {
        if (langCode == "vi") {
            textToSpeech?.setSpeechRate(RATE_VIETNAMESE)
            Log.d(TAG, "Đã set tốc độ $RATE_VIETNAMESE cho Tiếng Việt")
        } else {
            textToSpeech?.setSpeechRate(RATE_NORMAL)
            Log.d(TAG, "Đã set tốc độ $RATE_NORMAL cho ngôn ngữ khác")
        }
    }

    /** Đẩy lệnh diễn đọc mới hoặc nối tiếp văn bản tạm dừng từ trước. */
    fun speak(text: String) {
        if (!isLoaded) return

        if (isPaused && text == currentRawText) {
            resumeSpeech()
            return
        }

        startNewSpeech(text)
    }

    /** Trở lại chu trình diễn xuất cho dòng nội dung đang dang dở. */
    private fun resumeSpeech() {
        isPaused = false
        speakCurrentSentence(TextToSpeech.QUEUE_FLUSH)
    }

    /** Cắt vỡ cụm từ ngữ và đẩy phát sinh phiên đọc mới. */
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

    /** Ngắt chừng nhẹ trước đoạn phát chống hao hụt âm thanh đầu. */
    private fun playSilencePrefix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.playSilentUtterance(
                SILENCE_DURATION,
                TextToSpeech.QUEUE_FLUSH,
                "SILENCE_PREFIX"
            )
        }
    }

    /** Đóng bằng luồng đọc nội dung. */
    fun pause() {
        if (isSpeaking()) {
            isPaused = true
            textToSpeech?.stop()
        }
    }

    /** Tra cứu tiến trình đang đọc diễn ra không. */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking == true
    }

    /** Triệt tiêu đối tượng và giải phóng hoàn toàn bộ nhớ máy đọc. */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    /** Xử lý tự động chuyển câu nói ngay luồng trước kết thúc. */
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

    /** Đưa hệ thống tiến trình về không và xuất lệnh hoàn tất. */
    private fun finishSpeech() {
        currentSentenceIndex = 0
        onSpeechFinished?.invoke()
    }

    /** Yêu cầu máy phát một mẩu phân đoạn trích sẵn. */
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

    /** Thanh lọc các ký tự đặc thù tránh trình trạng đọc dính âm lạ. */
    private fun cleanupForSpeech(text: String): String {
        var result = text

        result = result.replace(REGEX_PARENTHESES_VN, "")
            .replace(REGEX_TAXONOMY, "")
            .replace(REGEX_SYMBOLS, "")
            .replace(REGEX_SPECIAL_CHARS, "")
            .replace(REGEX_WHITESPACE, " ")

        return result.trim()
    }

    /** Bẻ nhỏ chuỗi văn bản lớn dựa vào cấu trúc câu điển hình. */
    private fun splitTextToSentences(text: String): List<String> {
        return text.split(REGEX_SENTENCE_SPLIT)
            .filter { it.isNotBlank() }
    }
}