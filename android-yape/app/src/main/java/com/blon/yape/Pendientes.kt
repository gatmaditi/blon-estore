package com.blon.yape

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Pagos que no se pudieron mandar a la tienda: se guardan en el celular y se reenvían luego.
object Pendientes {
    private const val CLAVE = "pendientes"

    @Synchronized
    fun guardar(ctx: Context, monto: Double, nombre: String) {
        val p = ctx.getSharedPreferences("estado", Context.MODE_PRIVATE)
        val arr = try { JSONArray(p.getString(CLAVE, "[]") ?: "[]") } catch (e: Exception) { JSONArray() }
        arr.put(JSONObject().put("monto", monto).put("nombre", nombre))
        p.edit().putString(CLAVE, arr.toString()).apply()
    }

    // Intenta mandar de nuevo los pendientes; los que sí se mandan se quitan de la lista
    @Synchronized
    fun reenviar(ctx: Context) {
        val p = ctx.getSharedPreferences("estado", Context.MODE_PRIVATE)
        val arr = try { JSONArray(p.getString(CLAVE, "[]") ?: "[]") } catch (e: Exception) { return }
        if (arr.length() == 0) return
        val quedan = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (!enviar(o.optDouble("monto", 0.0), o.optString("nombre", ""))) quedan.put(o)
        }
        p.edit().putString(CLAVE, quedan.toString()).apply()
    }

    fun cantidad(ctx: Context): Int = try {
        JSONArray(ctx.getSharedPreferences("estado", Context.MODE_PRIVATE).getString(CLAVE, "[]") ?: "[]").length()
    } catch (e: Exception) { 0 }

    private fun enviar(monto: Double, nombre: String): Boolean = try {
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
        code in 200..299
    } catch (e: Exception) {
        false
    }
}
