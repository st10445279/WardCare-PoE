package com.wardcare.app.data.repository

import android.util.Log
import com.wardcare.app.data.local.MedicationDao
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.remote.ApiClient
import com.wardcare.app.data.remote.ApiService
import com.wardcare.app.data.remote.FirestoreMapper
import com.wardcare.app.data.remote.FirestorePaths
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first medication logging (single source of truth = Room):
 * 1. a log is written to Room with isSyncPending = true (works with no connection),
 * 2. pending logs are pushed to Firestore in atomic batches of 20,
 * 3. a log is only flagged as synced after the server answers HTTP 200, so nothing is lost offline.
 */
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val apiService: ApiService = ApiClient.apiService,
    private val projectIdProvider: () -> String = { ApiClient.projectId }
) {
    companion object {
        private const val TAG = "MedicationRepository"
        private const val COLLECTION = "medicationLogs"
        const val BATCH_SIZE = 20
    }

    val allLogs: Flow<List<MedicationLog>> = medicationDao.getAllLogs()
    val pendingCount: Flow<Int> = medicationDao.getPendingCount()

    suspend fun addMedicationLog(log: MedicationLog) {
        medicationDao.insertLog(log)
        Log.d(TAG, "addMedicationLog: saved locally uuid=${log.uuid}, status=${log.status}, pending sync")
        syncPendingLogs() // try straight away; if offline the log simply stays queued
    }

    /** Pulls logs from Firestore into Room. Local data is kept if the network call fails. */
    suspend fun refreshLogs() {
        try {
            val path = FirestorePaths.collection(projectIdProvider(), COLLECTION)
            val response = apiService.listDocuments(path, 300, null)
            if (response.isSuccessful) {
                val remote = FirestoreMapper.fieldsOfDocuments(response.body())
                    .mapNotNull { FirestoreMapper.medicationLogFromFields(it) }
                remote.forEach { medicationDao.insertLog(it) }
                Log.i(TAG, "refreshLogs: pulled ${remote.size} logs from Firestore")
            } else {
                Log.w(TAG, "refreshLogs: HTTP ${response.code()}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "refreshLogs: offline, keeping cached logs (${e.javaClass.simpleName})")
        }
    }

    suspend fun syncPendingLogs() {
        val pending = medicationDao.getPendingLogs()
        if (pending.isEmpty()) return
        Log.d(TAG, "syncPendingLogs: ${pending.size} log(s) waiting")

        val projectId = projectIdProvider()
        pending.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val writes = batch.map {
                    FirestoreMapper.updateWrite(
                        FirestorePaths.document(projectId, COLLECTION, it.uuid),
                        FirestoreMapper.medicationLogToDocument(it)
                    )
                }
                val response = apiService.commit(
                    FirestorePaths.database(projectId),
                    FirestoreMapper.commitBody(writes)
                )
                if (response.isSuccessful) {
                    batch.forEach { medicationDao.markAsSynced(it.uuid) }
                    Log.i(TAG, "syncPendingLogs: synced batch of ${batch.size}")
                } else {
                    Log.w(TAG, "syncPendingLogs: HTTP ${response.code()}, ${batch.size} stay queued")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "syncPendingLogs: no connection, ${batch.size} stay queued (${e.javaClass.simpleName})")
            }
        }
    }
}