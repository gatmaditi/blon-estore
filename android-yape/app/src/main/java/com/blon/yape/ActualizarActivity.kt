package com.blon.yape

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Página de "Nueva actualización": se abre sola al entrar a la app si hay una versión nueva.
class ActualizarActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        fun redondo(color: Int) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
        }

        val build = intent.getIntExtra("build", 0)
        val notas = intent.getStringExtra("notas") ?: ""
        val apk = intent.getStringExtra("apk") ?: ""

        window.statusBarColor = Color.parseColor("#121212")
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#121212")) }
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }

        fun titulo(t: String, size: Float, color: Int, negrita: Boolean) = TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_HORIZONTAL
            if (negrita) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(6))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        caja.addView(titulo("🆕", 48f, Color.WHITE, false))
        caja.addView(titulo("Nueva actualización", 22f, Color.WHITE, true))
        caja.addView(titulo("Versión $build lista para instalar", 15f, Color.parseColor("#9E9E9E"), false))

        if (notas.isNotBlank()) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = redondo(Color.parseColor("#1E1E24"))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(18) }
            }
            card.addView(TextView(this).apply {
                text = "Cambios"
                textSize = 14f
                setTextColor(Color.parseColor("#9E9E9E"))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(6))
            })
            card.addView(TextView(this).apply {
                text = notas.trim().take(600)
                textSize = 14f
                setTextColor(Color.parseColor("#F2F2F2"))
            })
            caja.addView(card)
        }

        caja.addView(Button(this).apply {
            text = "Actualizar ahora"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = redondo(Color.parseColor("#1B5E20"))
            setOnClickListener {
                // Abre el enlace del APK: el celular lo descarga y luego pide instalarlo
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apk)))
                finish()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(22) }
        })
        caja.addView(Button(this).apply {
            text = "Más tarde"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = redondo(Color.parseColor("#2A2A33"))
            setOnClickListener { finish() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
        })

        scroll.addView(caja)
        setContentView(scroll)
    }
}
