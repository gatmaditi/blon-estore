package com.blon.yape

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var raiz: LinearLayout
    private var dens = 1f

    private val colorFondo = Color.parseColor("#121212")
    private val colorTarjeta = Color.parseColor("#1E1E24")
    private val colorBoton = Color.parseColor("#2A2A33")
    private val colorTexto = Color.parseColor("#F2F2F2")
    private val colorSuave = Color.parseColor("#9E9E9E")
    private val colorVerde = Color.parseColor("#1B5E20")
    private val colorRojo = Color.parseColor("#B71C1C")
    private val colorOk = Color.parseColor("#81C784")
    private val colorMal = Color.parseColor("#EF9A9A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dens = resources.displayMetrics.density
        window.statusBarColor = colorFondo

        val scroll = ScrollView(this).apply { setBackgroundColor(colorFondo) }
        raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(20))
        }
        scroll.addView(raiz)
        setContentView(scroll)
        dibujar()
    }

    override fun onResume() {
        super.onResume()
        if (::raiz.isInitialized) dibujar()
    }

    private fun dp(v: Int) = (v * dens).toInt()

    private fun tarjeta(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(14).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(12) }
    }

    private fun texto(t: String, size: Float = 14f, color: Int = colorTexto, negrita: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (negrita) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun boton(t: String, accion: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(colorTexto)
        background = GradientDrawable().apply {
            setColor(colorBoton)
            cornerRadius = dp(10).toFloat()
        }
        setOnClickListener { accion() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(10) }
    }

    private fun dibujar() {
        raiz.removeAllViews()
        val p = getSharedPreferences("estado", MODE_PRIVATE)
        val acceso = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true

        raiz.addView(texto("Yape Avisos", 24f, colorTexto, true))
        raiz.addView(texto("Conecta tus pagos de Yape con la tienda", 13f, colorSuave))

        // Estado: rojo si falta activar, verde si todo está bien
        val estado = tarjeta(if (acceso) colorVerde else colorRojo)
        estado.addView(texto(if (acceso) "✅ Todo listo" else "❌ Falta activar", 18f, Color.WHITE, true))
        estado.addView(texto(
            if (acceso) "La app está leyendo tus avisos de Yape."
            else "Toca \"Abrir acceso\" y activa Yape Avisos en la lista.",
            14f, Color.WHITE
        ))
        raiz.addView(estado)

        raiz.addView(boton("Abrir acceso a notificaciones") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        raiz.addView(boton("Actualizar") { dibujar() })

        // Última notificación y último envío
        val info = tarjeta(colorTarjeta)
        info.addView(texto("Última notificación de Yape", 12f, colorSuave, true))
        info.addView(texto(p.getString("ultimaNotif", "ninguna todavía") ?: "", 14f))
        info.addView(texto("Último envío a la tienda", 12f, colorSuave, true))
        info.addView(texto(p.getString("ultimoEnvio", "ninguno todavía") ?: "", 14f))
        raiz.addView(info)

        // Historial de pagos
        raiz.addView(texto("Historial de pagos", 17f, colorTexto, true))
        val historial = tarjeta(colorTarjeta)
        val filas = Registro.leer(this)
        if (filas.isEmpty()) {
            historial.addView(texto("Todavía no hay pagos registrados.", 14f, colorSuave))
        }
        for (fila in filas) {
            val partes = fila.split("|")
            if (partes.size < 3) continue
            val resultado = partes[2]
            val color = when {
                resultado == "Enviado" -> colorOk
                resultado.startsWith("Repetido") -> colorSuave
                else -> colorMal
            }
            historial.addView(texto("Bs ${partes[1]}   ·   ${partes[0]}   ·   $resultado", 14f, color))
        }
        raiz.addView(historial)
    }
}
