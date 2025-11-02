package com.mod5.evalfinal_gestareav5.data

// Librerías
import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

class TaskRepository(application: Application) {

    // Vals
    private val context  = application
    private val fileName = "tareas.csv"
    private val file     = File(context.getExternalFilesDir(null), fileName)

    init {
        // verifica que el archivo existe y lo crea si no existe
        if (!file.exists()) {
            file.createNewFile()
        }
    }

    // Lee el csv y trae todos los registros, si los hay.
    suspend fun readAllTasks(): List<Task> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<Task>()
        if (!file.exists()) return@withContext tasks

        try {
            BufferedReader(FileReader(file)).use { reader ->
                reader.forEachLine { rawLine ->
                    val trimmedLine = rawLine.trim()

                    if (trimmedLine.isNotEmpty()) {
                        // como buena práctica, hace una limpieza defensiva
                        val cleanLine = trimmedLine.replace(Regex("\\s*,\\s*"),
                            ",")

                        Task.fromCsvString(cleanLine)?.let { tasks.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error al leer el archivo CSV: ${e.message}")
        }

        // Retorna la lista en el objeto 'tasks'.
        tasks
    }

    // Guarda la lista de tareas en el csv
    private fun saveAllTasks(tasks: List<Task>): Boolean {
        return try {
            BufferedWriter(FileWriter(file)).use { writer ->
                tasks.forEach { task ->
                    writer.write(task.toCsvString())
                    writer.newLine()
                }
            }
            true
        } catch (e: Exception) {
            println("Error al escribir en el archivo 'csv': ${e.message}")
            false
        }
    }

    // Guarda una nueva tarea.
    suspend fun saveTaskToCSV(task: Task): Boolean = withContext(Dispatchers.IO) {
        val tasks = readAllTasks().toMutableList()
        tasks.add(task)
        saveAllTasks(tasks)
    }

    // Actualiza una tarea existente.
    suspend fun updateTaskInCSV(updatedTask: Task): Boolean = withContext(Dispatchers.IO) {
        val tasks = readAllTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == updatedTask.id }

        return@withContext if (index != -1) {
            tasks[index] = updatedTask
            saveAllTasks(tasks)
        } else {
            saveTaskToCSV(updatedTask)
        }
    }

    // Elimina una tarea por ID.
    suspend fun deleteTaskById(id: String): Boolean = withContext(Dispatchers.IO) {
        val tasks        = readAllTasks().toMutableList()
        val originalSize = tasks.size
        tasks.removeIf { it.id == id }

        return@withContext if (tasks.size < originalSize) {
            saveAllTasks(tasks)
        } else {
            false
        }
    }
}