package com.mod5.evalfinal_gestareav5

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.jvm.java

// Pantalla de bienvenida usando un 'Intent' explícito
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Busca el layout
        setContentView(R.layout.main_splash)

        val buttonStartApp: Button = findViewById(R.id.buttonStartApp)
        // Listener del botón
        buttonStartApp.setOnClickListener {
            // Aquí el Intent explícito lanza la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Se finaliza la SplashActivity para que el usuario no pueda volver con el botón 'back'
            finish()
        }

        //  Si el usuario no presiona el botón, el splash desaparecerá igual después de 3 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)

    }
}