package com.nguyendevs.ecolens.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SpeakerManager(context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isLoaded = false

    // Biến để quản lý việc "Đọc tiếp"
    private var sentenceList: List<String> = emptyList()
    private var currentSentenceIndex = 0
    private var isPaused = false

    // Callback để thông báo cho Activity biết khi nào đọc xong hết
    var onSpeechFinished: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context, this)
        // Lắng nghe sự kiện khi đọc xong 1 câu
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                // Khi đọc xong 1 câu, tăng index và đọc câu tiếp theo
                currentSentenceIndex++
                if (currentSentenceIndex < sentenceList.size) {
                    // Nếu chưa dừng (pause) thì đọc tiếp
                    if (!isPaused) {
                        speakCurrentSentence()
                    }
                } else {
                    // Đã đọc hết danh sách -> Reset và gọi callback về UI
                    currentSentenceIndex = 0
                    onSpeechFinished?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {}
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("vi"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("SpeakerManager", "Tiếng Việt không hỗ trợ")
            } else {
                isLoaded = true
            }
        }
    }

    // Hàm gọi từ bên ngoài: Bắt đầu đọc văn bản mới hoặc resume
    fun speak(text: String) {
        if (!isLoaded) return

        isPaused = false

        // Nếu text mới khác text cũ -> Chia lại câu và reset index
        // Nếu text giống text cũ (người dùng ấn lại nút Speak) -> Đọc tiếp từ index hiện tại
        val newSentences = splitTextToSentences(text)
        if (sentenceList != newSentences) {
            sentenceList = newSentences
            currentSentenceIndex = 0
        }

        speakCurrentSentence()
    }

    // Hàm dừng tạm thời
    fun pause() {
        isPaused = true
        textToSpeech?.stop()
    }

    // Hàm đọc câu hiện tại dựa trên index
    private fun speakCurrentSentence() {
        if (currentSentenceIndex < sentenceList.size) {
            val sentence = sentenceList[currentSentenceIndex]
            // utteranceId là cần thiết để onDone hoạt động
            textToSpeech?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "ID_SENTENCE_$currentSentenceIndex")
        }
    }

    // Hàm chia văn bản thành các câu nhỏ (dựa trên dấu chấm, xuống dòng...)
    private fun splitTextToSentences(text: String): List<String> {
        // Tách dựa trên dấu chấm, chấm phẩy, xuống dòng, nhưng giữ lại dấu câu để đọc tự nhiên hơn
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