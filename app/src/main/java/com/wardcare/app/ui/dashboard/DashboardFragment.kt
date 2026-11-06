package com.wardcare.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wardcare.app.R
import com.wardcare.app.databinding.DialogAddMedicationLogBinding
import com.wardcare.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val adapter = MedicationLogAdapter()

    private val statuses = arrayOf("given", "missed", "delayed")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewLogs.adapter = adapter

        binding.fabAddLog.setOnClickListener {
            showAddLogDialog()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLogs.collectLatest { logs ->
                adapter.submitList(logs)
                if (logs.isEmpty()) {
                    binding.emptyStateView.visibility = View.VISIBLE
                    binding.recyclerViewLogs.visibility = View.GONE
                } else {
                    binding.emptyStateView.visibility = View.GONE
                    binding.recyclerViewLogs.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showAddLogDialog() {
        val dialogBinding = DialogAddMedicationLogBinding.inflate(layoutInflater)

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        dialogBinding.statusDropdown.setAdapter(statusAdapter)
        dialogBinding.statusDropdown.setText("given", false)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val patientId = dialogBinding.patientIdInput.text.toString().trim()
                val patientName = dialogBinding.patientNameInput.text.toString().trim()
                val medicationName = dialogBinding.medicationNameInput.text.toString().trim()
                val status = dialogBinding.statusDropdown.text.toString().trim()
                val note = dialogBinding.notesInput.text.toString().trim()

                if (patientId.isEmpty() || patientName.isEmpty() || medicationName.isEmpty() || status.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addLog(
                        patientId = patientId,
                        patientName = patientName,
                        medicationName = medicationName,
                        status = status,
                        note = note
                    )
                    Toast.makeText(requireContext(), "Medication log added", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
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
