package com.wardcare.app.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.data.repository.UserRepository
import com.wardcare.app.databinding.FragmentUserProfileBinding
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun loadProfile() {
        val userManager = UserManager(requireContext())
        binding.tvProfileName.text = userManager.staffName
        binding.tvProfileStaffName.text = "Staff Name: ${userManager.staffName}"
        binding.tvProfileRole.text = "${userManager.userRole} — ${userManager.userWard}"
        binding.tvProfileEmail.text = "Email: ${userManager.userEmail}"
        binding.tvProfilePhone.text = "Phone: ${userManager.userPhone}"
        binding.tvProfileWard.text = "Ward / Department: ${userManager.userWard}"
        binding.tvProfileStaffId.text = "Staff ID: ${userManager.staffId}"
    }

    private fun showEditProfileDialog() {
        val context = requireContext()
        val userManager = UserManager(context)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etStaffName = EditText(context).apply {
            setText(userManager.staffName)
            hint = "Staff Name"
            setSingleLine()
        }

        val etStaffId = EditText(context).apply {
            setText(userManager.staffId)
            hint = "Staff ID"
            setSingleLine()
        }

        val etPhone = EditText(context).apply {
            setText(userManager.userPhone)
            hint = "Phone Number"
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine()
        }

        val etWard = EditText(context).apply {
            setText(userManager.userWard)
            hint = "Ward / Department"
            setSingleLine()
        }

        layout.addView(etStaffName)
        layout.addView(etStaffId)
        layout.addView(etPhone)
        layout.addView(etWard)

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit Staff Profile")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = etStaffName.text.toString().trim()
                val newId = etStaffId.text.toString().trim()
                val newPhone = etPhone.text.toString().trim()
                val newWard = etWard.text.toString().trim()

                if (newName.isNotEmpty()) {
                    userManager.staffName = newName
                    if (newId.isNotEmpty()) userManager.staffId = newId
                    if (newPhone.isNotEmpty()) userManager.userPhone = newPhone
                    if (newWard.isNotEmpty()) userManager.userWard = newWard

                    loadProfile()

                    val database = AppDatabase.getDatabase(context)
                    val userRepository = UserRepository(database.userDao())

                    viewLifecycleOwner.lifecycleScope.launch {
                        userRepository.updateProfile(
                            name = userManager.staffName,
                            email = userManager.userEmail,
                            ward = userManager.userWard,
                            role = userManager.userRole,
                            staffId = userManager.staffId,
                            phone = userManager.userPhone
                        )
                        Toast.makeText(context, "Staff Profile updated & synced", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Staff Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}