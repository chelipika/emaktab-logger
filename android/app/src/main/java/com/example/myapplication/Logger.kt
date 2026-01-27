package com.example.myapplication

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// 1. Data Class for UI Updates (Resolves 'message' and 'percentage' errors)
data class LogUpdate(
    val message: String,
    val percentage: Float
)

class Logger(private val context: Context) {

    // Data Models
    data class LoginRequest(
        val clientId: String, val clientSecret: String, val scope: String,
        val username: String, val password: String, val agreeTerms: String
    )
    data class LoginResponse(val type: String?, val credentials: Credentials?)
    data class Credentials(val accessToken: String?, val userId: Long?)
    data class ContextResponse(val contextPersons: List<ContextPerson>?, val info: Info?)
    data class ContextPerson(val group: Group?)
    data class Group(val id: Long?)
    data class Info(val personId: String?)
    data class DiaryResponse(val type: String?)


    // 2. Change return type to Flow<LogUpdate>
    fun runLoginScript(): Flow<LogUpdate> = flow {
        val tag = "LoginChecker"
        val credManager = CredentialsManager(context)
        val usersList = credManager.loadUsers()

        if (usersList.isEmpty()) {
            emit(LogUpdate("Error: No users found in Excel file!", 0f))
            return@flow
        }

        emit(LogUpdate("Starting check for ${usersList.size} users...", 0f))

        val url = "https://api.emaktab.uz/mobile/v10.0/authorizations/bycredentials"
        val gson = Gson()
        val client = OkHttpClient()
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        var successCount = 0
        var failCount = 0

        // 3. Loop and Emit Updates
        for ((index, user) in usersList.withIndex()) {
            val currentProgress = ((index + 1).toFloat() / usersList.size.toFloat())

            // Notify UI: "Checking user..."
            emit(LogUpdate("Checking: ${user.login}...", currentProgress))

            // Run Logic
            val resultMessage = loginIn(client, gson, url, jsonMediaType, user.login, user.password)

            // Check result to update counts
            if (resultMessage.contains("Success")) successCount++ else failCount++

            // Notify UI: Result
            Log.d(tag, "${user.login}: $resultMessage")
            emit(LogUpdate("${user.login}: $resultMessage", currentProgress))

            // Small delay to ensure UI can update smoothly
            delay(50)
        }

        emit(LogUpdate("DONE! Success: $successCount, Failed: $failCount", 1.0f))
    }

    private fun loginIn(
        client: OkHttpClient, gson: Gson, url: String, mediaType: okhttp3.MediaType,
        username: String, password: String
    ): String {
        val payload = LoginRequest(
            clientId = "B70AECAA-A0E2-4E0D-A147-2D6051A1397B",
            clientSecret = "C0A8880F-30CA-4877-ADD2-26ED9672EC93",
            scope = "Schools,Relatives,EduGroups,Lessons,marks,EduWorks,Avatar",
            username = username,
            password = password,
            agreeTerms = "false"
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("app-version", "10.0.0(121)")
            .addHeader("User-Agent", "okhttp/4.10.0")
            .post(gson.toJson(payload).toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: return "HTTP Error: Empty Body"
                if (!response.isSuccessful) return "HTTP Error: ${response.code}"

                val loginResponse = gson.fromJson(bodyString, LoginResponse::class.java)

                return when (loginResponse.type) {
                    "Success" -> {
                        val token = loginResponse.credentials?.accessToken
                        val userId = loginResponse.credentials?.userId
                        if (token != null && userId != null) {
                            val dairy = checkDairy(client, gson, token, userId.toString())
                            "Login OK | $dairy"
                        } else {
                            "Login OK | Missing Creds"
                        }
                    }
                    "Error" -> "WRONG LOGIN/PASS"
                    else -> "Unknown: ${loginResponse.type}"
                }
            }
        } catch (e: Exception) {
            return "Net Ex: ${e.message}"
        }
    }

    private fun checkDairy(client: OkHttpClient, gson: Gson, accessToken: String, userId: String): String {
        val ids = getGroupsId(client, gson, accessToken, userId) ?: return "Dairy: No IDs"
        val (gid, pid) = ids
        val url = "https://api.emaktab.uz/mobile/v10.0/persons/$pid/groups/$gid/diary?id="
        val request = Request.Builder().url(url).addHeader("Accept-Encoding", "gzip").addHeader("Access-Token", accessToken).addHeader("app-version", "10.0.0(121)").addHeader("User-Agent", "okhttp/4.10.0").get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return "Dairy: Empty"
                val data = gson.fromJson(body, DiaryResponse::class.java)
                return if (data.type == "systemForbidden") "Dairy: FORBIDDEN" else "Dairy: SUCCESS"
            }
        } catch (e: Exception) { return "Dairy Ex" }
    }

    private fun getGroupsId(client: OkHttpClient, gson: Gson, accessToken: String, userId: String): Pair<Long, String>? {
        val url = "https://api.emaktab.uz/mobile/v10.0/users/$userId/context"
        val request = Request.Builder().url(url).addHeader("Accept-Encoding", "gzip").addHeader("Access-Token", accessToken).addHeader("app-version", "10.0.0(121)").addHeader("User-Agent", "okhttp/4.10.0").get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                if (!response.isSuccessful) return null
                val data = gson.fromJson(body, ContextResponse::class.java)
                val gid = data.contextPersons?.firstOrNull()?.group?.id
                val pid = data.info?.personId
                return if (gid != null && pid != null) Pair(gid, pid) else null
            }
        } catch (e: Exception) { return null }
    }
}