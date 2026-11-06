package com.wardcare.app.ui.patients

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wardcare.app.R
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.model.Patient
import com.wardcare.app.data.repository.PatientRepository
import com.wardcare.app.databinding.DialogAddPatientBinding
import com.wardcare.app.databinding.FragmentPatientListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class PatientListFragment : Fragment() {

    private var _binding: FragmentPatientListBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PatientRepository
    private lateinit var adapter: PatientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        repository = PatientRepository(database.patientDao())

        viewLifecycleOwner.lifecycleScope.launch {
            repository.refreshPatients()
        }

        adapter = PatientAdapter { patient ->
            val bundle = bundleOf("patientId" to patient.id)
            findNavController().navigate(R.id.patientDetailFragment, bundle)
        }

        binding.recyclerViewPatients.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadPatients(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnAddPatient.setOnClickListener {
            showAddPatientDialog()
        }

        loadPatients("")
    }

    private fun loadPatients(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.searchPatients(query).collectLatest { patients ->
                adapter.submitList(patients)
            }
        }
    }

    private fun showAddPatientDialog() {
        val dialogBinding = DialogAddPatientBinding.inflate(layoutInflater)

        // Dropdown options
        val wards = arrayOf("General Ward", "ICU", "Pediatrics", "Maternity", "Emergency")
        val genders = arrayOf("Male", "Female", "Other")
        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val timeSlotsList = arrayOf(
            "08:00 AM, 04:00 PM, 08:00 PM",
            "09:00 AM, 05:00 PM",
            "08:00 AM, 12:00 PM, 04:00 PM, 08:00 PM",
            "10:00 AM, 10:00 PM"
        )

        dialogBinding.etWard.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, wards)
        )
        dialogBinding.etWard.setText("General Ward", false)

        dialogBinding.etGender.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genders)
        )
        dialogBinding.etGender.setText("Male", false)

        dialogBinding.etBloodType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes)
        )
        dialogBinding.etBloodType.setText("O+", false)

        dialogBinding.etTimeSlots.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, timeSlotsList)
        )
        dialogBinding.etTimeSlots.setText(timeSlotsList.first(), false)

        var selectedScheduleDate = "2025-03-30"
        var selectedAppointmentTime = "14:00 PM"

        dialogBinding.btnSelectScheduleDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedScheduleDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    dialogBinding.btnSelectScheduleDate.text = "Date: $selectedScheduleDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnSelectAppointmentDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedAppointmentTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                    dialogBinding.btnSelectAppointmentDate.text = "Appointment: $selectedAppointmentTime"
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Register", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val ward = dialogBinding.etWard.text.toString().trim()
                val bedNumber = dialogBinding.etBedNumber.text.toString().trim()
                val name = dialogBinding.etPatientName.text.toString().trim()
                val patientId = dialogBinding.etPatientId.text.toString().trim()
                val age = dialogBinding.etAge.text.toString().toIntOrNull() ?: 40
                val gender = dialogBinding.etGender.text.toString().trim()
                val bloodType = dialogBinding.etBloodType.text.toString().trim()
                val history = dialogBinding.etMedicalHistory.text.toString().trim().ifEmpty { "None" }
                val allergies = dialogBinding.etAllergies.text.toString().trim().ifEmpty { "None" }
                val chosenSlots = dialogBinding.etTimeSlots.text.toString().trim()

                val fullSchedule = "$selectedScheduleDate ($chosenSlots)"

                if (bedNumber.isEmpty() || name.isEmpty()) {
                    Toast.makeText(requireContext(), "Bed Number and Name are required", Toast.LENGTH_SHORT).show()
                } else {
                    val newPatient = Patient(
                        patientId = if (patientId.isEmpty()) "P-${(100..999).random()}" else patientId,
                        name = name,
                        bedNumber = bedNumber,
                        ward = ward.ifEmpty { "General Ward" },
                        age = age,
                        gender = gender.ifEmpty { "Male" },
                        bloodType = bloodType.ifEmpty { "O+" },
                        medicalHistory = history,
                        allergies = allergies,
                        medSchedule = fullSchedule,
                        nextAppointment = selectedAppointmentTime
                    )
                    lifecycleScope.launch {
                        repository.addPatient(newPatient)
                        Toast.makeText(requireContext(), "Patient registered successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
