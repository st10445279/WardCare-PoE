package com.wardcare.app.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.model.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreMapperTest {

    private val log = MedicationLog(
        uuid = "log-1",
        patientId = "P-101",
        patientName = "J. Nkosi",
        medicationName = "Amoxicillin",
        status = "given",
        note = "With food",
        scheduledTime = 1_700_000_000_000L,
        loggedTime = 1_700_000_100_000L,
        staffId = "NURSE-1234"
    )

    private fun fieldsOf(doc: JsonObject): JsonObject = doc.getAsJsonObject("fields")

    @Test
    fun medicationLog_roundTrip_keepsEveryField() {
        val back = FirestoreMapper.medicationLogFromFields(
            fieldsOf(FirestoreMapper.medicationLogToDocument(log))
        )
        // documents read from the server are already synced
        assertEquals(log.copy(isSyncPending = false), back)
    }

    @Test
    fun integers_areSentAsStringsAsFirestoreRequires() {
        val time = fieldsOf(FirestoreMapper.medicationLogToDocument(log))
            .getAsJsonObject("scheduledTime").get("integerValue")
        assertTrue(time.asJsonPrimitive.isString)
        assertEquals("1700000000000", time.asString)
    }

    @Test
    fun missingNote_isLeftOutAndReadsBackAsNull() {
        val noNote = log.copy(note = null)
        val fields = fieldsOf(FirestoreMapper.medicationLogToDocument(noNote))
        assertFalse(fields.has("note"))
        assertNull(FirestoreMapper.medicationLogFromFields(fields)?.note)
    }

    @Test
    fun documentMissingARequiredField_isSkipped() {
        val fields = fieldsOf(FirestoreMapper.medicationLogToDocument(log))
        fields.remove("patientId")
        assertNull(FirestoreMapper.medicationLogFromFields(fields))
    }

    @Test
    fun patient_roundTrip_keepsEveryField() {
        val patient = Patient(
            id = "patient-1", patientId = "P-101", name = "J. Nkosi", bedNumber = "Bed 4",
            ward = "ICU", age = 42, gender = "Male", medicalHistory = "Asthma",
            allergies = "Penicillin", bloodType = "A+", nextAppointment = "14:00",
            medSchedule = "Amoxicillin 8h"
        )
        val back = FirestoreMapper.patientFromFields(
            fieldsOf(FirestoreMapper.patientToDocument(patient))
        )
        assertEquals(patient, back)
    }

    @Test
    fun commitBody_wrapsEachWriteWithItsFullResourceName() {
        val name = FirestorePaths.document("demo-project", "medicationLogs", "log-1")
        val write = FirestoreMapper.updateWrite(name, FirestoreMapper.medicationLogToDocument(log))
        val body = FirestoreMapper.commitBody(listOf(write))

        val update = body.getAsJsonArray("writes")[0].asJsonObject.getAsJsonObject("update")
        assertEquals(
            "projects/demo-project/databases/(default)/documents/medicationLogs/log-1",
            update.get("name").asString
        )
        assertNotNull(update.getAsJsonObject("fields").get("status"))
    }

    @Test
    fun fieldsOfDocuments_handlesEmptyAndFilledResponses() {
        assertTrue(FirestoreMapper.fieldsOfDocuments(null).isEmpty())
        // Firestore returns {} for an empty collection
        assertTrue(FirestoreMapper.fieldsOfDocuments(JsonObject()).isEmpty())

        val response = JsonParser.parseString(
            """{"documents":[{"name":"x","fields":{"a":{"stringValue":"b"}}}]}"""
        ).asJsonObject
        assertEquals(1, FirestoreMapper.fieldsOfDocuments(response).size)
    }

    @Test
    fun paths_areBuiltForTheDefaultDatabase() {
        assertEquals("projects/p/databases/(default)", FirestorePaths.database("p"))
        assertEquals("projects/p/databases/(default)/documents/patients", FirestorePaths.collection("p", "patients"))
        assertEquals("projects/p/databases/(default)/documents/users/u1", FirestorePaths.document("p", "users", "u1"))
    }
}