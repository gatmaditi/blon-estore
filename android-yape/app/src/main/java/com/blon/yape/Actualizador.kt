package com.blon.yape

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Una versión nueva publicada en GitHub
class Actualizacion(val build: Int, val notas: String, val apkUrl: String)

object Actualizador {
    private const val API = "https://api.github.com/repos/gatmaditi/blon-estore/releases/latest"

    // Devuelve la última versión si es más nueva que la instalada; si no, null
    fun buscar(versionInstalada: Int): Actualizacion? {
        val conn = URL(API).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "YapeAvisos")
        val json = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val j = JSONObject(json)
        // El tag es "yape-v<número de compilación>"
        val build = j.optString("tag_name").removePrefix("yape-v").toIntOrNull() ?: return null
        if (build <= versionInstalada) return null
        val notas = if (j.isNull("body")) "" else j.optString("body", "")
        val assets = j.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) {
                return Actualizacion(build, notas, a.getString("browser_download_url"))
            }
        }
        return null
    }
}
