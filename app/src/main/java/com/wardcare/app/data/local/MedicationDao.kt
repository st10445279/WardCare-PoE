package com.wardcare.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wardcare.app.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    @Query("SELECT * FROM medication_logs ORDER BY loggedTime DESC")
    fun getAllLogs(): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE patientId = :patientId OR patientName LIKE '%' || :patientName || '%' ORDER BY loggedTime DESC")
    fun getLogsForPatient(patientId: String, patientName: String): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE isSyncPending = 1")
    suspend fun getPendingLogs(): List<MedicationLog>

    @Query("UPDATE medication_logs SET isSyncPending = 0 WHERE uuid = :uuid")
    suspend fun markAsSynced(uuid: String)

    @Query("SELECT COUNT(*) FROM medication_logs WHERE isSyncPending = 1")
    fun getPendingCount(): Flow<Int>
}
