package com.example.myapplication

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig

class GeminiLogic {
    // PASTE YOUR API KEY HERE
    private val apiKey = "AIzaSyBNv4_4pbh9pDL5kbD0y1fdHpTCADxFyYM"

    suspend fun generate_content(userMessage: String): String? {
        val prompt =
            "you are just a test bot\n $userMessage"

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0f
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.LOW_AND_ABOVE),
            )
        )

        try {
            val response = generativeModel.generateContent(prompt)
            return response.text
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }
}