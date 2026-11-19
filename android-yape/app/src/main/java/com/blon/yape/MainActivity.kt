package com.blon.yape

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        info = TextView(this).apply { textSize = 14f }
        caja.addView(info)
        caja.addView(Button(this).apply {
            text = "Actualizar estado"
            setOnClickListener { mostrarEstado() }
        })
        caja.addView(Button(this).apply {
            text = "Abrir acceso a notificaciones"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        setContentView(caja)
        mostrarEstado()
    }

    override fun onResume() {
        super.onResume()
        if (::info.isInitialized) mostrarEstado()
    }

    private fun mostrarEstado() {
        val p = getSharedPreferences("estado", MODE_PRIVATE)
        val acceso = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        info.text = "Acceso a notificaciones: " + (if (acceso) "ACTIVADO" else "DESACTIVADO") +
            "\n\nÚltima notificación de Yape recibida:\n" + p.getString("ultimaNotif", "ninguna todavía") +
            "\n\nÚltimo envío a la tienda:\n" + p.getString("ultimoEnvio", "ninguno todavía")
    }
}
