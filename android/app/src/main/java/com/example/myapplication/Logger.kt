package com.example.myapplication

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// UI Update Model
data class LogUpdate(val message: String, val percentage: Float)

class Logger(private val context: Context) {

    // --- ROBUST DATA MODELS ---
    // Using Strings for IDs is safer to avoid Long/Int precision issues

    data class LoginRequest(
        val clientId: String, val clientSecret: String, val scope: String,
        val username: String, val password: String, val agreeTerms: String
    )

    data class LoginResponse(
        @SerializedName("type") val type: String?,
        @SerializedName("credentials") val credentials: Credentials?
    )

    data class Credentials(
        @SerializedName("accessToken") val accessToken: String?,
        @SerializedName("userId") val userId: String? // String handles both "123" and 123
    )

    data class ContextResponse(
        @SerializedName("contextPersons") val contextPersons: List<ContextPerson>?,
        @SerializedName("info") val info: Info?
    )

    data class ContextPerson(
        @SerializedName("group") val group: Group?
    )

    data class Group(
        @SerializedName("id") val id: String?
    )

    data class Info(
        @SerializedName("personId") val personId: String?
    )

    data class DiaryResponse(
        @SerializedName("type") val type: String?
    )


    // --- MAIN LOGIC ---
    fun runLoginScript(): Flow<LogUpdate> = flow {
        val credManager = CredentialsManager(context)
        val usersList = credManager.loadUsers()

        if (usersList.isEmpty()) {
            emit(LogUpdate("❌ Error: No users found in Excel!", 0f))
            return@flow
        }

        emit(LogUpdate("Starting check for ${usersList.size} users...", 0f))

        val url = "https://api.emaktab.uz/mobile/v10.0/authorizations/bycredentials"
        val gson = Gson()

        // OkHttp handles Gzip automatically. Do NOT add Accept-Encoding header manually.
        val client = OkHttpClient()
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        var successCount = 0
        var failCount = 0

        for ((index, user) in usersList.withIndex()) {
            val currentProgress = ((index + 1).toFloat() / usersList.size.toFloat())

            emit(LogUpdate("Checking: ${user.login}...", currentProgress))

            // Run Logic
            val resultMessage = loginIn(client, gson, url, jsonMediaType, user.login, user.password)

            if (resultMessage.contains("✅")) {
                successCount++
            } else {
                failCount++
            }

            Log.d("LoginChecker", "${user.login}: $resultMessage")
            emit(LogUpdate("${user.login}: $resultMessage", currentProgress))

            delay(50)
        }

        emit(LogUpdate("DONE! ✅ Success: $successCount, ❌ Failed: $failCount", 1.0f))
        val mediaPlayer = MediaPlayer.create(context, R.raw.progess_finished)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
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
                val bodyString = response.body?.string() ?: return "HTTP Empty Body"
                if (!response.isSuccessful) return "HTTP ${response.code}"

                val loginResponse = gson.fromJson(bodyString, LoginResponse::class.java)

                return when (loginResponse.type) {
                    "Success" -> {
                        val token = loginResponse.credentials?.accessToken
                        val userId = loginResponse.credentials?.userId
                        if (token != null && userId != null) {
                            checkDairy(client, gson, token, userId)
                        } else {
                            "Login OK | Missing Creds"
                        }
                    }
                    "Error" -> "❌ WRONG LOGIN/PASS"
                    else -> "Unknown: ${loginResponse.type}"
                }
            }
        } catch (e: Exception) {
            return "Net Ex: ${e.message}"
        }
    }

    private fun getGroupsId(
        client: OkHttpClient,
        gson: Gson,
        accessToken: String,
        userId: String
    ): Triple<String?, String?, String?> {
        val url = "https://api.emaktab.uz/mobile/v10.0/users/$userId/context"

        val request = Request.Builder()
            .url(url)
            // REMOVED "Accept-Encoding: gzip" - This was causing the String vs Object error!
            .addHeader("Access-Token", accessToken)
            .addHeader("app-version", "10.0.0(121)")
            .addHeader("User-Agent", "okhttp/4.10.0")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) return Triple(null, null, "Context HTTP ${response.code}")

                try {
                    val data = gson.fromJson(body, ContextResponse::class.java)

                    val pId = data.info?.personId
                    val gId = data.contextPersons?.firstOrNull()?.group?.id

                    if (pId == null || gId == null) {
                        return Triple(null, null, "IDs Null. Body: $body") // Show body to debug
                    }

                    return Triple(gId, pId, null)
                } catch (e: JsonSyntaxException) {
                    // This captures the "Expected BEGIN_OBJECT" error and shows you what the server actually sent
                    return Triple(null, null, "JSON Error: ${e.message}. RAW BODY: $body")
                }
            }
        } catch (e: Exception) {
            return Triple(null, null, "Context Ex: ${e.message}")
        }
    }

    private fun checkDairy(client: OkHttpClient, gson: Gson, accessToken: String, userId: String): String {
        val (gid, pid, errorMsg) = getGroupsId(client, gson, accessToken, userId)

        if (errorMsg != null) {
            return "Dairy Fail: $errorMsg"
        }

        val url = "https://api.emaktab.uz/mobile/v10.0/persons/$pid/groups/$gid/diary?id="

        val request = Request.Builder()
            .url(url)
            // REMOVED "Accept-Encoding: gzip"
            .addHeader("Access-Token", accessToken)
            .addHeader("app-version", "10.0.0(121)")
            .addHeader("User-Agent", "okhttp/4.10.0")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return "Dairy Empty"

                try {
                    val data = gson.fromJson(body, DiaryResponse::class.java)
                    return if (data.type == "systemForbidden") "Dairy: ❌ FORBIDDEN" else "Dairy: ✅ SUCCESS"
                } catch (e: JsonSyntaxException) {
                    return "Dairy JSON Error. RAW: $body"
                }
            }
        } catch (e: Exception) {
            return "Dairy Ex: ${e.message}"
        }
    }
}