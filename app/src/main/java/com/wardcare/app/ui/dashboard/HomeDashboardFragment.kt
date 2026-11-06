package com.wardcare.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.wardcare.app.R
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.databinding.FragmentHomeDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userManager = UserManager(requireContext())
        binding.tvGreeting.text = "Hello, ${userManager.userName}"

        val database = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            database.medicationDao().getPendingCount().collectLatest { count ->
                binding.tvOfflineQueue.text = "Offline queue: $count pending sync"
            }
        }

        binding.btnLogMedication.setOnClickListener {
            findNavController().navigate(R.id.logMedicationFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
