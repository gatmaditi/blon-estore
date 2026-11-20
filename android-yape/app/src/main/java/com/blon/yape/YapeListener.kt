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

// Lee las notificaciones y, si dicen "NOMBRE te envió Bs. X", guarda nombre y monto en Supabase.
// Guarda en el celular lo último que recibió y lo último que envió, para diagnosticar problemas.
class YapeListener : NotificationListenerService() {

    // Grupo 1: nombre de quien paga. Grupo 2: monto.
    private val regex = Regex("([\\p{L} .]+?)\\s+te envi[oó]\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    // Otro formato de Yape: "Acabas de recibir Bs. 15.00 en tu Yape" (no trae el nombre)
    private val regexRecibir = Regex("recibir\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val prefijos = Regex("^(yape\\s+)?(qr\\s+de\\s+)?", RegexOption.IGNORE_CASE)

    // Evita procesar dos veces la misma notificación (misma clave de Android)
    private val clavesVistas = LinkedHashSet<String>()
    // Evita duplicados: mismo texto exacto (incluye el nombre de quien paga) en menos de 60 s
    private val textosRecientes = HashMap<String, Long>()

    // Al conectarse el servicio, reenvía los pagos que quedaron pendientes
    override fun onListenerConnected() {
        Thread { Pendientes.reenviar(applicationContext) }.start()
    }

    private fun estado() = getSharedPreferences("estado", MODE_PRIVATE)
    private fun hora() = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!clavesVistas.add(sbn.key)) return
        if (clavesVistas.size > 200) clavesVistas.remove(clavesVistas.first())

        val extras = sbn.notification.extras
        val texto = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
            .joinToString(" ") { extras.getCharSequence(it)?.toString() ?: "" }

        val m = regex.find(texto)
        val mRecibir = if (m == null) regexRecibir.find(texto) else null
        if (m == null && mRecibir == null) {
            if (texto.contains("yape", ignoreCase = true)) {
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
