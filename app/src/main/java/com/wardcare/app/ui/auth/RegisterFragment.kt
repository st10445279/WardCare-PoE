package com.wardcare.app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.wardcare.app.R
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.databinding.FragmentRegisterBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.wardcare.app.util.InputValidator

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    private val wards = arrayOf("General Ward", "ICU", "Pediatrics", "Maternity", "Emergency")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWardDropdown()

        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
                val name = binding.nameInput.text.toString().trim()
                val email = binding.emailInput.text.toString().trim()
                val phone = binding.phoneInput.text.toString().trim()
                val ward = binding.wardDropdown.text.toString().trim()

                viewModel.register(
                    name = name,
                    email = email,
                    ward = ward,
                    password = binding.passwordInput.text.toString(),
                    phoneNumber = phone
                )
            }
        }

        binding.goToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        observeViewModel()
    }

    private fun setupWardDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, wards)
        binding.wardDropdown.setAdapter(adapter)
    }

    private fun validateInputs(): Boolean {
        val name = binding.nameInput.text.toString()
        val phone = binding.phoneInput.text.toString()
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val confirm = binding.confirmPasswordInput.text.toString()

        binding.nameLayout.error =
            if (InputValidator.isValidName(name)) null else "Name must be 2-60 characters"
        binding.phoneLayout.error =
            if (InputValidator.isValidPhone(phone)) null else getString(R.string.error_invalid_phone)
        binding.emailLayout.error =
            if (InputValidator.isValidEmail(email)) null else "Invalid email format"
        binding.passwordLayout.error =
            if (InputValidator.isValidPassword(password)) null else getString(R.string.error_password_requirement)
        binding.confirmPasswordLayout.error =
            if (InputValidator.passwordsMatch(password, confirm)) null else getString(R.string.error_passwords_dont_match)

        return listOf(
            binding.nameLayout, binding.phoneLayout, binding.emailLayout,
            binding.passwordLayout, binding.confirmPasswordLayout
        ).all { it.error == null }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthViewModel.AuthState.Loading -> {
                        binding.registerButton.isEnabled = false
                    }
                    is AuthViewModel.AuthState.Success -> {
                        binding.registerButton.isEnabled = true
                        val userManager = UserManager(requireContext())
                        userManager.userEmail = state.user.email
                        userManager.userName = state.user.name
                        userManager.userWard = state.user.ward
                        userManager.userRole = state.user.role
                        userManager.staffId = state.user.staffId
                        userManager.userPhone = state.user.phoneNumber

                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                    }
                    is AuthViewModel.AuthState.Error -> {
                        binding.registerButton.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
