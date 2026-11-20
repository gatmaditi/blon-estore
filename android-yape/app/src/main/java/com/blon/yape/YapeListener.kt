package com.blon.yape

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Lee los avisos de Yape y, si son un pago, guarda nombre y monto en Supabase.
class YapeListener : NotificationListenerService() {

    // Grupo 1: nombre de quien paga. Grupo 2: monto.
    private val regex = Regex("([\\p{L} .]+?)\\s+te envi[oó]\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    // Otro formato de Yape: "Acabas de recibir Bs. 15.00 en tu Yape" (no trae el nombre)
    private val regexRecibir = Regex("recibir\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val prefijos = Regex("^(yape\\s+)?(qr\\s+de\\s+)?", RegexOption.IGNORE_CASE)

    // Evita procesar dos veces la misma notificación mientras la app está abierta
    private val clavesVistas = LinkedHashSet<String>()
    // Evita duplicados: mismo texto exacto en menos de 60 s
    private val textosRecientes = HashMap<String, Long>()
    // Cuántos pagos nuevos se encontraron en la última revisión de la barra
    private var cargadosEnRevision = 0

    private fun estado() = getSharedPreferences("estado", MODE_PRIVATE)
    private fun hora() = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())

    // Al conectarse (o al pedir revisión): reenvía pendientes y revisa los avisos que sigan en la barra
    override fun onListenerConnected() {
        Thread { Pendientes.reenviar(applicationContext) }.start()
        revisarAvisosActivos()
    }

    fun revisarAvisosActivos() {
        val ahora = System.currentTimeMillis()
        cargadosEnRevision = 0
        val activas = try { activeNotifications } catch (e: Exception) { null }
        if (activas != null) {
            for (sbn in activas) {
                // Solo avisos de las últimas 6 horas
                if (ahora - sbn.postTime > 6 * 60 * 60 * 1000L) continue
                procesar(sbn, desdeRevision = true)
            }
        }
        // Deja constancia del resultado para que la app lo muestre
        estado().edit()
            .putLong("revisionHora", System.currentTimeMillis())
            .putInt("revisionCantidad", cargadosEnRevision)
            .apply()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        procesar(sbn, desdeRevision = false)
    }

    private fun yaProcesada(key: String): Boolean =
        (estado().getString("keysProcesadas", "") ?: "").split(",").contains(key.replace(",", ";"))

    private fun marcarProcesada(key: String) {
        val lista = (estado().getString("keysProcesadas", "") ?: "").split(",").filter { it.isNotEmpty() } + key.replace(",", ";")
        estado().edit().putString("keysProcesadas", lista.takeLast(300).joinToString(",")).apply()
    }

    private fun procesar(sbn: StatusBarNotification, desdeRevision: Boolean) {
        if (!clavesVistas.add(sbn.key)) return
        if (clavesVistas.size > 200) clavesVistas.remove(clavesVistas.first())

        val extras = sbn.notification.extras
        val texto = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
            .joinToString(" ") { extras.getCharSequence(it)?.toString() ?: "" }

        val esYape = texto.contains("yape", ignoreCase = true)
        if (desdeRevision && !esYape) return
        if (desdeRevision && yaProcesada(sbn.key)) return
        if (esYape) marcarProcesada(sbn.key)

        val m = regex.find(texto)
        val mRecibir = if (m == null) regexRecibir.find(texto) else null
        if (m == null && mRecibir == null) {
            if (esYape) {
                estado().edit().putString("ultimaNotif", hora() + " · Yape, pero no es un pago: " + texto.take(120)).apply()
            }
            return
        }
        estado().edit().putString("ultimaNotif", hora() + " · " + texto.take(120)).apply()

        val montoTxt = if (m != null) m.groupValues[2] else mRecibir!!.groupValues[1]
        val monto = montoTxt.replace(',', '.').toDoubleOrNull() ?: return
        val nombre = if (m != null) m.groupValues[1].trim().replace(prefijos, "").trim() else ""

        val ahora = System.currentTimeMillis()
        textosRecientes.entries.removeAll { ahora - it.value > 60_000 }
        if (textosRecientes.containsKey(texto)) {
            registrar(monto, "Repetido, ignorado")
            return
        }
        textosRecientes[texto] = ahora

        if (desdeRevision) cargadosEnRevision++
        Thread { guardarPago(monto, nombre) }.start()
    }

    private fun registrar(monto: Double, resultado: String, nombre: String = "") {
        val montoTxt = String.format(Locale.US, "%.2f", monto)
        Registro.agregar(this, hora() + "|" + montoTxt + "|" + resultado)
        val quien = if (nombre.isEmpty()) "" else " · $nombre"
        estado().edit().putString("ultimoEnvio", hora() + " · " + resultado + " · Bs " + montoTxt + quien).apply()
    }

    // Intenta hasta 3 veces por si no hay internet en ese momento
    private fun guardarPago(monto: Double, nombre: String) {
        val esperas = longArrayOf(0L, 3000L, 8000L)
        var resultado = "Error"
        for (espera in esperas) {
            if (espera > 0) Thread.sleep(espera)
            resultado = intentarEnvio(monto, nombre)
            if (resultado == "Enviado") break
        }
        registrar(monto, resultado, nombre)
        // Si no se pudo mandar, queda guardado y se reenvía después
        if (resultado != "Enviado") Pendientes.guardar(this, monto, nombre)
    }

    private fun intentarEnvio(monto: Double, nombre: String): String = try {
        val conn = URL("${Config.SUPABASE_URL}/rest/v1/pagos_yape").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("apikey", Config.SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Prefer", "return=minimal")
        conn.doOutput = true
        val cuerpo = JSONObject().put("monto", monto).put("usado", false).put("nombre", nombre).toString()
        conn.outputStream.use { it.write(cuerpo.toByteArray()) }
        val code = conn.responseCode
        conn.disconnect()
        if (code in 200..299) "Enviado" else "Error HTTP $code"
    } catch (e: Exception) {
        "Error: " + (e.message ?: "sin conexión")
    }
}
