package com.example.diary
// Kotlin code for MainActivity with 'messages' attribute for GPT API responses

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class GPTResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class DiaryEntryRequest(val model: String, val messages: List<Map<String, String>>)

class MainActivity : AppCompatActivity() {
    private val client = HttpClient()
    private var messages: MutableList<String> = mutableListOf()
    private val api_key = "api_key"
    private val gpt_direction = "이 일기에 대한 성찰적이고 긍정적인 답변을 해주세요. 일기의 형식이니 사용자가 대답하지 못한다는 점에 유념하세요."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        testGPTAPIIntegration()
    }

    private fun testGPTAPIIntegration() = CoroutineScope(Dispatchers.IO).launch {
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

        val responseBody:String = response.readText()

        // 알려지지 않은 키를 무시하도록 Json 설정을 수정합니다.
        val json = Json { ignoreUnknownKeys = true }

        try {

            // JSON 응답에서 GPTResponse 객체로 디코드합니다.
            val gptResponse = json.decodeFromString<GPTResponse>(responseBody)

            // 첫 번째 선택지의 메시지 내용을 가져옵니다.
            val content = gptResponse.choices.first().message.content

            // Log the response body
            Log.d("GPTApiResponse", content)
            fetchSpeech(content)

            // Placeholder logic assuming successful parsing
            messages.add("Successfully parsed response")
        } catch(e: Exception){
            Log.e("GPTAPIResponseError", "Error parsing GPT API response", e)
        }
    }

    private fun fetchSpeech(inputText: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response: HttpResponse = client.post("https://api.openai.com/v1/audio/speech") {
                header("Authorization", "Bearer $api_key")
                contentType(ContentType.Application.Json)
                body = """
                {
                    "model": "tts-1-hd",
                    "input": "$inputText",
                    "voice": "nova"
                }
            """.trimIndent()
            }

            // 응답으로 받은 오디오 데이터를 파일로 저장
            val bytes = response.readBytes()
            if (bytes.isNotEmpty()) {
                val outputFile = File(applicationContext.filesDir, "speech.mp3")
                outputFile.writeBytes(bytes)
                Log.d("GPTAudio", "Audio file saved successfully: ${outputFile.absolutePath}")

                withContext(Dispatchers.Main) {
                    playAudioFromFile("${applicationContext.filesDir}/speech.mp3")
                }
            } else {
                Log.e("GPTAudio", "No audio data received.")
            }
        } catch (e: Exception){
            Log.e("GPTAudio", "Error fetching or saving audio data: ${e.localizedMessage}")
        }

        // 파일 저장 완료 후 UI 스레드에서 UI 업데이트
        withContext(Dispatchers.Main) {
            playAudioFromFile("${applicationContext.filesDir}/speech.mp3")
            // UI 업데이트 로직 (예: 음성 파일 재생)
        }
    }

    private fun playAudioFromFile(filePath: String) {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            mediaPlayer.setOnCompletionListener {
                // 재생이 끝나면 리소스 해제
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
