package com.mod5.evalfinal_gestareav5.ui

// Librerías
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mod5.evalfinal_gestareav5.MainActivity
import com.mod5.evalfinal_gestareav5.R
import com.mod5.evalfinal_gestareav5.viewmodel.TaskViewModel
import java.util.*

class CrearTareaFragment : Fragment() {

    // Vars
    private lateinit var taskViewModel          : TaskViewModel
    private lateinit var editTextTaskName       : EditText
    private lateinit var editTextTaskDescription: EditText
    private lateinit var editTextTaskDate       : EditText
    private lateinit var editTextTaskTime       : EditText
    private lateinit var spinnerStatus          : Spinner
    private lateinit var spinnerCategory        : Spinner
    private lateinit var checkBoxRequiresAlarm  : CheckBox
    private lateinit var buttonGrabar           : Button

    // Estados iniciales
    private var taskId      : String?       = null
    private var isEditing   : Boolean       = false
    private var selectedDate: String        = ""
    private var selectedTime: String        = ""
    private var mainActivity: MainActivity? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(),
                "Permiso de notificación concedido. Intente grabar de nuevo.",
                Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(),
                "ALERTA: Falta autorizar permiso, la alarma no funcionará.",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.crear_tarea,
            container, false)

        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        mainActivity  = activity as? MainActivity

        // Mapeo vistas
        editTextTaskName        = view.findViewById(R.id.editTextTaskName)
        editTextTaskDescription = view.findViewById(R.id.editTextTaskDescription)
        editTextTaskDate        = view.findViewById(R.id.editTextTaskDate)
        editTextTaskTime        = view.findViewById(R.id.editTextTaskTime)
        spinnerStatus           = view.findViewById(R.id.spinnerStatus)
        spinnerCategory         = view.findViewById(R.id.spinnerCategory)
        checkBoxRequiresAlarm   = view.findViewById(R.id.checkBoxRequiresAlarm)
        buttonGrabar            = view.findViewById(R.id.buttonSaveTask)

        // Listeners Fecha/Hora
        editTextTaskDate.setOnClickListener { showDatePickerDialog() }
        editTextTaskTime.setOnClickListener { showTimePickerDialog() }

        // 3. Carga de Edición (Lógica original sin cambios)
        arguments?.let { args ->
            isEditing = true
            taskId    = args.getString(TASK_ID_KEY)
            editTextTaskName.setText(args.getString(TASK_NAME_KEY) ?: "")
            editTextTaskDescription.setText(args.getString(TASK_DESCRIPTION_KEY) ?: "")
            selectedDate = args.getString(TASK_DATE_KEY) ?: ""
            selectedTime = args.getString(TASK_TIME_KEY) ?: ""
            editTextTaskDate.setText(selectedDate)
            editTextTaskTime.setText(selectedTime)
            checkBoxRequiresAlarm.isChecked = args.getBoolean(TASK_ALARM_KEY,
                false)

            (spinnerStatus.adapter as? ArrayAdapter<String>)?.let { adapter ->
                args.getString(TASK_STATUS_KEY)?.let { desired ->
                    adapter.getPosition(desired).takeIf { it >= 0 }?.
                    let { spinnerStatus.setSelection(it) }
                }
            }
            (spinnerCategory.adapter as? ArrayAdapter<String>)?.let { adapter ->
                args.getString(TASK_CATEGORY_KEY)?.let { desired ->
                    adapter.getPosition(desired).takeIf { it >= 0 }?.
                    let { spinnerCategory.setSelection(it) }
                }
            }

            buttonGrabar.text = "Actualizar Tarea"
        } ?: run {
            buttonGrabar.text = "Guardar Tarea"
        }

        // Llama a los observadores
        setupObservers()

        // Listener del botón para guardar/actualizar
        buttonGrabar.setOnClickListener {
            // Deshabilitado inicialmente para prevenir doble click mientras corre la corrutina
            buttonGrabar.isEnabled = false
            saveTaskAction()
        }
        return view
    }

    // Limpia los campos
    private fun setupObservers() {
        // Observer de la acción Guardado/Actualización
        taskViewModel.taskSavedEvent.observe(viewLifecycleOwner) { taskIdEvent ->
            taskIdEvent?.let {
                // el taskIdEvent indica éxito y dispara la lógica update/save
                val operationType = if (isEditing) "Actualizada" else "Guardada"

                // Limpia el formulario
                resetFormFields()

                // Redirecciona a la lista de tareas
                mainActivity?.navigateToTaskList()

                Toast.makeText(requireContext(),
                    "Tarea $operationType con éxito.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Observer del statusMessage para ERRORES
        taskViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // Muestra el error
                Toast.makeText(requireContext(), it,
                    Toast.LENGTH_LONG).show()
                // Limpia el mensaje
                taskViewModel.clearStatusMessage()
                // Restaura el botón si la operación falla.
                buttonGrabar.isEnabled = true
            }
        }

        // Observer de carga para restaurar el botón
        taskViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (taskViewModel.taskSavedEvent.value == null && !taskViewModel.
                statusMessage.value.isNullOrBlank()) {
                buttonGrabar.isEnabled = !isLoading
            }
        }
    }

    // Método para guardar/actualizar
    private fun saveTaskAction() {
        val taskName        = editTextTaskName.text.toString().trim()
        val taskDescription = editTextTaskDescription.text.toString().trim()
        val taskStatus      = spinnerStatus.selectedItem?.toString()   ?: "Pendiente"
        val taskCategory    = spinnerCategory.selectedItem?.toString() ?: "Evento"
        val requiresAlarm   = checkBoxRequiresAlarm.isChecked

        // Validación
        if (taskName.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(requireContext(),
                "Debe completar campos obligatorios.",
                Toast.LENGTH_LONG).show()
            buttonGrabar.isEnabled = true
            return
        }

        // Habilitar permisos de notificación
        if (requiresAlarm && android.os.Build.VERSION.SDK_INT >= android.
            os.Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            Toast.makeText(requireContext(),
                "Falta autorizar permiso de notificación. Intente de nuevo.",
                Toast.LENGTH_LONG).show()
            buttonGrabar.isEnabled = true
            return
        }

        // Guarda/Actualiza tarea
        taskViewModel.saveOrUpdateTask(
            id            = taskId,
            name          = taskName,
            description   = taskDescription,
            status        = taskStatus,
            date          = selectedDate,
            time          = selectedTime,
            category      = taskCategory,
            requiresAlarm = requiresAlarm
        )
    }

    // Métodos Auxiliares
    // Limpia los campos
    private fun resetFormFields() {
        editTextTaskName.setText("")
        editTextTaskDescription.setText("")
        editTextTaskDate.setText("")
        editTextTaskTime.setText("")
        checkBoxRequiresAlarm.isChecked = false
        selectedDate = ""
        selectedTime = ""
        (spinnerStatus.adapter as? ArrayAdapter<String>)?.let { adapter ->
            val idx = adapter.getPosition("Pendiente")
            if (idx >= 0) spinnerStatus.setSelection(idx)
        }
        (spinnerCategory.adapter as? ArrayAdapter<String>)?.let { adapter ->
            val idx = adapter.getPosition("Evento")
            if (idx >= 0) spinnerCategory.setSelection(idx)
        }
        taskId = null
        isEditing = false
    }

    // Selector de fecha
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(requireContext(),
            { _, year, month, day ->
                selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                editTextTaskDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    // Selector de hora
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(requireContext(),
            { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                editTextTaskTime.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true)
        timePicker.show()
    }

    // Constantes
    companion object {
        const val TASK_ID_KEY          = "task_id"
        const val TASK_NAME_KEY        = "task_name"
        const val TASK_DESCRIPTION_KEY = "task_description"
        const val TASK_STATUS_KEY      = "task_status"
        const val TASK_DATE_KEY        = "task_date"
        const val TASK_TIME_KEY        = "task_time"
        const val TASK_CATEGORY_KEY    = "task_category"
        const val TASK_ALARM_KEY       = "task_alarm"

        @JvmStatic
        fun newInstanceForEditing(
            taskId         : String,
            taskName       : String,
            taskDescription: String,
            taskStatus     : String,
            taskDate       : String,
            taskTime       : String,
            taskCategory   : String,
            requiresAlarm  : Boolean
        ): CrearTareaFragment {
            val fragment = CrearTareaFragment()
            val args = Bundle().apply {
                putString(TASK_ID_KEY, taskId)
                putString(TASK_NAME_KEY, taskName)
                putString(TASK_DESCRIPTION_KEY, taskDescription)
                putString(TASK_STATUS_KEY, taskStatus)
                putString(TASK_DATE_KEY, taskDate)
                putString(TASK_TIME_KEY, taskTime)
                putString(TASK_CATEGORY_KEY, taskCategory)
                putBoolean(TASK_ALARM_KEY, requiresAlarm)
            }
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstanceForCreation(): CrearTareaFragment = CrearTareaFragment()
    }
}
