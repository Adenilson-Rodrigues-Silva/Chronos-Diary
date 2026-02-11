package com.example.chronosdiary.ui.activities // <--- ESSA LINHA É A MAIS IMPORTANTE

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.chronosdiary.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val textTitle = findViewById<TextView>(R.id.text_title)
        val btnStart = findViewById<Button>(R.id.btn_start)

        // Texto que será "digitado"
        val fullText = "CHRONOS DIARY"
        textTitle.text = "" // Começa vazio
        btnStart.visibility = View.INVISIBLE // Botão oculto no início

        // 1. Inicia a animação de digitação
        animateTyping(textTitle, fullText) {
            // 2. Quando terminar de digitar, mostra o botão com um efeito suave
            btnStart.visibility = View.VISIBLE
            btnStart.alpha = 0f
            btnStart.animate().alpha(1f).setDuration(1000).start()
        }

        // 3. Espera o clique para ir para a próxima tela (Digital)
        btnStart.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Função que faz o efeito de "Máquina de Escrever"
    private fun animateTyping(textView: TextView, text: String, onFinished: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    textView.text = text.substring(0, index)
                    index++
                    handler.postDelayed(this, 150) // Velocidade da digitação (150ms)
                } else {
                    onFinished() // Chama a função quando acaba
                }
            }
        }
        handler.post(runnable)
    }
}