package com.mod5.evalfinal_gestareav5.ui

// Librerías
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mod5.evalfinal_gestareav5.MainActivity
import com.mod5.evalfinal_gestareav5.R
import com.mod5.evalfinal_gestareav5.data.Task
import com.mod5.evalfinal_gestareav5.adapter.TaskAdapter
import com.mod5.evalfinal_gestareav5.viewmodel.TaskViewModel

class VerTareasFragment : Fragment() {

    // Vars
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewEmptyMessage: TextView
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskViewModel: TaskViewModel
    private var mainActivity: MainActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.ver_tareas, container,
            false)

        recyclerView         = view.findViewById(R.id.recyclerViewTasks)
        textViewEmptyMessage = view.findViewById(R.id.textViewEmptyListMessage)
        progressBarLoading   = view.findViewById(R.id.progressBarLoading)
        mainActivity         = activity as? MainActivity
        taskViewModel        = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        taskAdapter = TaskAdapter(
            tasks         = emptyList(),
            onItemClick   = { task -> mainActivity?.startTaskEdit(task) },
            onDeleteClick = { task -> confirmAndDeleteTask(task) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter       = taskAdapter

        setupObservers()
        return view
    }

    // Método que se ejecuta cuando el fragmento se hace visible y fuerza la recarga
    override fun onStart() {
        super.onStart()
        taskViewModel.loadTasks()
    }

    // Método que setea los observadores
    private fun setupObservers() {
        taskViewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.updateTasks(tasks)

            val isLoadingNow = taskViewModel.isLoading.value ?: false
            if (tasks.isNullOrEmpty() && !isLoadingNow) {
                recyclerView.visibility = View.GONE
                textViewEmptyMessage.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                textViewEmptyMessage.visibility = View.GONE
            }
        }

        taskViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Lógica para evitar el "parpadeo" de la lista vacía
            if (isLoading) {
                textViewEmptyMessage.visibility = View.GONE
                recyclerView.visibility = View.GONE
            } else {
                val tasks = taskViewModel.allTasks.value
                if (tasks.isNullOrEmpty()) {
                    recyclerView.visibility = View.GONE
                    textViewEmptyMessage.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    textViewEmptyMessage.visibility = View.GONE
                }
            }
        }
    }

    private fun confirmAndDeleteTask(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar tarea")
            .setMessage("Confirme que desea eliminar la tarea \"${task.name}\"")
            .setPositiveButton("Eliminar") { _, _ ->
                taskViewModel.deleteTask(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = VerTareasFragment()
    }
}