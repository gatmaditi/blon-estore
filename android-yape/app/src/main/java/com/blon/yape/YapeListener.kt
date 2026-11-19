package com.blon.yape

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Lee las notificaciones y, si dicen "te envió Bs. X", guarda el monto en Supabase.
// Guarda en el celular el historial de pagos y lo último que recibió / envió.
class YapeListener : NotificationListenerService() {

    private val regex = Regex("te envi[oó]\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)

    // Evita procesar dos veces la misma notificación (misma clave de Android)
    private val clavesVistas = LinkedHashSet<String>()
    // Evita duplicados: mismo texto exacto (incluye el nombre de quien paga) en menos de 60 s
    private val textosRecientes = HashMap<String, Long>()

    private fun estado() = getSharedPreferences("estado", MODE_PRIVATE)
    private fun hora() = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!clavesVistas.add(sbn.key)) return
        if (clavesVistas.size > 200) clavesVistas.remove(clavesVistas.first())

        val extras = sbn.notification.extras
        val texto = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
            .joinToString(" ") { extras.getCharSequence(it)?.toString() ?: "" }

        val m = regex.find(texto)
        if (m == null) {
            if (texto.contains("yape", ignoreCase = true)) {
                estado().edit().putString("ultimaNotif", hora() + " · Yape, pero no es un pago: " + texto.take(120)).apply()
            }
            return
        }
        estado().edit().putString("ultimaNotif", hora() + " · " + texto.take(120)).apply()

        val monto = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return

        val ahora = System.currentTimeMillis()
        textosRecientes.entries.removeAll { ahora - it.value > 60_000 }
        if (textosRecientes.containsKey(texto)) {
            registrar(monto, "Repetido, ignorado")
            return
        }
        textosRecientes[texto] = ahora

        Thread { guardarPago(monto) }.start()
    }

    private fun registrar(monto: Double, resultado: String) {
        val montoTxt = String.format(Locale.US, "%.2f", monto)
        Registro.agregar(this, hora() + "|" + montoTxt + "|" + resultado)
        estado().edit().putString("ultimoEnvio", hora() + " · " + resultado + " · Bs " + montoTxt).apply()
    }

    // Intenta hasta 3 veces por si no hay internet en ese momento
    private fun guardarPago(monto: Double) {
        val esperas = longArrayOf(0L, 3000L, 8000L)
        var resultado = "Error"
        for (espera in esperas) {
            if (espera > 0) Thread.sleep(espera)
            resultado = intentarEnvio(monto)
            if (resultado == "Enviado") break
        }
        registrar(monto, resultado)
    }

    private fun intentarEnvio(monto: Double): String = try {
        val conn = URL("${Config.SUPABASE_URL}/rest/v1/pagos_yape").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("apikey", Config.SUPABASE_ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Prefer", "return=minimal")
        conn.doOutput = true
        conn.outputStream.use { it.write("""{"monto":$monto,"usado":false}""".toByteArray()) }
        val code = conn.responseCode
        conn.disconnect()
        if (code in 200..299) "Enviado" else "Error HTTP $code"
    } catch (e: Exception) {
        "Error: " + (e.message ?: "sin conexión")
    }
}
