package com.nguyendevs.ecolens.manager

import android.content.Context
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

    var onSpeechFinished: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context, this)
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                currentSentenceIndex++
                if (currentSentenceIndex < sentenceList.size) {
                    if (!isPaused) {
                        speakCurrentSentence()
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
            val result = textToSpeech?.setLanguage(Locale("vi"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("SpeakerManager", "Tiếng Việt không hỗ trợ")
            } else {
                isLoaded = true
            }
        }
    }

    fun speak(text: String) {
        if (!isLoaded) return

        isPaused = false
        val cleanedText = removeVietnameseInParentheses(text)
        val newSentences = splitTextToSentences(cleanedText)
        if (sentenceList != newSentences) {
            sentenceList = newSentences
            currentSentenceIndex = 0
        }

        speakCurrentSentence()
    }

    fun pause() {
        isPaused = true
        textToSpeech?.stop()
    }

    private fun speakCurrentSentence() {
        if (currentSentenceIndex < sentenceList.size) {
            val sentence = sentenceList[currentSentenceIndex]
            // utteranceId là cần thiết để onDone hoạt động
            textToSpeech?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "ID_SENTENCE_$currentSentenceIndex")
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