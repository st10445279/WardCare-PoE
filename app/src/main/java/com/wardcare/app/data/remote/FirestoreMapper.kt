package com.wardcare.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.wardcare.app.data.model.MedicationLog
import com.wardcare.app.data.model.Patient
import com.wardcare.app.data.model.UserAccount

/** Builds Firestore REST resource paths. */
object FirestorePaths {
    fun database(projectId: String) = "projects/$projectId/databases/(default)"
    fun collection(projectId: String, name: String) = "${database(projectId)}/documents/$name"
    fun document(projectId: String, name: String, id: String) = "${collection(projectId, name)}/$id"
}

/**
 * Converts app models to and from the Firestore REST "typed value" JSON format, e.g.
 * {"fields": {"status": {"stringValue": "given"}, "loggedTime": {"integerValue": "1700000000000"}}}
 * Reference: https://firebase.google.com/docs/firestore/reference/rest/v1/Value
 * Pure Kotlin (no Android classes) so it can be unit tested on the JVM.
 */
object FirestoreMapper {

    private fun str(v: String) = JsonObject().apply { addProperty("stringValue", v) }

    // Firestore REST sends 64-bit integers as JSON strings
    private fun int(v: Long) = JsonObject().apply { addProperty("integerValue", v.toString()) }

    private fun readString(fields: JsonObject, key: String): String? =
        fields.getAsJsonObject(key)?.get("stringValue")?.asString

    private fun readLong(fields: JsonObject, key: String): Long? =
        fields.getAsJsonObject(key)?.get("integerValue")?.asString?.toLongOrNull()

    private fun document(fields: JsonObject) = JsonObject().apply { add("fields", fields) }

    // ---------- medication logs ----------

    fun medicationLogToDocument(log: MedicationLog): JsonObject {
        val fields = JsonObject().apply {
            add("uuid", str(log.uuid))
            add("patientId", str(log.patientId))
            add("patientName", str(log.patientName))
            add("medicationName", str(log.medicationName))
            add("status", str(log.status))
            // a missing note is simply left out of the document
            log.note?.let { add("note", str(it)) }
            add("scheduledTime", int(log.scheduledTime))
            add("loggedTime", int(log.loggedTime))
            add("staffId", str(log.staffId))
        }
        return document(fields)
    }

    /** Returns null if the document is missing a required field (it is then skipped). */
    fun medicationLogFromFields(f: JsonObject): MedicationLog? {
        return MedicationLog(
            uuid = readString(f, "uuid") ?: return null,
            patientId = readString(f, "patientId") ?: return null,
            patientName = readString(f, "patientName") ?: return null,
            medicationName = readString(f, "medicationName") ?: return null,
            status = readString(f, "status") ?: return null,
            note = readString(f, "note"),
            scheduledTime = readLong(f, "scheduledTime") ?: return null,
            loggedTime = readLong(f, "loggedTime") ?: return null,
            staffId = readString(f, "staffId") ?: "",
            isSyncPending = false // it came from the server, so it is already synced
        )
    }

    // ---------- patients ----------

    fun patientToDocument(p: Patient): JsonObject {
        val fields = JsonObject().apply {
            add("id", str(p.id))
            add("patientId", str(p.patientId))
            add("name", str(p.name))
            add("bedNumber", str(p.bedNumber))
            add("ward", str(p.ward))
            add("age", int(p.age.toLong()))
            add("gender", str(p.gender))
            add("medicalHistory", str(p.medicalHistory))
            add("allergies", str(p.allergies))
            add("bloodType", str(p.bloodType))
            add("nextAppointment", str(p.nextAppointment))
            add("medSchedule", str(p.medSchedule))
        }
        return document(fields)
    }

    fun patientFromFields(f: JsonObject): Patient? {
        return Patient(
            id = readString(f, "id") ?: return null,
            patientId = readString(f, "patientId") ?: return null,
            name = readString(f, "name") ?: return null,
            bedNumber = readString(f, "bedNumber") ?: return null,
            ward = readString(f, "ward") ?: "General Ward",
            age = readLong(f, "age")?.toInt() ?: 0,
            gender = readString(f, "gender") ?: "",
            medicalHistory = readString(f, "medicalHistory") ?: "",
            allergies = readString(f, "allergies") ?: "",
            bloodType = readString(f, "bloodType") ?: "",
            nextAppointment = readString(f, "nextAppointment") ?: "",
            medSchedule = readString(f, "medSchedule") ?: ""
        )
    }

    // ---------- staff profile (the password is never part of it) ----------

    fun userToDocument(u: UserAccount): JsonObject {
        val fields = JsonObject().apply {
            add("email", str(u.email))
            add("name", str(u.name))
            add("ward", str(u.ward))
            add("role", str(u.role))
            add("staffId", str(u.staffId))
            add("phoneNumber", str(u.phoneNumber))
        }
        return document(fields)
    }

    fun userFromFields(f: JsonObject): UserAccount? {
        return UserAccount(
            email = readString(f, "email") ?: return null,
            name = readString(f, "name") ?: "",
            ward = readString(f, "ward") ?: "General Ward",
            passwordHash = "",
            role = readString(f, "role") ?: "Registered Nurse",
            staffId = readString(f, "staffId") ?: "",
            phoneNumber = readString(f, "phoneNumber") ?: ""
        )
    }

    // ---------- push token ----------

    fun deviceTokenDocument(token: String, updatedAt: Long): JsonObject {
        val fields = JsonObject().apply {
            add("token", str(token))
            add("updatedAt", int(updatedAt))
        }
        return document(fields)
    }

    // ---------- batch writes ----------

    /** One "update" write for the :commit endpoint; resourceName is the full document path. */
    fun updateWrite(resourceName: String, document: JsonObject): JsonObject {
        val update = JsonObject()
        update.addProperty("name", resourceName)
        update.add("fields", document.getAsJsonObject("fields"))
        return JsonObject().apply { add("update", update) }
    }

    fun commitBody(writes: List<JsonObject>): JsonObject {
        val array = JsonArray()
        writes.forEach { array.add(it) }
        return JsonObject().apply { add("writes", array) }
    }

    /** Extracts the "fields" object of every document in a list response (empty collection returns {}). */
    fun fieldsOfDocuments(response: JsonObject?): List<JsonObject> {
        val documents = response?.getAsJsonArray("documents") ?: return emptyList()
        return documents.mapNotNull { it.asJsonObject.getAsJsonObject("fields") }
    }
}