package com.wardcare.app.ui.patients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.repository.PatientRepository
import com.wardcare.app.databinding.FragmentPatientHistoryBinding
import com.wardcare.app.ui.dashboard.MedicationLogAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PatientHistoryFragment : Fragment() {

    private var _binding: FragmentPatientHistoryBinding? = null
    private val binding get() = _binding!!
    private val adapter = MedicationLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val patientId = arguments?.getString("patientId") ?: "patient-1"
        val database = AppDatabase.getDatabase(requireContext())
        val patientRepo = PatientRepository(database.patientDao())

        binding.recyclerViewPatientHistoryLogs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            patientRepo.getPatientById(patientId).collectLatest { patient ->
                if (patient != null) {
                    binding.tvHistoryPatientHeader.text = "${patient.name} — ${patient.bedNumber} (${patient.ward})"
                    binding.tvHistoryPatientId.text = "Patient ID: ${patient.patientId}"
                    binding.tvHistoryDemographics.text = "Age: ${patient.age} yrs | Gender: ${patient.gender} | Blood: ${patient.bloodType}"
                    binding.tvHistoryMedicalHistory.text = "Medical History: ${patient.medicalHistory}"
                    binding.tvHistoryAllergies.text = "Allergies: ${patient.allergies}"
                    binding.tvHistorySchedule.text = "Active Med Schedule: ${patient.medSchedule}"
                    binding.tvHistoryAppointment.text = "Next Appointment: ${patient.nextAppointment}"

                    launch {
                        database.medicationDao().getLogsForPatient(patient.patientId, patient.name)
                            .collectLatest { logs ->
                                adapter.submitList(logs)
                            }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
