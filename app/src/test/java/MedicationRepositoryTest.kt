package com.wardcare.app.data.repository

import com.google.gson.JsonObject
import com.wardcare.app.data.local.MedicationDao
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class MedicationRepositoryTest {

    /** In-memory stand-in for the Room DAO. */
    private class FakeMedicationDao : MedicationDao {
        val logs = mutableListOf<MedicationLog>()
        override suspend fun insertLog(log: MedicationLog) {
            logs.removeAll { it.uuid == log.uuid }
            logs.add(log)
        }
        override fun getAllLogs(): Flow<List<MedicationLog>> = flowOf(logs.toList())
        override fun getLogsForPatient(patientId: String, patientName: String): Flow<List<MedicationLog>> =
            flowOf(logs.filter { it.patientId == patientId })
        override suspend fun getPendingLogs(): List<MedicationLog> = logs.filter { it.isSyncPending }
        override suspend fun markAsSynced(uuid: String) {
            val i = logs.indexOfFirst { it.uuid == uuid }
            if (i >= 0) logs[i] = logs[i].copy(isSyncPending = false)
        }
        override fun getPendingCount(): Flow<Int> = flowOf(logs.count { it.isSyncPending })
    }

    /** Fake REST API: records the size of each batch and returns whatever the test asks for. */
    private class FakeApi(private val onCommit: () -> Response<JsonObject>) : ApiService {
        val batchSizes = mutableListOf<Int>()
        override suspend fun commit(database: String, body: JsonObject): Response<JsonObject> {
            batchSizes.add(body.getAsJsonArray("writes").size())
            return onCommit()
        }
        override suspend fun upsertDocument(path: String, body: JsonObject) = Response.success(JsonObject())
        override suspend fun getDocument(path: String) = Response.success(JsonObject())
        override suspend fun listDocuments(path: String, pageSize: Int, pageToken: String?) =
            Response.success(JsonObject())
    }

    private fun log(id: String) = MedicationLog(
        uuid = id, patientId = "P-101", patientName = "J. Nkosi", medicationName = "Amoxicillin",
        status = "given", scheduledTime = 1L, loggedTime = 2L, staffId = "NURSE-1"
    )

    private fun repo(dao: MedicationDao, api: ApiService) =
        MedicationRepository(dao, api, projectIdProvider = { "test-project" })

    @Test
    fun online_logIsSavedAndMarkedSynced() = runBlocking {
        val dao = FakeMedicationDao()
        repo(dao, FakeApi { Response.success(JsonObject()) }).addMedicationLog(log("a"))

        assertEquals(1, dao.logs.size)
        assertTrue(dao.getPendingLogs().isEmpty())
    }

    @Test
    fun offline_logStaysQueuedAndNothingCrashes() = runBlocking {
        val dao = FakeMedicationDao()
        repo(dao, FakeApi { throw IOException("no connection") }).addMedicationLog(log("a"))

        assertEquals(1, dao.getPendingLogs().size)
    }

    @Test
    fun serverError_keepsLogsQueued() = runBlocking {
        val dao = FakeMedicationDao()
        val error = Response.error<JsonObject>(500, "".toResponseBody("application/json".toMediaType()))
        repo(dao, FakeApi { error }).addMedicationLog(log("a"))

        assertEquals(1, dao.getPendingLogs().size)
    }

    @Test
    fun pendingLogs_areSentInBatchesOfTwenty() = runBlocking {
        val dao = FakeMedicationDao()
        (1..45).forEach { dao.insertLog(log("log-$it")) }
        val api = FakeApi { Response.success(JsonObject()) }

        repo(dao, api).syncPendingLogs()

        assertEquals(listOf(20, 20, 5), api.batchSizes)
        assertTrue(dao.getPendingLogs().isEmpty())
    }
}