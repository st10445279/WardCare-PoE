package com.wardcare.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.model.Patient
import com.wardcare.app.data.repository.MedicationRepository
import com.wardcare.app.data.repository.PatientRepository
import com.wardcare.app.databinding.FragmentLogMedicationBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogMedicationFragment : Fragment() {

    private var _binding: FragmentLogMedicationBinding? = null
    private val binding get() = _binding!!
    private val adapter = MedicationLogAdapter()

    private val statuses = arrayOf("given", "missed", "delayed")
    private var patientList: List<Patient> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val medRepo = MedicationRepository(database.medicationDao(), com.wardcare.app.data.remote.ApiClient.apiService)
        val patientRepo = PatientRepository(database.patientDao())

        binding.recyclerViewLogsHistory.adapter = adapter

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.statusDropdown.setAdapter(statusAdapter)
        binding.statusDropdown.setText("given", false)

        viewLifecycleOwner.lifecycleScope.launch {
            patientRepo.allPatients.collectLatest { patients ->
                patientList = patients
                val patientNames = patients.map { "${it.bedNumber} - ${it.name}" }
                val patientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patientNames)
                binding.patientDropdown.setAdapter(patientAdapter)
                if (patientNames.isNotEmpty() && binding.patientDropdown.text.isNullOrEmpty()) {
                    binding.patientDropdown.setText(patientNames.first(), false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            medRepo.allLogs.collectLatest { logs ->
                adapter.submitList(logs)
            }
        }

        binding.btnSaveLog.setOnClickListener {
            val selectedPatientText = binding.patientDropdown.text.toString().trim()
            val medication = binding.medicationInput.text.toString().trim()
            val status = binding.statusDropdown.text.toString().trim()
            val note = binding.noteInput.text.toString().trim()

            if (selectedPatientText.isEmpty() || medication.isEmpty() || status.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in patient, medication, and status", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val matchingPatient = patientList.find { "${it.bedNumber} - ${it.name}" == selectedPatientText }
            val patientId = matchingPatient?.patientId ?: "P-100"
            val patientName = matchingPatient?.name ?: selectedPatientText

            val userManager = UserManager(requireContext())
            val log = MedicationLog(
                patientId = patientId,
                patientName = patientName,
                medicationName = medication,
                status = status,
                note = note.ifBlank { null },
                scheduledTime = System.currentTimeMillis(),
                loggedTime = System.currentTimeMillis(),
                staffId = userManager.staffId
            )

            lifecycleScope.launch {
                medRepo.addMedicationLog(log)
                com.wardcare.app.notifications.NotificationHelper.sendMedicationLoggedNotification(
                    context = requireContext(),
                    patientName = patientName,
                    medicationName = medication,
                    status = status
                )
                Toast.makeText(requireContext(), "Medication log saved and notification sent", Toast.LENGTH_SHORT).show()
                binding.medicationInput.setText("")
                binding.noteInput.setText("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
