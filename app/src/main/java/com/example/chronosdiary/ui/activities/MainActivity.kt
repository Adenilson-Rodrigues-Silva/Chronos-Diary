package com.example.chronosdiary.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chronosdiary.data.model.Note
import com.example.chronosdiary.ui.adapters.NoteAdapter
import com.example.chronosdiary.R
import com.example.chronosdiary.utils.VoiceHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()

    private lateinit var voiceHelper: VoiceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Pedir permissão de áudio
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        // 1. Checar biometria
        if (checkDeviceCanAuthenticate()) {
            setupBiometric()
        }
    }

    private fun checkDeviceCanAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> return true
            else -> {
                Toast.makeText(
                    this,
                    "Biometria necessária para acessar o Chronos.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    showChronosLogs()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finish()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("> SYSTEM_LOCKED")
            .setSubtitle("VERIFY_IDENTITY_SCAN_REQUIRED")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showChronosLogs() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_notes)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_note)

        if (notesList.isEmpty()) {
            notesList.add(Note(1, "10 FEV 2026", "Sistema Chronos Inicializado."))
        }

        noteAdapter = NoteAdapter(notesList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = noteAdapter

        recyclerView.visibility = View.VISIBLE
        fab.visibility = View.VISIBLE

        fab.setOnClickListener {
            showVoiceSheet()
        }
    }

    private fun addNewLog(text: String) {

        val sdf =
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.forLanguageTag("pt-BR"))

        val currentDate = sdf.format(java.util.Date()).uppercase()

        val newNote = Note(notesList.size + 1, currentDate, text)
        notesList.add(0, newNote)
        // ... resto do código


        notesList.add(0, newNote)
        noteAdapter.notifyItemInserted(0)
        findViewById<RecyclerView>(R.id.recycler_notes).scrollToPosition(0)
    }

    private fun showVoiceSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_voice_capture, null)
        dialog.setContentView(view)

        //val partialTv = view.findViewById<android.widget.TextView>(R.id.partial_text_view)
        //val statusTv = view.findViewById<android.widget.TextView>(R.id.status_text)
        val lottieVisualizer =
            view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_visualizer)
        val lottieMic = view.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottieMic)

        voiceHelper = VoiceHelper(
            this,
            onResult = { textoFinal ->
                runOnUiThread {
                    if (textoFinal.isNotEmpty()) {
                       // partialTv.visibility = View.VISIBLE
                        //partialTv.text = textoFinal
                        addNewLog(textoFinal)
                    }
                    lottieVisualizer.cancelAnimation()
                    // Pequeno delay para a pessoa ver a animação de "save" antes de fechar
                    view.postDelayed({ dialog.dismiss() }, 1000)
                }
            },
            onStatusChange = { status ->
                runOnUiThread {
                    when (status) {
                        "LISTENING" -> {
                          //  statusTv.text = "> SYSTEM_ACTIVE: LISTENING..."
                           // statusTv.setTextColor(ContextCompat.getColor(this, R.color.neon_green))

                            lottieVisualizer.visibility = View.VISIBLE

                            lottieVisualizer.scaleX = 0.5f
                            lottieVisualizer.scaleY = 0.8f



                            lottieVisualizer.playAnimation()

                            lottieMic.setAnimation(R.raw.mic_verde_test_2)
                            lottieMic.playAnimation()
                        }

                        "PROCESSING" -> {
                          //  statusTv.text = "> PROCESSING AUDIO..."
                           // statusTv.setTextColor(android.graphics.Color.CYAN)

                            lottieVisualizer.cancelAnimation()
                            lottieVisualizer.visibility = View.GONE

                            // AQUI: Troca para a animação de SALVAR/NOTAS
                            lottieMic.setAnimation(R.raw.save_note)
                            lottieMic.playAnimation()
                            lottieMic.loop(false) // Geralmente save_note roda só uma vez
                        }
                        // ... DONE e ERROR permanecem iguais
                    }
                }
            }
        )

        lottieMic.setOnClickListener {
            if (::voiceHelper.isInitialized) {
                // No clique, apenas paramos. O "onStatusChange" cuidará da troca visual
                voiceHelper.stopAndSend()
            }
        }

        dialog.show()
        view.postDelayed({ voiceHelper.startListening() }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Isso garante que, se você fechar o app, o microfone e as tarefas
        // em segundo plano do VoiceHelper sejam encerradas imediatamente.
        if (::voiceHelper.isInitialized) {
            voiceHelper.destroy()
        }
    }

    private fun startPulseAnimation(view: View, scale: Float) {
        val scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, scale, 1f)
        val scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, scale, 1f)

        scaleX.repeatCount = android.animation.ValueAnimator.INFINITE
        scaleY.repeatCount = android.animation.ValueAnimator.INFINITE

        android.animation.AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 1000
            start()
        }
    }

}