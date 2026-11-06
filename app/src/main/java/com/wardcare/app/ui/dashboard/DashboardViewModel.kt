package com.wardcare.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.remote.ApiClient
import com.wardcare.app.data.repository.MedicationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository

    val allLogs: StateFlow<List<MedicationLog>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MedicationRepository(database.medicationDao(), ApiClient.apiService)
        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        viewModelScope.launch {
            repository.refreshLogs()
            repository.syncPendingLogs()
        }
    }

    fun addLog(patientId: String, patientName: String, medicationName: String, status: String, note: String?) {
        viewModelScope.launch {
            val log = MedicationLog(
                patientId = patientId,
                patientName = patientName,
                medicationName = medicationName,
                status = status,
                note = note?.ifBlank { null },
                scheduledTime = System.currentTimeMillis(),
                loggedTime = System.currentTimeMillis(),
                staffId = "STAFF-01" // Current staff session
            )
            repository.addMedicationLog(log)
            com.wardcare.app.notifications.NotificationHelper.sendMedicationLoggedNotification(
                context = getApplication(),
                patientName = patientName,
                medicationName = medicationName,
                status = status
            )
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncPendingLogs()
        }
    }
}
