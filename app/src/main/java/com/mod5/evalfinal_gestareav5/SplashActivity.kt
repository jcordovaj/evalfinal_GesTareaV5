package com.mod5.evalfinal_gestareav5

// Librerías
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
            // El 'Intent' lanza la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Finalizamos la SplashActivity para sacarla del backstack
            finish()
        }

        //  Creamos un delay de 3 segs para el botón "Continuar, así pasa igual a la vista de lista
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}