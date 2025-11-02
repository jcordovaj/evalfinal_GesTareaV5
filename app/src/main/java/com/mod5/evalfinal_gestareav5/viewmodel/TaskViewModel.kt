package com.mod5.evalfinal_gestareav5.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.mod5.evalfinal_gestareav5.data.Task
import com.mod5.evalfinal_gestareav5.data.TaskRepository
import kotlinx.coroutines.*
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _allTasks = MutableLiveData<List<Task>>(emptyList())
    val allTasks: LiveData<List<Task>> get() = _allTasks

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> get() = _statusMessage

    // Evento para activar la navegación y limpieza
    private val _taskSavedEvent = MutableLiveData<String?>()
    val taskSavedEvent: LiveData<String?> get() = _taskSavedEvent

    init {
        loadTasks()
    }

    fun loadTasks(notifyOnSuccess: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true

            val tasks = withContext(Dispatchers.IO) {
                try {
                    delay(300L) // Simulación de retraso
                } catch (t: Throwable) { /* ignore */ }

                // ⭐ Lógica de Negocio: Leer, ordenar y filtrar (Pendientes)
                repository.readAllTasks()
                    .sortedWith(compareByDescending<Task> { it.date }.thenByDescending { it.time })
                    .filter { it.status == "Pendiente" }
            }

            _allTasks.value = tasks
            _isLoading.value = false
        }
    }

    fun saveOrUpdateTask(
        id: String?,
        name: String,
        description: String,
        status: String,
        date: String,
        time: String,
        category: String,
        requiresAlarm: Boolean
    ) {
        if (name.isBlank() || date.isBlank() || time.isBlank()) {
            _statusMessage.value = "ERROR: Falta completar campos obligatorios."
            return
        }

        viewModelScope.launch {
            val isEditing = id != null
            val taskId = id ?: UUID.randomUUID().toString()
            val task = Task(taskId, name, description, status, date, time, category, requiresAlarm)

            // 1. Ejecutar Persistencia en IO (simple, solo guarda/actualiza)
            val persistenceSuccess = withContext(Dispatchers.IO) {
                try {
                    delay(300L)
                } catch (t: Throwable) { /* ignore */ }

                if (isEditing) repository.updateTaskInCSV(task)
                else repository.saveTaskToCSV(task)
            }

            // El código regresa al hilo principal (Main)
            if (persistenceSuccess) {
                // 2. Activar Evento Único (para CrearTareaFragment)
                _taskSavedEvent.value = taskId
                _taskSavedEvent.value = null // Reset inmediato

                // 3. Forzar Recarga (actualiza VerTareasFragment con datos frescos)
                loadTasks()
            } else {
                _statusMessage.value = "ERROR al guardar la tarea"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun markTaskAsCompleted(task: Task) {
        val completedTask = task.copy(status = "Completada")
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.updateTaskInCSV(completedTask)
            }
            if (success) {
                _statusMessage.value = "Tarea marcada como 'Completada'"
                loadTasks()
            } else {
                _statusMessage.value = "Error al intentar cambiar el estado"
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.deleteTaskById(task.id)
            }
            if (success) {
                _statusMessage.value = "Tarea '${task.name}' eliminada."
                loadTasks()
            } else {
                _statusMessage.value = "Error al eliminar la tarea"
            }
        }
    }
}