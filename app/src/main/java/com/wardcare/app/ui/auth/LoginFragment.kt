package com.wardcare.app.ui.auth

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wardcare.app.R
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.databinding.FragmentLoginBinding
import com.wardcare.app.util.InputValidator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    // Result of the Google account picker (Google single sign-on)
    // Reference: https://firebase.google.com/docs/auth/android/google-signin
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Log.w(TAG, "Google sign-in returned no ID token")
                Toast.makeText(requireContext(), getString(R.string.error_sso_failed), Toast.LENGTH_LONG).show()
            } else {
                viewModel.loginWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign-in failed, status code ${e.statusCode}")
            Toast.makeText(
                requireContext(),
                "Google sign-in failed (code ${e.statusCode}). Check the SHA-1 fingerprint in Firebase.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            binding.emailLayout.error =
                if (InputValidator.isValidEmail(email)) null else "Enter a valid email address"
            binding.passwordLayout.error =
                if (password.isNotEmpty()) null else getString(R.string.error_field_required)

            if (binding.emailLayout.error == null && binding.passwordLayout.error == null) {
                viewModel.login(email, password)
            }
        }

        binding.googleSsoButton.setOnClickListener {
            // Sign out first so the account picker is always shown
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        binding.goToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        observeViewModel()
    }

    private fun setupGoogleSignIn() {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
        // default_web_client_id is generated from google-services.json
        val id = requireContext().resources.getIdentifier(
            "default_web_client_id", "string", requireContext().packageName
        )
        if (id != 0) {
            builder.requestIdToken(getString(id))
        } else {
            Log.e(TAG, "default_web_client_id missing: enable Google sign-in in Firebase and re-download google-services.json")
        }
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), builder.build())
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter your registered email"
            setText(binding.emailInput.text.toString().trim())
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password")
            .setMessage("We will email you a secure link to choose a new password.")
            .setView(container)
            .setPositiveButton("Send reset link") { dialog, _ ->
                val email = input.text.toString().trim()
                if (InputValidator.isValidEmail(email)) {
                    viewModel.sendPasswordResetEmail(email)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthViewModel.AuthState.Loading -> {
                        binding.loginButton.isEnabled = false
                    }
                    is AuthViewModel.AuthState.Success -> {
                        binding.loginButton.isEnabled = true
                        val userManager = UserManager(requireContext())
                        userManager.isLoggedIn = true
                        userManager.userEmail = state.user.email
                        userManager.userName = state.user.name
                        userManager.userWard = state.user.ward
                        userManager.userRole = state.user.role
                        userManager.staffId = state.user.staffId
                        userManager.userPhone = state.user.phoneNumber

                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_homeDashboardFragment)
                        viewModel.resetState()
                    }
                    is AuthViewModel.AuthState.Error -> {
                        binding.loginButton.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> binding.loginButton.isEnabled = true
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetTokenState.collectLatest { state ->
                when (state) {
                    is AuthViewModel.ResetTokenState.Sent -> {
                        Toast.makeText(
                            requireContext(),
                            "Reset link sent to ${state.email}. Check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetState()
                    }
                    is AuthViewModel.ResetTokenState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
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