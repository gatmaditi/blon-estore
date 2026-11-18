package com.blon.yape

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        caja.addView(TextView(this).apply {
            text = "Yape Avisos\n\nPaso 1: tocá el botón y activá \"Yape Avisos\" en la lista.\n\nPaso 2: volvé a esta app. Cuando llegue un Yape, se guarda solo."
            textSize = 16f
        })
        caja.addView(Button(this).apply {
            text = "Abrir acceso a notificaciones"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        setContentView(caja)
    }
}
