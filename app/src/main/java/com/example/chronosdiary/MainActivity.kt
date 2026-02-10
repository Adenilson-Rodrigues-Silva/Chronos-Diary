package com.example.chronosdiary

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Primeiro checamos se o hardware está ok
        if (checkDeviceCanAuthenticate()) {
            setupBiometric()
        }
    }

    private fun checkDeviceCanAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> return true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Hardware biométrico não encontrado.", Toast.LENGTH_LONG).show()
                return false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Sensor ocupado ou indisponível.", Toast.LENGTH_LONG).show()
                return false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "Nenhuma digital cadastrada.", Toast.LENGTH_LONG).show()
                return false
            }
            else -> return false
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Se o tema no XML estiver certo, aqui o sistema libera os logs
                    showChronosLogs()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Se o usuário cancelar ou der erro, o ideal é fechar para manter a segurança
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Aqui o sensor vibra e avisa que não reconheceu
                }
            })

        // ALTERE OS TEXTOS AQUI PARA O ESTILO DAS IMAGENS
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("> SYSTEM_LOCKED") // Título mais técnico
            .setSubtitle("VERIFY_IDENTITY_SCAN_REQUIRED") // Subtítulo estilo terminal
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setConfirmationRequired(false) // Deixa o processo mais rápido e fluido
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Função que realmente "monta" o app após a digital
    private fun showChronosLogs() {
        val fakeNotes = listOf(
            Note(1, "10 FEV 2026", "Log de sistema: Primeira entrada no diário Chronos."),
            Note(2, "09 FEV 2026", "A segurança biométrica foi implementada com sucesso hoje."),
            Note(3, "08 FEV 2026", "Planejamento da interface cyberpunk concluído.")
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_notes)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_note)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = NoteAdapter(fakeNotes)

        // Torna os elementos visíveis
        recyclerView.visibility = View.VISIBLE
        fab.visibility = View.VISIBLE

        Toast.makeText(this, "Logs Recuperados!", Toast.LENGTH_SHORT).show()
    }

}