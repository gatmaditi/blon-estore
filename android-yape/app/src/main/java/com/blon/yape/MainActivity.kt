package com.blon.yape

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MainActivity : Activity() {

    private class Pago(
        val monto: Double,
        val nombre: String,
        val compra: String,
        val creado: String,
        val usado: Boolean
    )

    private val PIN = "2007"

    private var dens = 1f
    private var pagos: List<Pago> = emptyList()
    private var errorCarga: String? = null
    private var desbloqueado = false

    private val colorFondo = Color.parseColor("#121212")
    private val colorTarjeta = Color.parseColor("#1E1E24")
    private val colorBoton = Color.parseColor("#2A2A33")
    private val colorTexto = Color.parseColor("#F2F2F2")
    private val colorSuave = Color.parseColor("#9E9E9E")
    private val colorVerde = Color.parseColor("#1B5E20")
    private val colorRojo = Color.parseColor("#B71C1C")
    private val colorOk = Color.parseColor("#81C784")
    private val colorMal = Color.parseColor("#EF9A9A")
    private val colorPendiente = Color.parseColor("#FFB74D")
    private val colorCargando = Color.parseColor("#FFB74D")

    private lateinit var saludo: TextView
    private lateinit var chipTienda: TextView
    private lateinit var estadoCard: LinearLayout
    private lateinit var infoNotif: TextView
    private lateinit var infoEnvio: TextView
    private lateinit var infoTitular: TextView
    private lateinit var cargaTexto: TextView
    private lateinit var buscador: EditText
    private lateinit var lista: LinearLayout
    private lateinit var botonHistorial: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dens = resources.displayMetrics.density
        window.statusBarColor = colorFondo

        val scroll = ScrollView(this).apply { setBackgroundColor(colorFondo) }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(20))
        }
        scroll.addView(raiz)
        setContentView(scroll)

        // Encabezado: título y saludo a la izquierda; tienda vinculada a la derecha
        val cab = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val izq = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        izq.addView(texto("Yape Avisos", 24f, colorTexto, true))
        saludo = texto("", 14f, colorSuave)
        izq.addView(saludo)
        cab.addView(izq)

        val chip = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = redondeado(colorBoton, 14)
        }
        chip.addView(TextView(this).apply {
            text = "Vinculado a:"
            textSize = 11f
            setTextColor(colorSuave)
        })
        chipTienda = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        chip.addView(chipTienda)
        cab.addView(chip)
        raiz.addView(cab)
        raiz.addView(espacio(14))

        // Estado: rojo si falta activar, verde si todo está bien
        estadoCard = tarjeta(colorRojo)
        raiz.addView(estadoCard)

        raiz.addView(boton("Abrir acceso a notificaciones") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        raiz.addView(boton("Actualizar") { cargarTodo(null) })

        val info = tarjeta(colorTarjeta)
        info.addView(texto("Última notificación de Yape", 12f, colorSuave, true))
        infoNotif = texto("", 14f)
        info.addView(infoNotif)
        info.addView(texto("Último envío a la tienda", 12f, colorSuave, true))
        infoEnvio = texto("", 14f)
        info.addView(infoEnvio)
        info.addView(texto("Titular del QR vinculado", 12f, colorSuave, true))
        infoTitular = texto("", 14f)
        info.addView(infoTitular)
        raiz.addView(info)

        raiz.addView(texto("Historial de pagos", 17f, colorTexto, true))
        cargaTexto = texto("", 13f, colorCargando)
        raiz.addView(cargaTexto)
        botonHistorial = boton("🔒 Cargar historial") { pedirPin() }
        raiz.addView(botonHistorial)
        buscador = EditText(this).apply {
            hint = "Buscar por nombre, compra o monto"
            setHintTextColor(colorSuave)
            setTextColor(colorTexto)
            setSingleLine(true)
            background = redondeado(colorTarjeta, 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(10) }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { pintarHistorial() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        raiz.addView(buscador)

        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        raiz.addView(lista)

        pintarEstado()
        pintarHistorial()
    }

    override fun onResume() {
        super.onResume()
        if (::lista.isInitialized) cargarTodo(null)
    }

    private fun dp(v: Int) = (v * dens).toInt()

    private fun redondeado(color: Int, radioDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radioDp).toFloat()
    }

    private fun tarjeta(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = redondeado(color, 14)
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

    private fun espacio(alto: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(alto))
    }

    private fun boton(t: String, accion: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(colorTexto)
        background = redondeado(colorBoton, 10)
        setOnClickListener { accion() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(10) }
    }

    // Lee del celular lo último guardado (funciona sin internet)
    private fun pintarEstado() {
        val p = getSharedPreferences("estado", MODE_PRIVATE)
        val acceso = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true

        estadoCard.background = redondeado(if (acceso) colorVerde else colorRojo, 14)
        estadoCard.removeAllViews()
        estadoCard.addView(texto(if (acceso) "✅ Todo listo" else "❌ Falta activar", 18f, Color.WHITE, true))
        estadoCard.addView(texto(
            if (acceso) "La app está leyendo tus avisos de Yape."
            else "Toca \"Abrir acceso\" y activa Yape Avisos en la lista.",
            14f, Color.WHITE
        ))

        infoNotif.text = p.getString("ultimaNotif", "ninguna todavía")
        infoEnvio.text = p.getString("ultimoEnvio", "ninguno todavía")

        val nombre = p.getString("qrNombre", "") ?: ""
        chipTienda.text = if (nombre.isEmpty()) "Sin QR" else Titular.nombreCorto(nombre)
        if (nombre.isEmpty()) {
            infoTitular.text = "Sin QR cargado en la tienda"
            saludo.text = "Conecta tus pagos de Yape con la tienda"
        } else {
            infoTitular.text = Titular.nombreCorto(nombre)
            val primero = Titular.primerNombre(nombre)
            val yaVio = p.getBoolean("bienvenidaVista", false)
            saludo.text = if (yaVio) "Bienvenido de nuevo $primero" else "Bienvenido $primero"
            if (!yaVio) p.edit().putBoolean("bienvenidaVista", true).apply()
        }
    }

    // Ventana oscura con 4 casillas para el PIN
    private fun pedirPin() {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(18))
            background = redondeado(colorTarjeta, 18)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        caja.addView(texto("🔒 Cargar historial", 18f, colorTexto, true))
        caja.addView(texto("Escribe tu PIN de 4 dígitos", 14f, colorSuave))

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(12))
        }
        val casillas = ArrayList<EditText>()
        val msg = texto("", 14f, colorMal)

        for (i in 0 until 4) {
            val c = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                filters = arrayOf(InputFilter.LengthFilter(1))
                gravity = Gravity.CENTER
                textSize = 22f
                setTextColor(colorTexto)
                setHintTextColor(colorSuave)
                setSingleLine(true)
                background = GradientDrawable().apply {
                    setColor(colorBoton)
                    setStroke(dp(2), colorSuave)
                    cornerRadius = dp(10).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(54), dp(62)).apply { setMargins(dp(6), 0, dp(6), 0) }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                        msg.text = ""
                        if (s != null && s.isNotEmpty() && i < 3) casillas[i + 1].requestFocus()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
                setOnKeyListener { _, keyCode, ev ->
                    // Borrar en una casilla vacía regresa a la anterior
                    if (keyCode == KeyEvent.KEYCODE_DEL && ev.action == KeyEvent.ACTION_DOWN && text.isEmpty() && i > 0) {
                        casillas[i - 1].setText("")
                        casillas[i - 1].requestFocus()
                        true
                    } else false
                }
            }
            casillas.add(c)
            fila.addView(c)
        }
        caja.addView(fila)
        caja.addView(msg)

        val confirmar = boton("Confirmar") {}
        confirmar.setOnClickListener {
            val pin = casillas.joinToString("") { it.text.toString() }
            when {
                pin.length < 4 -> {
                    msg.text = "Escribe los 4 dígitos."
                    msg.setTextColor(colorMal)
                }
                pin != PIN -> {
                    msg.text = "❌ PIN incorrecto. Intenta de nuevo."
                    msg.setTextColor(colorMal)
                    casillas.forEach { it.setText("") }
                    casillas[0].requestFocus()
                }
                else -> {
                    msg.text = "✅ PIN correcto. Cargando historial..."
                    msg.setTextColor(colorOk)
                    confirmar.isEnabled = false
                    desbloqueado = true
                    cargarTodo { ok ->
                        if (ok) {
                            msg.text = "✅ Historial cargado"
                            msg.postDelayed({ dlg.dismiss() }, 1500)
                        } else {
                            msg.text = "❌ No se pudo cargar. Revisa internet y toca Confirmar."
                            msg.setTextColor(colorMal)
                            confirmar.isEnabled = true
                        }
                    }
                }
            }
        }
        caja.addView(confirmar)
        caja.addView(boton("Cancelar") { dlg.dismiss() })

        dlg.setContentView(caja)
        dlg.show()
        casillas[0].requestFocus()
    }

    // Trae de Supabase el nombre de la tienda, el titular del QR y, si ya desbloqueaste, el historial de pagos.
    // alTerminar (opcional) avisa si salió bien (true) o no (false).
    private fun cargarTodo(alTerminar: ((Boolean) -> Unit)?) {
        pintarEstado()
        cargaTexto.setTextColor(colorCargando)
        cargaTexto.text = "⏳ Cargando datos..."
        Thread {
            var err: String? = null
            var nuevos: List<Pago> = pagos

            // Solo pedimos el nombre del QR (la consulta completa trae las imágenes y es lenta)
            try {
                val arr = JSONArray(get("estado_tienda?id=eq.tienda_principal&select=titular:data->config->qr->nombre"))
                val valor = arr.optJSONObject(0)?.opt("titular")
                val nombreQr = if (valor is String) valor else ""
                getSharedPreferences("estado", MODE_PRIVATE).edit()
                    .putString("qrNombre", nombreQr)
                    .apply()
            } catch (e: Exception) {
                err = "No pude leer el nombre del QR (revisa internet)."
            }

            if (desbloqueado) try {
                val arr = JSONArray(get("pagos_yape?select=id,monto,nombre,compra,creado,usado&order=creado.desc&limit=100"))
                nuevos = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Pago(
                        monto = o.optDouble("monto", 0.0),
                        nombre = o.optString("nombre", ""),
                        compra = o.optString("compra", ""),
                        creado = formatoFecha(o.optString("creado", "")),
                        usado = o.optBoolean("usado", false)
                    )
                }
            } catch (e: Exception) {
                err = "No pude cargar el historial (revisa internet)."
            }

            val errFinal = err
            val listaFinal = nuevos
            runOnUiThread {
                pagos = listaFinal
                errorCarga = errFinal
                pintarEstado()
                pintarHistorial()
                if (errFinal == null) {
                    cargaTexto.setTextColor(colorOk)
                    cargaTexto.text = "✅ Datos cargados"
                } else {
                    cargaTexto.setTextColor(colorMal)
                    cargaTexto.text = "❌ $errFinal"
                }
                alTerminar?.invoke(errFinal == null)
            }
        }.start()
    }

    private fun get(ruta: String): String {
        val conn = URL("${Config.SUPABASE_URL}/rest/v1/$ruta").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("apikey", Config.SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
        val txt = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return txt
    }

    // "2026-11-19T00:35:49+00:00" (UTC) -> "18/11 20:35" (hora del celular)
    private fun formatoFecha(iso: String): String = try {
        val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        entrada.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = entrada.parse(iso.take(19))
        if (fecha == null) "" else SimpleDateFormat("dd/MM HH:mm", Locale.US).format(fecha)
    } catch (e: Exception) {
        ""
    }

    private fun pintarHistorial() {
        if (!::lista.isInitialized) return
        botonHistorial.visibility = if (desbloqueado) View.GONE else View.VISIBLE
        buscador.visibility = if (desbloqueado) View.VISIBLE else View.GONE
        lista.removeAllViews()
        if (!desbloqueado) return
        errorCarga?.let { lista.addView(texto(it, 14f, colorMal)) }

        val q = buscador.text.toString().trim().lowercase(Locale.getDefault()).replace(',', '.')
        val filtrados = pagos.filter { p ->
            q.isEmpty() ||
                p.nombre.lowercase(Locale.getDefault()).contains(q) ||
                p.compra.lowercase(Locale.getDefault()).contains(q) ||
                String.format(Locale.US, "%.2f", p.monto).contains(q)
        }

        if (filtrados.isEmpty() && errorCarga == null) {
            val aviso = if (q.isEmpty()) "Todavía no hay pagos." else "Nada coincide con \"$q\"."
            lista.addView(texto(aviso, 14f, colorSuave))
        }

        for (p in filtrados) {
            val tarjetaPago = tarjeta(colorTarjeta)
            val monto = String.format(Locale.US, "%.2f", p.monto)
            val quien = if (p.nombre.isEmpty()) "Sin nombre" else p.nombre
            tarjetaPago.addView(texto("Bs $monto  ·  $quien", 15f, colorTexto, true))
            tarjetaPago.addView(texto(
                if (p.compra.isNotEmpty()) "🛍️ ${p.compra}" else "🛍️ Sin compra registrada",
                13f, colorSuave
            ))
            val (estadoTxt, color) = when {
                !p.usado -> "⏳ Pendiente (sin venta)" to colorPendiente
                p.compra.isNotEmpty() -> "✅ Vendido" to colorOk
                else -> "✔ Usado" to colorSuave
            }
            tarjetaPago.addView(texto("${p.creado}  ·  $estadoTxt", 13f, color))
            lista.addView(tarjetaPago)
        }
    }
}
