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
// Además guarda en el celular lo último que recibió y lo último que envió, para diagnosticar problemas.
class YapeListener : NotificationListenerService() {

    private val regex = Regex("te envi[oó]\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private var ultimoMonto = -1.0
    private var ultimoMs = 0L

    private fun estado() = getSharedPreferences("estado", MODE_PRIVATE)
    private fun hora() = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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

        // Yape a veces repite la misma notificación: si es igual y llega en menos de 60 s, la ignoramos
        val ahora = System.currentTimeMillis()
        if (monto == ultimoMonto && ahora - ultimoMs < 60_000) {
            estado().edit().putString("ultimoEnvio", hora() + " · repetido, ignorado (Bs " + monto + ")").apply()
            return
        }
        ultimoMonto = monto
        ultimoMs = ahora

        Thread { guardarPago(monto) }.start()
    }

    private fun guardarPago(monto: Double) {
        try {
            val conn = URL("${Config.SUPABASE_URL}/rest/v1/pagos_yape").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", Config.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${Config.SUPABASE_ANON_KEY}")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            conn.outputStream.use { it.write("""{"monto":$monto,"usado":false}""".toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            val resultado = if (code in 200..299) "OK ($code)" else "ERROR HTTP $code"
            estado().edit().putString("ultimoEnvio", hora() + " · " + resultado + " · Bs " + monto).apply()
        } catch (e: Exception) {
            estado().edit().putString("ultimoEnvio", hora() + " · ERROR: " + e.message + " · Bs " + monto).apply()
        }
    }
}
