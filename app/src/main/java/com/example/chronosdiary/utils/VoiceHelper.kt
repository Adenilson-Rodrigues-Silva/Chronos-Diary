package com.example.chronosdiary.utils

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.chronosdiary.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class VoiceHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onStatusChange: (String) -> Unit
) {
    // Variável que a NoteDetailActivity usa para saber se deve parar ou começar
    var isListening: Boolean = false

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO)

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val audioStream = ByteArrayOutputStream()

    fun startListening() {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            onStatusChange("ERROR_PERMISSION")
            return
        }

        audioStream.reset()
        isRecording = true
        isListening = true // Define como ouvindo para a Activity saber
        onStatusChange("LISTENING")

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        Log.d("CHRONOS_AUDIO", "Gravação iniciada")

        scope.launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    audioStream.write(buffer, 0, read)
                }
            }
        }
    }

    fun stopAndSend() {
        if (!isRecording) return
        isRecording = false
        isListening = false // Define como parado para a Activity saber

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        onStatusChange("PROCESSING")

        scope.launch {
            sendToGoogle(audioStream.toByteArray())
        }
    }

    private suspend fun sendToGoogle(audioData: ByteArray) {
        try {
            val url = URL("https://speech.googleapis.com/v1/speech:recognize?key=${BuildConfig.GOOGLE_API_KEY}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true

            val audioBase64 = Base64.encodeToString(audioData, Base64.NO_WRAP)

            val jsonRequest = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("encoding", "LINEAR16")
                    put("sampleRateHertz", 16000)
                    put("languageCode", "pt-BR")
                    put("enableAutomaticPunctuation", true)
                    put("model", "latest_long")
                    put("useEnhanced", true)
                })
                put("audio", JSONObject().apply { put("content", audioBase64) })
            }

            connection.outputStream.use { it.write(jsonRequest.toString().toByteArray()) }

            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)
            val results = jsonResponse.optJSONArray("results")

            val fullTranscript = StringBuilder()
            if (results != null) {
                for (i in 0 until results.length()) {
                    val transcript = results.getJSONObject(i)
                        .getJSONArray("alternatives")
                        .getJSONObject(0)
                        .getString("transcript")
                    fullTranscript.append(transcript).append(" ")
                }
            }
            val finalResult = fullTranscript.toString().trim()

            withContext(Dispatchers.Main) {
                onResult(finalResult)
                onStatusChange("DONE")
            }

        } catch (e: Exception) {
            Log.e("CHRONOS_CLOUD", "Erro: ${e.message}")
            withContext(Dispatchers.Main) { onStatusChange("ERROR") }
        }
    }

    fun destroy() {
        isRecording = false
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        scope.cancel()
    }
}