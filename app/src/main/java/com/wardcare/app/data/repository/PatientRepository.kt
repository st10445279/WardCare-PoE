package com.wardcare.app.data.repository

import android.util.Log
import com.wardcare.app.data.local.PatientDao
import com.wardcare.app.data.model.Patient
import com.wardcare.app.data.remote.ApiClient
import com.wardcare.app.data.remote.ApiService
import com.wardcare.app.data.remote.FirestoreMapper
import com.wardcare.app.data.remote.FirestorePaths
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PatientRepository(
    private val patientDao: PatientDao,
    private val apiService: ApiService = ApiClient.apiService
) {
    companion object {
        private const val TAG = "PatientRepository"
        private const val COLLECTION = "patients"
    }

    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()

    fun searchPatients(query: String): Flow<List<Patient>> {
        return if (query.isBlank()) patientDao.getAllPatients() else patientDao.searchPatients(query)
    }

    fun getPatientById(id: String): Flow<Patient?> = patientDao.getPatientById(id)

    suspend fun addPatient(patient: Patient) {
        patientDao.insertPatient(patient)
        Log.d(TAG, "addPatient: saved locally id=${patient.id}")
        try {
            push(patient)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not lost: refreshPatients() uploads local-only patients next time we are online
            Log.w(TAG, "addPatient: offline, will upload on next refresh (${e.javaClass.simpleName})")
        }
    }

    /** Pulls patients from Firestore and uploads any that only exist on this device. */
    suspend fun refreshPatients() {
        try {
            val path = FirestorePaths.collection(ApiClient.projectId, COLLECTION)
            val response = apiService.listDocuments(path, 300, null)
            if (!response.isSuccessful) {
                Log.w(TAG, "refreshPatients: HTTP ${response.code()}")
                return
            }
            val remote = FirestoreMapper.fieldsOfDocuments(response.body())
                .mapNotNull { FirestoreMapper.patientFromFields(it) }
            val remoteIds = remote.map { it.id }.toSet()

            val localOnly = patientDao.getAllPatients().first().filter { it.id !in remoteIds }
            localOnly.forEach { push(it) }
            if (remote.isNotEmpty()) patientDao.insertPatients(remote)
            Log.i(TAG, "refreshPatients: pulled ${remote.size}, uploaded ${localOnly.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "refreshPatients: offline, keeping local patients (${e.javaClass.simpleName})")
        }
    }

    private suspend fun push(patient: Patient) {
        val path = FirestorePaths.document(ApiClient.projectId, COLLECTION, patient.id)
        val response = apiService.upsertDocument(path, FirestoreMapper.patientToDocument(patient))
        if (!response.isSuccessful) Log.w(TAG, "push: HTTP ${response.code()} for patient ${patient.id}")
    }
}