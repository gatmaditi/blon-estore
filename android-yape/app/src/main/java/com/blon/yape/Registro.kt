package com.blon.yape

import android.content.Context

// Historial de pagos guardado en el celular (los últimos 50). Cada línea: hora|monto|resultado
object Registro {
    private const val CLAVE = "historial"
    private const val MAX = 50

    @Synchronized
    fun agregar(ctx: Context, linea: String) {
        val p = ctx.getSharedPreferences("estado", Context.MODE_PRIVATE)
        val actual = p.getString(CLAVE, "") ?: ""
        val nueva = (listOf(linea) + actual.split("\n").filter { it.isNotBlank() }).take(MAX).joinToString("\n")
        p.edit().putString(CLAVE, nueva).apply()
    }

    fun leer(ctx: Context): List<String> {
        val p = ctx.getSharedPreferences("estado", Context.MODE_PRIVATE)
        return (p.getString(CLAVE, "") ?: "").split("\n").filter { it.isNotBlank() }
    }
}
