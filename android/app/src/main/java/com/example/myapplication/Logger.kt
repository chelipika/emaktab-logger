package com.example.myapplication

import android.content.Context
import android.util.Half.toFloat
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.system.measureTimeMillis

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.media.MediaPlayer
class Logger(private val context: Context) {
    data class ProgressState(
        val message: String = "",
        val percentage: Float = 0f
    )
    // --- JSON Data Models ---
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


    // --- Main Entry Point ---
    fun runLoginScript(): Flow<ProgressState> = flow  {
        val tag = "LoginChecker"
        emit(ProgressState("Loginlarni yuklayapman...", 0.2f))
        // Add your logins here
        val logins = listOf(
            "abdumalikovibrohim02", "gabdurahimova0211201", "mushtaryi",
            "dilshoda.alijonova06", "alimjonov_husan", "fotima.alimjonova052",
            "madina.ataboyeva1020", "muhbubaxonaxmedova", "akbarshoh_azimov",
            "firdavsismoilov03201", "jabbarovamubinabonu", "kumishjalolova",
            "jamoliddinovazarina", "r.karimjonova0611201", "sayyodbekkarimov",
            "oisha.kasimova", "marupjonovamubina", "meliqulova_diyora",
            "miramatjonov", "muxriddinovabubakr", "ortikovamubinaxon",
            "aqidaraximjanova", "s.mavludahon", "tojidinovamalikaxon",
            "nigina.tursunboyeva1", "ziyoda.umarova300420", "u_ziyoviddin",
            "mubosherv", "jahongirxamidilloyev", "shaxnoza.rahimova102",
            "azizbek.yunusov20072", "mustafo.yunusov10201",
            "ruhshona.yunusova082", "zahidjanov"
        )

        val url = "https://api.emaktab.uz/mobile/v10.0/authorizations/bycredentials"
        val gson = Gson()
        val client = OkHttpClient()
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val sucLogin = mutableListOf<String>()
        val failLogin = mutableListOf<String>()
        val feedbackLogs = mutableListOf<String>()

        val progressAddiction = (70f / logins.size) / 100f
        emit(ProgressState("${logins.size}: uchun kirishni boshlayapman...", 0.3f))
        val executionTime = measureTimeMillis {
            var savedProgressForLogin = 0.3f
            for (username in logins) {
                // We capture the String returned by loginIn
                val statusMessage = loginIn(client, gson, url, jsonMediaType, username, sucLogin, failLogin)

                feedbackLogs.add("$username: $statusMessage")
                savedProgressForLogin += progressAddiction
                emit(ProgressState("FEEDBACK for $username: $statusMessage", savedProgressForLogin))
            }
        }

        emit(ProgressState("DETAILED FEEDBACK: $feedbackLogs", 1f))
        val mediaPlayer = MediaPlayer.create(context, R.raw.progess_finished)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
    }

    /**
     * Helper: Performs Login.
     * Returns a String message describing the result.
     */
    private fun loginIn(
        client: OkHttpClient,
        gson: Gson,
        url: String,
        mediaType: okhttp3.MediaType,
        username: String,
        sucLogin: MutableList<String>,
        failLogin: MutableList<String>
    ): String {
        val payload = LoginRequest(
            clientId = "B70AECAA-A0E2-4E0D-A147-2D6051A1397B",
            clientSecret = "C0A8880F-30CA-4877-ADD2-26ED9672EC93",
            scope = "Schools,Relatives,EduGroups,Lessons,marks,EduWorks,Avatar",
            username = username,
            password = "111111",
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
            // Explicitly executing inside try block
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()

                if (!response.isSuccessful || bodyString == null) {
                    failLogin.add(username)
                    // Explicit return from function
                    return "HTTP Error: ${response.code}"
                }

                val loginResponse = gson.fromJson(bodyString, LoginResponse::class.java)

                when (loginResponse.type) {
                    "Success" -> {
                        val token = loginResponse.credentials?.accessToken
                        val userId = loginResponse.credentials?.userId

                        if (token != null && userId != null) {
                            // Recursively call checkDairy
                            val dairyFeedback = checkDairy(client, gson, token, userId.toString())

                            sucLogin.add(username)
                            return "Успешный логин | $dairyFeedback"
                        } else {
                            failLogin.add(username)
                            return "Login Success but Missing Credentials"
                        }
                    }
                    "Error" -> {
                        failLogin.add(username)
                        return "LOGIN YOKI PAROL NOTOGRI"
                    }
                    else -> {
                        failLogin.add(username)
                        return "Unknown Error Type: ${loginResponse.type}"
                    }
                }
            }
        } catch (e: Exception) {
            failLogin.add(username)
            return "Network Exception: ${e.message}"
        }

    }

    /**
     * Helper: Gets Group IDs.
     * Returns a Pair or Null.
     */
    private fun getGroupsId(
        client: OkHttpClient,
        gson: Gson,
        accessToken: String,
        userId: String
    ): Pair<Long, String>? {
        val url = "https://api.emaktab.uz/mobile/v10.0/users/$userId/context"

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept-Encoding", "gzip")
            .addHeader("Access-Token", accessToken)
            .addHeader("app-version", "10.0.0(121)")
            .addHeader("User-Agent", "okhttp/4.10.0")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && bodyString != null) {
                    val contextData = gson.fromJson(bodyString, ContextResponse::class.java)
                    val groupId = contextData.contextPersons?.firstOrNull()?.group?.id
                    val personId = contextData.info?.personId

                    if (groupId != null && personId != null) {
                        Pair(groupId, personId)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Helper: Checks Diary.
     * Returns a String message.
     */
    private fun checkDairy(
        client: OkHttpClient,
        gson: Gson,
        accessToken: String,
        userId: String
    ): String {
        // 1. Get Group ID
        val ids = getGroupsId(client, gson, accessToken, userId)

        if (ids == null) {
            return "Dairy Check Failed: Could not retrieve Group/Person IDs"
        }

        val (userGroupId, personId) = ids
        val dairyUrl = "https://api.emaktab.uz/mobile/v10.0/persons/$personId/groups/$userGroupId/diary?id="

        val request = Request.Builder()
            .url(dairyUrl)
            .addHeader("Accept-Encoding", "gzip")
            .addHeader("Access-Token", accessToken)
            .addHeader("app-version", "10.0.0(121)")
            .addHeader("User-Agent", "okhttp/4.10.0")
            .get()
            .build()

        // 2. Execute Request
        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()

                if (bodyString == null) {
                    return "Проверка дневника не удачен: Empty Body"
                }

                val dairyResponse = gson.fromJson(bodyString, DiaryResponse::class.java)

                if (dairyResponse.type == "systemForbidden") {
                    return "Дневник: Система запрешаят"
                } else {
                    return "Дневник: Успешно"
                }
            }
        } catch (e: Exception) {
            return "Dairy Check Exception: ${e.message}"
        }
    }
}