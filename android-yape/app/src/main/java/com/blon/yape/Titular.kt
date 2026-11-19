package com.blon.yape

// Convierte "SEBASTIAN G.V" en "Sebastian G.V" (nombre + inicial de cada apellido)
object Titular {
    fun primerNombre(raw: String): String {
        val partes = raw.split(' ', '.').filter { it.isNotBlank() }
        return partes.firstOrNull()?.let { capitalizar(it) } ?: ""
    }

    fun nombreCorto(raw: String): String {
        val partes = raw.split(' ', '.').filter { it.isNotBlank() }
        if (partes.isEmpty()) return ""
        val iniciales = partes.drop(1).map { it.first().uppercaseChar().toString() }
        return capitalizar(partes[0]) + (if (iniciales.isEmpty()) "" else " " + iniciales.joinToString("."))
    }

    private fun capitalizar(s: String) = s.lowercase().replaceFirstChar { it.uppercaseChar() }
}
