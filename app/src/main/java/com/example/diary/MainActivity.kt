package com.example.diary
// Kotlin code for MainActivity with 'messages' attribute for GPT API responses

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DiaryEntryRequest(val model: String, val messages: List<Map<String, String>>)

class MainActivity : AppCompatActivity() {
    private val client = HttpClient()
    private var messages: MutableList<String> = mutableListOf()
    private val api_key = "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"
    private val gpt_direction = "이 일기에 대한 성찰적이고 긍정적인 답변을 해주세요. 일기의 형식이니 사용자가 대답하지 못한다는 점에 유념하세요."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        testGPTAPIIntegration()
    }

    private fun testGPTAPIIntegration() = CoroutineScope(Dispatchers.IO).launch {
        submitDiaryEntry("오늘은 정말 힘든 하루였어.")
        submitDiaryEntry("오늘은 정말 멋진 하루였어!")
    }

    private suspend fun submitDiaryEntry(entry: String) {
        val promptMessages = listOf(
            mapOf("role" to "system", "content" to gpt_direction),
            mapOf("role" to "user", "content" to entry)
        )
        val response: HttpResponse = client.post("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $api_key")
            contentType(ContentType.Application.Json)
            body = Json.encodeToString(DiaryEntryRequest("gpt-3.5-turbo", promptMessages))
        }
        handleGPTAPIResponse(response)
    }

    private suspend fun handleGPTAPIResponse(response: HttpResponse) {
        // TODO: Implement logic for parsing and adding the response to 'messages'

        val responseBody = response.readText()

        // Log the response body
        Log.d("GPTApiResponse", responseBody)

        // Placeholder logic assuming successful parsing
        messages.add("Successfully parsed response")
    }
}