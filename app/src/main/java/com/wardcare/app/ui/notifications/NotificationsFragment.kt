package com.wardcare.app.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.repository.NotificationRepository
import com.wardcare.app.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val adapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewNotifications.adapter = adapter

        val database = AppDatabase.getDatabase(requireContext())
        val repository = NotificationRepository(database.notificationDao())

        viewLifecycleOwner.lifecycleScope.launch {
            repository.refreshNotifications()
            repository.allNotifications.collectLatest { notifications ->
                adapter.submitList(notifications)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
