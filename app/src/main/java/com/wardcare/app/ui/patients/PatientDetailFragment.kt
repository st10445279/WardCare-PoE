package com.wardcare.app.ui.patients

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.wardcare.app.R
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.repository.MedicationRepository
import com.wardcare.app.data.repository.PatientRepository
import com.wardcare.app.databinding.FragmentPatientDetailBinding
import com.wardcare.app.notifications.NotificationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PatientDetailFragment : Fragment() {

    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val patientId = arguments?.getString("patientId") ?: "patient-1"
        val database = AppDatabase.getDatabase(requireContext())
        val patientRepo = PatientRepository(database.patientDao())
        val medRepo = MedicationRepository(database.medicationDao(), com.wardcare.app.data.remote.ApiClient.apiService)

        viewLifecycleOwner.lifecycleScope.launch {
            patientRepo.getPatientById(patientId).collectLatest { patient ->
                if (patient != null) {
                    binding.tvPatientHeader.text = "${patient.name} — ${patient.bedNumber} (${patient.ward})"
                    binding.tvNextAppointment.text = "Next appointment: ${patient.nextAppointment}"
                    binding.tvMedSchedule.text = "Med schedule: ${patient.medSchedule}"
                    binding.tvPatientId.text = "Patient ID: ${patient.patientId}"
                    binding.tvAgeGenderBlood.text = "Age: ${patient.age} yrs | Gender: ${patient.gender} | Blood: ${patient.bloodType}"
                    binding.tvMedicalHistory.text = "History: ${patient.medicalHistory}"
                    binding.tvAllergies.text = "Allergies: ${patient.allergies}"

                    binding.btnMarkDoseGiven.setOnClickListener {
                        val userManager = UserManager(requireContext())
                        val log = MedicationLog(
                            patientId = patient.patientId,
                            patientName = patient.name,
                            medicationName = patient.medSchedule,
                            status = "given",
                            scheduledTime = System.currentTimeMillis(),
                            loggedTime = System.currentTimeMillis(),
                            staffId = userManager.staffId
                        )
                        lifecycleScope.launch {
                            medRepo.addMedicationLog(log)
                            NotificationHelper.sendMedicationDueReminder(
                                requireContext(),
                                patient.name,
                                patient.bedNumber,
                                patient.medSchedule
                            )
                            Toast.makeText(requireContext(), "Dose marked as given for ${patient.name}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    binding.btnViewFullHistory.setOnClickListener {
                        val bundle = bundleOf("patientId" to patient.id)
                        findNavController().navigate(R.id.patientHistoryFragment, bundle)
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
