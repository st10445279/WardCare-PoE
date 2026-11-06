package com.wardcare.app.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST API used by WardCare: the Cloud Firestore REST API (a hosted API in front of the database).
 * Reference: https://firebase.google.com/docs/firestore/use-rest-api
 *
 * Every request carries the signed-in user's Firebase ID token (see ApiClient), so the
 * Firestore security rules decide who may read or write.
 */
interface ApiService {

    /** Creates the document if it does not exist, otherwise replaces it (safe to retry). */
    @PATCH("v1/{path}")
    suspend fun upsertDocument(
        @Path("path", encoded = true) path: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @GET("v1/{path}")
    suspend fun getDocument(
        @Path("path", encoded = true) path: String
    ): Response<JsonObject>

    @GET("v1/{path}")
    suspend fun listDocuments(
        @Path("path", encoded = true) path: String,
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String?
    ): Response<JsonObject>

    /** Atomic batch write: either every write in the body succeeds or none do. */
    @POST("v1/{database}/documents:commit")
    suspend fun commit(
        @Path("database", encoded = true) database: String,
        @Body body: JsonObject
    ): Response<JsonObject>
}