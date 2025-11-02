package com.mod5.evalfinal_gestareav5.data

data class Task(
    val id           : String,
    val name         : String,
    val description  : String,
    val status       : String, // Pendiente, Completada, Cancelada
    val date         : String, // DD/MM/YYYY
    val time         : String, // HH:MM
    val category     : String,
    val requiresAlarm: Boolean
) {
    // Convierte cada objeto Task en una línea del CSV sin espacios después de las comas.
    fun toCsvString(): String {
        return "$id,$name,$description,$status,$date,$time,$category,$requiresAlarm"
    }

    companion object {
        // Crea un objeto Task a partir de una línea de CSV.
        fun fromCsvString(csvString: String): Task? {
            return try {
                // Confiamos en que la limpieza se hizo en el Repository antes de split
                val parts = csvString.split(',')
                if (parts.size != 8) return null

                val requiresAlarm = parts[7].toBoolean()

                // Nota: Los campos no necesitan trim() si la limpieza se hizo en el Repository,
                // pero si el campo en sí contenía espacios, deben manejarse por separado (ej. parts[1].trim())
                Task(
                    id            = parts[0],
                    name          = parts[1],
                    description   = parts[2],
                    status        = parts[3],
                    date          = parts[4],
                    time          = parts[5],
                    category      = parts[6],
                    requiresAlarm = requiresAlarm
                )
            } catch (e: Exception) {
                println("Error leyendo el CSV para la Tarea: $csvString - ${e.message}")
                null
            }
        }
    }
}