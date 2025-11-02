package com.mod5.evalfinal_gestareav5.ui

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

    // --- Propiedades de Vistas (NOMBRES ESTABLES) ---
    private lateinit var taskViewModel: TaskViewModel

    private lateinit var editTextTaskName: EditText
    private lateinit var editTextTaskDescription: EditText
    private lateinit var editTextTaskDate: EditText
    private lateinit var editTextTaskTime: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var checkBoxRequiresAlarm: CheckBox
    private lateinit var buttonGrabar: Button // Agregada al ámbito de la clase para habilitación/deshabilitación

    // --- Estado de Edición/Datos ---
    private var taskId: String? = null
    private var isEditing: Boolean = false
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var mainActivity: MainActivity? = null // Añadida para facilitar el uso de navigateToTaskList()

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
        val view = inflater.inflate(R.layout.crear_tarea, container, false)

        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        mainActivity = activity as? MainActivity // Inicializar Activity principal

        // 1. Mapeo de Vistas (USANDO TUS IDs EXACTOS)
        editTextTaskName = view.findViewById(R.id.editTextTaskName)
        editTextTaskDescription = view.findViewById(R.id.editTextTaskDescription)
        editTextTaskDate = view.findViewById(R.id.editTextTaskDate)
        editTextTaskTime = view.findViewById(R.id.editTextTaskTime)
        spinnerStatus = view.findViewById(R.id.spinnerStatus)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        checkBoxRequiresAlarm = view.findViewById(R.id.checkBoxRequiresAlarm)
        buttonGrabar = view.findViewById(R.id.buttonSaveTask) // Ahora es una propiedad de clase

        // 2. Listeners de Fecha/Hora
        editTextTaskDate.setOnClickListener { showDatePickerDialog() }
        editTextTaskTime.setOnClickListener { showTimePickerDialog() }

        // 3. Carga de Edición (Lógica original sin cambios)
        arguments?.let { args ->
            isEditing = true
            taskId = args.getString(TASK_ID_KEY)
            editTextTaskName.setText(args.getString(TASK_NAME_KEY) ?: "")
            editTextTaskDescription.setText(args.getString(TASK_DESCRIPTION_KEY) ?: "")
            selectedDate = args.getString(TASK_DATE_KEY) ?: ""
            selectedTime = args.getString(TASK_TIME_KEY) ?: ""
            editTextTaskDate.setText(selectedDate)
            editTextTaskTime.setText(selectedTime)
            checkBoxRequiresAlarm.isChecked = args.getBoolean(TASK_ALARM_KEY, false)

            (spinnerStatus.adapter as? ArrayAdapter<String>)?.let { adapter ->
                args.getString(TASK_STATUS_KEY)?.let { desired ->
                    adapter.getPosition(desired).takeIf { it >= 0 }?.let { spinnerStatus.setSelection(it) }
                }
            }
            (spinnerCategory.adapter as? ArrayAdapter<String>)?.let { adapter ->
                args.getString(TASK_CATEGORY_KEY)?.let { desired ->
                    adapter.getPosition(desired).takeIf { it >= 0 }?.let { spinnerCategory.setSelection(it) }
                }
            }

            buttonGrabar.text = "Actualizar Tarea"
        } ?: run {
            buttonGrabar.text = "Guardar Tarea"
        }

        // 4. Observadores (Lógica V5.0)
        setupObservers()

        // 5. Listener de Guardado
        buttonGrabar.setOnClickListener {
            // Deshabilitar para prevenir doble click mientras corre la corrutina
            buttonGrabar.isEnabled = false
            saveTaskAction()
        }
        return view
    }

    // --- Lógica de V5.0 (Observadores Limpios) ---
    private fun setupObservers() {
        // ⭐ 1. Observador del Evento de Guardado/Actualización (Navegación y Limpieza)
        taskViewModel.taskSavedEvent.observe(viewLifecycleOwner) { taskIdEvent ->
            taskIdEvent?.let {
                // La presencia de taskIdEvent (String) indica éxito y dispara la lógica
                val operationType = if (isEditing) "Actualizada" else "Guardada"

                // 1️⃣ Limpiar formulario
                resetFormFields()

                // 2️⃣ Navegar de vuelta a la lista de tareas
                mainActivity?.navigateToTaskList()

                Toast.makeText(requireContext(),
                    "Tarea $operationType con éxito.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Observación de statusMessage para ERRORES (Se usa para feedback de validación/persistncia)
        taskViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // Se muestra el error
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // Se limpia el mensaje
                taskViewModel.clearStatusMessage()
                // Y se re-habilita el botón si la operación falló.
                buttonGrabar.isEnabled = true
            }
        }

        // 3. Observación de Carga para re-habilitar el botón
        taskViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Esto evita que el usuario toque el botón mientras la corrutina de guardado está activa.
            // Si no estamos en un estado de éxito (navegando), mantenemos el estado del botón.
            if (taskViewModel.taskSavedEvent.value == null && !taskViewModel.statusMessage.value.isNullOrBlank()) {
                buttonGrabar.isEnabled = !isLoading
            }
        }
    }

    // --- Lógica de Guardado (Refactorizada a un método) ---
    private fun saveTaskAction() {
        val taskName = editTextTaskName.text.toString().trim()
        val taskDescription = editTextTaskDescription.text.toString().trim()
        val taskStatus = spinnerStatus.selectedItem?.toString() ?: "Pendiente"
        val taskCategory = spinnerCategory.selectedItem?.toString() ?: "Evento"
        val requiresAlarm = checkBoxRequiresAlarm.isChecked

        // Validación
        if (taskName.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(requireContext(),
                "Debe completar campos obligatorios.",
                Toast.LENGTH_LONG).show()
            buttonGrabar.isEnabled = true // Re-habilitar si falla validación local
            return
        }

        // Permisos de notificación (si aplica)
        if (requiresAlarm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            Toast.makeText(requireContext(),
                "Falta autorizar permiso de notificación. Intente de nuevo.",
                Toast.LENGTH_LONG).show()
            buttonGrabar.isEnabled = true // Re-habilitar si falta permiso
            return
        }

        // Guardar o actualizar tarea (Llamada al ViewModel V5.0)
        taskViewModel.saveOrUpdateTask(
            id = taskId,
            name = taskName,
            description = taskDescription,
            status = taskStatus,
            date = selectedDate,
            time = selectedTime,
            category = taskCategory,
            requiresAlarm = requiresAlarm
        )
        // El botón será re-habilitado por el observador de statusMessage o deshabilitado
        // por el observador de isLoading si la operación es exitosa/falla en el ViewModel.
    }

    // --- Métodos Auxiliares ---
    private fun resetFormFields() {
        // ... (Tu lógica de reseteo original sin cambios)
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

    // --- Companion Object (sin cambios) ---
    companion object {
        const val TASK_ID_KEY = "task_id"
        const val TASK_NAME_KEY = "task_name"
        const val TASK_DESCRIPTION_KEY = "task_description"
        const val TASK_STATUS_KEY = "task_status"
        const val TASK_DATE_KEY = "task_date"
        const val TASK_TIME_KEY = "task_time"
        const val TASK_CATEGORY_KEY = "task_category"
        const val TASK_ALARM_KEY = "task_alarm"

        @JvmStatic
        fun newInstanceForEditing(
            taskId: String,
            taskName: String,
            taskDescription: String,
            taskStatus: String,
            taskDate: String,
            taskTime: String,
            taskCategory: String,
            requiresAlarm: Boolean
        ): com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment {
            val fragment = CrearTareaFragment()
            val args = Bundle().apply {
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_ID_KEY, taskId)
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_NAME_KEY, taskName)
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_DESCRIPTION_KEY, taskDescription)
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_STATUS_KEY, taskStatus)
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_DATE_KEY, taskDate)
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_TIME_KEY, taskTime)
                putString(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_CATEGORY_KEY, taskCategory)
                putBoolean(com.mod5.evalfinal_gestareav5.ui.CrearTareaFragment.Companion.TASK_ALARM_KEY, requiresAlarm)
            }
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstanceForCreation(): CrearTareaFragment = CrearTareaFragment()
    }
}
