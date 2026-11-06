package com.wardcare.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wardcare.app.R
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userManager = UserManager(requireContext())
        binding.switchNotifications.isChecked = userManager.notificationsEnabled

        val currentLang = userManager.selectedLanguage
        binding.tvLanguageSetting.text = "Language: $currentLang"
        binding.tvProfileSetting.text = "Staff Profile: ${userManager.staffName}"
        binding.tvWardSetting.text = "Ward: ${userManager.userWard}"

        binding.cardProfile.setOnClickListener {
            findNavController().navigate(R.id.userProfileFragment)
        }

        binding.cardWard.setOnClickListener {
            showWardDialog(userManager)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            userManager.notificationsEnabled = isChecked
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(requireContext(), "Notifications $status", Toast.LENGTH_SHORT).show()
        }

        binding.cardLanguage.setOnClickListener {
            showLanguageDialog(userManager)
        }

        binding.btnLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            userManager.logout()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }

    private fun showWardDialog(userManager: UserManager) {
        val wards = arrayOf("General Ward", "ICU", "Pediatrics", "Maternity", "Emergency")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Ward / Department")
            .setItems(wards) { _, which ->
                val selectedWard = wards[which]
                userManager.userWard = selectedWard
                Toast.makeText(requireContext(), "Ward changed to $selectedWard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showLanguageDialog(userManager: UserManager) {
        val languages = arrayOf("English (EN)", "isiZulu", "Afrikaans")
        val codes = arrayOf("en", "zu", "af")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Language")
            .setItems(languages) { _, which ->
                val selectedLangName = languages[which]
                val selectedCode = codes[which]

                userManager.selectedLanguage = selectedLangName

                // Set application locales dynamically
                val appLocales = LocaleListCompat.forLanguageTags(selectedCode)
                AppCompatDelegate.setApplicationLocales(appLocales)

                binding.tvLanguageSetting.text = "Language: $selectedLangName"
                Toast.makeText(requireContext(), "Language set to $selectedLangName", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            val userManager = UserManager(requireContext())
            binding.tvProfileSetting.text = "Staff Profile: ${userManager.staffName}"
            binding.tvWardSetting.text = "Ward: ${userManager.userWard}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
