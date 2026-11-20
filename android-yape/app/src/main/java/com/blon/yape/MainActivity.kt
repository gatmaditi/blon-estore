package com.blon.yape

import android.app.Activity
import android.app.DownloadManager
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File
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
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.InputMethodManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
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
    private var avisoVisto = false
    private var mostrarDigitos = false
    private var ojoPin: TextView? = null
    // Tipo de teclado que oculta los dígitos (puntos); el ojito cambia a numérico normal para verlos
    private val TIPO_OCULTO = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    private lateinit var pantallaPin: LinearLayout
    private lateinit var contenidoPrincipal: ScrollView
    private lateinit var vinculadoPin: TextView
    private lateinit var saludoPin: TextView
    private lateinit var msgPin: TextView
    private val casillasPin = ArrayList<TextView>()
    private lateinit var campoPin: EditText
    private lateinit var versionPin: TextView
    private var yendoAActualizar = false

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
    private val colorAzul = Color.parseColor("#1565C0")

    private var actualizacion: Actualizacion? = null
    private lateinit var bannerCard: LinearLayout
    private lateinit var bannerTexto: TextView

    private lateinit var saludo: TextView
    private lateinit var titularTexto: TextView
    private lateinit var bienvenidaCard: LinearLayout
    private lateinit var bienvenidaTitulo: TextView
    private lateinit var bienvenidaSub: TextView
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
        contenidoPrincipal = scroll
        contenidoPrincipal.visibility = View.GONE
        val root = FrameLayout(this)
        root.addView(scroll)
        pantallaPin = construirPantallaPin()
        root.addView(pantallaPin)
        setContentView(root)

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
        titularTexto = texto("", 16f, Color.parseColor("#4ADE80"), true)
        izq.addView(titularTexto)
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

        // Tarjeta de bienvenida (solo la primera vez)
        bienvenidaCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            background = redondeado(Color.parseColor("#4A148C"), 18)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(14) }
        }
        bienvenidaTitulo = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        bienvenidaSub = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#E1BEE7"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }
        bienvenidaCard.addView(bienvenidaTitulo)
        bienvenidaCard.addView(bienvenidaSub)
        bienvenidaCard.addView(Button(this).apply {
            text = "Empezar"
            isAllCaps = false
            setTextColor(Color.parseColor("#4A148C"))
            background = redondeado(Color.WHITE, 10)
            setOnClickListener {
                getSharedPreferences("estado", MODE_PRIVATE).edit().putBoolean("bienvenidaVista", true).apply()
                pintarEstado()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        bienvenidaCard.visibility = View.GONE
        raiz.addView(bienvenidaCard)

        // Aviso de nueva versión (oculto hasta que haya una)
        bannerCard = tarjeta(colorAzul)
        bannerTexto = texto("", 15f, Color.WHITE, true)
        bannerCard.addView(bannerTexto)
        bannerCard.visibility = View.GONE
        bannerCard.setOnClickListener { actualizacion?.let { mostrarDialogoActualizar(it) } }
        raiz.addView(bannerCard)

        // Estado: rojo si falta activar, verde si todo está bien
        estadoCard = tarjeta(colorRojo)
        raiz.addView(estadoCard)

        raiz.addView(boton("Abrir acceso a notificaciones") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        raiz.addView(boton("Actualizar") { cargarTodo(null) })
        raiz.addView(boton("Buscar actualización") { buscarActualizacion(false) })

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
        yendoAActualizar = false
        Thread { Pendientes.reenviar(applicationContext) }.start()
        if (!::lista.isInitialized) return
        if (!desbloqueado) pedirPin()
        cargarTodo(null)
        if (avisoVisto) return
        // Revisa si hay actualización en segundo plano; si la hay, su página sale encima
        buscarActualizacion(true) { _ -> avisoVisto = true }
    }

    // Al salir de la app, el historial vuelve a quedar bloqueado
    override fun onStop() {
        super.onStop()
        // Si solo fuimos a la página de actualización, no bloqueamos nada
        if (!yendoAActualizar) {
            desbloqueado = false
            avisoVisto = false
        }
        if (::lista.isInitialized) pintarHistorial()
    }

    // Busca una versión nueva en GitHub. Automático: avisa una vez por versión. Manual: siempre responde.
    private fun buscarActualizacion(automatico: Boolean, alTerminar: ((Boolean) -> Unit)? = null) {
        Thread {
            val instalada = try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode
            } catch (e: Exception) { 0 }
            val nueva = try { Actualizador.buscar(instalada) } catch (e: Exception) { null }
            runOnUiThread {
                actualizacion = nueva
                if (nueva == null) {
                    bannerCard.visibility = View.GONE
                    if (!automatico) Toast.makeText(this, "✅ Ya tienes la versión más reciente", Toast.LENGTH_SHORT).show()
                    alTerminar?.invoke(false)
                } else {
                    bannerCard.background = redondeado(colorAzul, 14)
                    bannerTexto.text = "🆕 Nueva versión disponible. Toca aquí para actualizar"
                    bannerCard.visibility = View.VISIBLE
                    mostrarDialogoActualizar(nueva)
                    alTerminar?.invoke(true)
                }
            }
        }.start()
    }

    // Abre la página "Nueva actualización" (una pantalla normal de la app)
    private fun mostrarDialogoActualizar(a: Actualizacion, alMasTarde: (() -> Unit)? = null) {
        yendoAActualizar = true
        startActivity(
            Intent(this, ActualizarActivity::class.java)
                .putExtra("build", a.build)
                .putExtra("notas", a.notas)
                .putExtra("apk", a.apkUrl)
        )
    }

    // Descarga el APK con el gestor de descargas del celular y abre el instalador al terminar
    private fun descargarActualizacion(a: Actualizacion) {
        val nombre = "yape-avisos-update.apk"
        File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), nombre).delete()
        val dm = getSystemService(DownloadManager::class.java)
        val id = dm.enqueue(
            DownloadManager.Request(Uri.parse(a.apkUrl))
                .setTitle("Yape Avisos")
                .setDescription("Descargando actualización ${a.build}")
                .setMimeType("application/vnd.android.package-archive")
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, nombre)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        )
        bannerCard.background = redondeado(colorAzul, 14)
        bannerCard.visibility = View.VISIBLE
        bannerTexto.text = "⬇️ Descargando actualización..."

        Thread {
            var terminado = false
            while (!terminado) {
                Thread.sleep(700)
                val c = dm.query(DownloadManager.Query().setFilterById(id))
                if (!c.moveToFirst()) {
                    c.close()
                    terminado = true
                    runOnUiThread { mostrarErrorBanner("❌ No se pudo descargar la actualización") }
                    continue
                }
                val estado = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bajado = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                c.close()
                when (estado) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        terminado = true
                        val uri = dm.getUriForDownloadedFile(id)
                        runOnUiThread { instalar(uri) }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        terminado = true
                        runOnUiThread { mostrarErrorBanner("❌ No se pudo descargar la actualización") }
                    }
                    else -> {
                        val pct = if (total > 0) bajado * 100 / total else 0
                        runOnUiThread { bannerTexto.text = "⬇️ Descargando actualización... $pct%" }
                    }
                }
            }
        }.start()
    }

    private fun instalar(uri: Uri) {
        bannerTexto.text = "✅ Descarga lista. Confirma la instalación."
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(i)
        } catch (e: Exception) {
            mostrarErrorBanner("Permite \"Instalar apps desconocidas\" para Yape Avisos y toca el aviso otra vez")
        }
    }

    private fun mostrarErrorBanner(msg: String) {
        bannerCard.background = redondeado(colorRojo, 14)
        bannerCard.visibility = View.VISIBLE
        bannerTexto.text = msg
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
        val pendientes = Pendientes.cantidad(this)
        infoEnvio.text = (p.getString("ultimoEnvio", "ninguno todavía") ?: "") +
            (if (pendientes > 0) "\n⏳ Pendientes por enviar: $pendientes" else "")

        val nombre = p.getString("qrNombre", "") ?: ""
        val tienda = p.getString("tiendaNombre", "") ?: ""
        chipTienda.text = if (tienda.isEmpty()) "Sin tienda" else tienda
        titularTexto.text = if (nombre.isEmpty()) "" else Titular.nombreCorto(nombre)
        if (nombre.isEmpty()) {
            infoTitular.text = "Sin QR cargado en la tienda"
            saludo.text = "Conecta tus pagos de Yape con la tienda"
            bienvenidaCard.visibility = View.GONE
        } else {
            infoTitular.text = Titular.nombreCorto(nombre)
            val primero = Titular.primerNombre(nombre)
            if (p.getBoolean("bienvenidaVista", false)) {
                // Ya la viste: solo un saludo chico
                bienvenidaCard.visibility = View.GONE
                saludo.text = "Bienvenido de nuevo, $primero"
            } else {
                // Primera vez: tarjeta de bienvenida grande
                bienvenidaCard.visibility = View.VISIBLE
                bienvenidaTitulo.text = "¡Bienvenido, $primero! 👋"
                bienvenidaSub.text = "Tus pagos de Yape quedan conectados a ${if (tienda.isEmpty()) "tu tienda" else tienda}."
                saludo.text = ""
            }
        }
    }

    // Pantalla de clave: se muestra cada vez que entras a la app, antes de la información
    private fun construirPantallaPin(): LinearLayout {
        val negro = Color.parseColor("#000000")
        val verde = Color.parseColor("#26A69A")
        val pantalla = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(negro)
            setPadding(dp(24), dp(40), dp(24), dp(24))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        pantalla.addView(TextView(this).apply {
            text = "Yape Avisos"
            textSize = 16f
            setTextColor(Color.parseColor("#E1BEE7"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        })
        saludoPin = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(40), 0, dp(30))
        }
        pantalla.addView(saludoPin)
        pantalla.addView(TextView(this).apply {
            text = "Ingresa tu clave"
            textSize = 15f
            setTextColor(Color.parseColor("#E1BEE7"))
            gravity = Gravity.CENTER
        })

        // Campo invisible: recibe lo que escribes y saca el teclado numérico
        campoPin = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
            alpha = 0f
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(1))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                    msgPin.text = ""
                    pintarCasillas()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        pantalla.addView(campoPin)

        // Las 4 casillas: muestran puntos, o los números si tocas el ojito
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(18))
        }
        for (i in 0 until 4) {
            val c = TextView(this).apply {
                textSize = 24f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(2), Color.parseColor("#E1BEE7"))
                    cornerRadius = dp(10).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(62)).apply { setMargins(dp(6), 0, dp(6), 0) }
                setOnClickListener { pedirTeclado() }
            }
            casillasPin.add(c)
            fila.addView(c)
        }
        ojoPin = TextView(this).apply {
            text = "👁"
            textSize = 22f
            setTextColor(Color.parseColor("#E1BEE7"))
            setPadding(dp(8), 0, 0, 0)
            setOnClickListener {
                mostrarDigitos = !mostrarDigitos
                text = if (mostrarDigitos) "🙈" else "👁"
                pintarCasillas()
            }
        }
        fila.addView(ojoPin)
        pantalla.addView(fila)

        msgPin = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#FFCDD2"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        pantalla.addView(msgPin)

        pantalla.addView(Button(this).apply {
            text = "Ingresar"
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(verde)
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { intentarIngresar() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
        })

        pantalla.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 0, 1f) })
        vinculadoPin = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#9E9E9E"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        pantalla.addView(vinculadoPin)
        versionPin = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#616161"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        pantalla.addView(versionPin)
        return pantalla
    }

    // Pinta las 4 casillas según lo escrito y el ojito
    private fun pintarCasillas() {
        if (!::campoPin.isInitialized) return
        val txt = campoPin.text.toString()
        for (i in 0 until casillasPin.size) {
            casillasPin[i].text = when {
                i >= txt.length -> ""
                mostrarDigitos -> txt[i].toString()
                else -> "●"
            }
        }
    }

    // Abre el teclado numérico
    private fun pedirTeclado() {
        campoPin.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(campoPin, InputMethodManager.SHOW_IMPLICIT)
    }

    // Muestra la pantalla de clave (cada vez que entras a la app)
    private fun pedirPin() {
        pintarSaludoPin()
        mostrarDigitos = false
        ojoPin?.text = "👁"
        val tienda = getSharedPreferences("estado", MODE_PRIVATE).getString("tiendaNombre", "") ?: ""
        vinculadoPin.text = "Vinculado a: ${if (tienda.isEmpty()) "tu tienda" else tienda}"
        versionPin.text = "Versión ${versionApp()}"
        contenidoPrincipal.visibility = View.GONE
        msgPin.text = ""
        campoPin.setText("")
        pintarCasillas()
        pantallaPin.visibility = View.VISIBLE
        campoPin.post { pedirTeclado() }
    }

    // Número de versión instalada (es el número de la compilación)
    private fun versionApp(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionCode.toString()
    } catch (e: Exception) {
        "?"
    }

    private fun pintarSaludoPin() {
        if (!::saludoPin.isInitialized) return
        val nombre = getSharedPreferences("estado", MODE_PRIVATE).getString("qrNombre", "") ?: ""
        saludoPin.text = if (nombre.isEmpty()) "¡Bienvenid@!" else "¡Bienvenid@ ${Titular.primerNombre(nombre)}!"
    }

    // Cierra el teclado para que no se quede abierto
    private fun ocultarTeclado() {
        val v = currentFocus ?: window.decorView
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
        v.clearFocus()
    }

    private fun intentarIngresar() {
        val pin = campoPin.text.toString()
        when {
            pin.length < 4 -> msgPin.text = "Escribe los 4 dígitos."
            pin != PIN -> {
                msgPin.text = "❌ Clave incorrecta. Intenta de nuevo."
                campoPin.setText("")
                pintarCasillas()
            }
            else -> {
                desbloqueado = true
                ocultarTeclado()
                pantallaPin.visibility = View.GONE
                contenidoPrincipal.visibility = View.VISIBLE
                cargarTodo(null)
            }
        }
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
                val arr = JSONArray(get("estado_tienda?id=eq.tienda_principal&select=titular:data->config->qr->nombre,tienda:data->config->nombre"))
                val fila = arr.optJSONObject(0)
                val nombreQr = fila?.opt("titular")?.let { if (it is String) it else "" } ?: ""
                val nombreTienda = fila?.opt("tienda")?.let { if (it is String) it else "" } ?: ""
                getSharedPreferences("estado", MODE_PRIVATE).edit()
                    .putString("qrNombre", nombreQr)
                    .putString("tiendaNombre", nombreTienda)
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
                pintarSaludoPin()
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
