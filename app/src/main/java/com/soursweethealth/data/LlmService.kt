package com.soursweethealth.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int = 512,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)
data class ChatResponse(
    val choices: List<Choice>?
) {
    data class Choice(val message: ChatMessage?)
}

// Streaming response models
data class StreamChoice(val delta: DeltaMessage?)
data class DeltaMessage(val content: String?)
data class StreamResponse(val choices: List<StreamChoice>?)

class LlmService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /** Streaming API call — emits text chunks as they arrive */
    fun chatStream(
        apiUrl: String,
        apiKey: String,
        modelName: String,
        prompt: String,
        maxTokens: Int = 512
    ): Flow<Result<String>> = flow {
        try {
            // Normalize URL: if it doesn't end with /chat/completions, append it
            // This makes the app compatible with:
            //   - Full endpoint URLs like https://api.siliconflow.cn/v1/chat/completions
            //   - Base URLs like https://api.deepseek.com or https://api.openai.com/v1
            val normalizedUrl = normalizeApiUrl(apiUrl)

            val body = gson.toJson(
                ChatRequest(
                    model = modelName,
                    messages = listOf(
                        ChatMessage("system", "你是一位和蜃、经验丰富、十分体贴的家庭医生，同时也是敢说真心话的好朋友。用“您”称呼，语气亲切自然像与老朋友聊天。可适当加1-2个表情符号，用**加粗**强调关键信息，中文回复。"),
                        ChatMessage("user", prompt)
                    ),
                    max_tokens = maxTokens,
                    stream = true
                )
            )
            val request = Request.Builder()
                .url(normalizedUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                emit(Result.failure<String>(Exception("API请求失败: ${response.code} $errBody")))
                return@flow
            }

            val reader = response.body?.charStream()?.buffered()
                ?: run { emit(Result.failure<String>(Exception("响应体为空"))); return@flow }

            reader.use {
                for (line in it.lineSequence()) {
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val chunk = gson.fromJson(data, StreamResponse::class.java)
                        val token = chunk.choices?.firstOrNull()?.delta?.content
                        if (!token.isNullOrEmpty()) {
                            emit(Result.success(token))
                        }
                    } catch (_: Exception) { /* skip malformed line */ }
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /** Normalize API URL to ensure it ends with /chat/completions */
    private fun normalizeApiUrl(url: String): String {
        val trimmed = url.trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            // For DeepSeek-style URLs (e.g. https://api.deepseek.com), append /chat/completions directly
            trimmed.matches(Regex("https?://[^/]+")) -> "$trimmed/chat/completions"
            else -> "$trimmed/v1/chat/completions"
        }
    }
}
