package com.blon.yape

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.net.HttpURLConnection
import java.net.URL

// Lee las notificaciones y, si dicen "te envió Bs. X", guarda el monto en Supabase.
class YapeListener : NotificationListenerService() {

    private val regex = Regex("te envi[oó]\\s+Bs\\.?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private var ultimoMonto = -1.0
    private var ultimoMs = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val texto = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
            .joinToString(" ") { extras.getCharSequence(it)?.toString() ?: "" }

        val m = regex.find(texto) ?: return
        val monto = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return

        // Yape a veces repite la misma notificación: si es igual y llega en menos de 60 s, la ignoramos
        val ahora = System.currentTimeMillis()
        if (monto == ultimoMonto && ahora - ultimoMs < 60_000) return
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
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            // Sin internet en ese momento: este aviso se pierde
        }
    }
}
