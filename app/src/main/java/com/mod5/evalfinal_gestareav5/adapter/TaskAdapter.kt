package com.mod5.evalfinal_gestareav5.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mod5.evalfinal_gestareav5.R
import com.mod5.evalfinal_gestareav5.data.Task

class TaskAdapter(
    private var tasks        : List<Task>,
    private val onItemClick  : (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        // Vincula los datos y pasa los callbacks
        holder.bind(task, onItemClick, onDeleteClick, position + 1)
    }

    override fun getItemCount(): Int = tasks.size

    // Función clave para MVVM: Actualiza la lista desde el LiveData del ViewModel
    fun updateTasks(newTasks: List<Task>) {
        this.tasks = newTasks
        notifyDataSetChanged() // realza la notificación del cambio
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskIdOrdinal  : TextView = itemView.findViewById(R.id.textViewTaskIdOrdinal)
        private val taskIdUuid     : TextView = itemView.findViewById(R.id.textViewTaskIdUuid)
        private val taskName       : TextView = itemView.findViewById(R.id.textViewTaskName)
        private val taskDescription: TextView = itemView.findViewById(R.id.textViewTaskDescription)
        private val taskDateTime   : TextView = itemView.findViewById(R.id.textViewTaskDateTime)
        private val taskStatus     : TextView = itemView.findViewById(R.id.textViewTaskStatus)
        private val taskCategory   : TextView = itemView.findViewById(R.id.textViewTaskCategory)
        private val taskAlarm      : TextView = itemView.findViewById(R.id.textViewTaskAlarm)
        private val buttonDelete   : Button   = itemView.findViewById(R.id.buttonDeleteTask)

        fun bind(task: Task, onItemClick: (Task) -> Unit,
                 onDeleteClick: (Task) -> Unit, ordinalId: Int) {
            taskIdOrdinal.text   = "ID  : #$ordinalId"
            taskIdUuid.text      = "UUID: ${task.id.substring(0, 8)}..." // Muestra parte del UUID
            taskName.text        = task.name
            taskDescription.text = task.description
            taskDateTime.text    = "Fecha/Hora: ${task.date} - ${task.time}"
            taskStatus.text      = "Estado: ${task.status}"
            taskCategory.text    = "Tipo: ${task.category}"
            taskAlarm.text       = if (task.requiresAlarm) "Alarma: ✅ ON" else "Alarma: ❌ OFF"

            // Listener para Edición (toda la fila)
            itemView.setOnClickListener { onItemClick(task) }

            // Listener para Eliminación (solo el botón)
            buttonDelete.setOnClickListener { onDeleteClick(task) }

            // Lógica visual adicional para el estado
            val context = itemView.context
            val statusColor = when (task.status) {
                "Completada" -> context.getColor(R.color.green_completed)
                "Cancelada"  -> context.getColor(R.color.red_cancelled)
                else -> context.getColor(R.color.blue_pending)
            }
            taskStatus.setTextColor(statusColor)
        }
    }
}