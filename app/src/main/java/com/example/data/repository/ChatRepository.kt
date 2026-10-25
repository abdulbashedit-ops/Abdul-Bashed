package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiSystemInstruction
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.preferences.ChatLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao
) {
    companion object {
        private const val TAG = "ChatRepository"
        val CANDIDATE_MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon")
    }

    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(customTitle: String? = null): ChatSessionEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val title = customTitle ?: "New Chat"
        val session = ChatSessionEntity(
            id = id,
            title = title,
            previewMessage = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        session
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Resolves the active API key:
     * 1. Custom key if provided by user in preferences
     * 2. BuildConfig.GEMINI_API_KEY if injected by AI Studio
     */
    fun resolveApiKey(customKey: String): String {
        val trimmedCustom = customKey.trim()
        if (trimmedCustom.isNotBlank()) {
            return trimmedCustom
        }
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    /**
     * Sends message to Gemini AI, updating Room database with user and model turns.
     */
    suspend fun sendMessage(
        sessionId: String,
        userPrompt: String,
        apiKey: String,
        language: ChatLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanPrompt = userPrompt.trim()
        if (cleanPrompt.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Prompt cannot be empty"))
        }

        if (apiKey.isBlank()) {
            val keyErrorMsg = "Gemini API key is missing. Please configure GEMINI_API_KEY in the AI Studio Secrets panel."
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "error",
                    content = keyErrorMsg
                )
            )
            return@withContext Result.failure(IllegalStateException(keyErrorMsg))
        }

        // 1. Save user turn to Room
        val userMessageId = chatDao.insertMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = cleanPrompt
            )
        )

        // Update session preview & title
        val existingSession = chatDao.getSessionById(sessionId)
        val sessionTitle = if (existingSession == null || existingSession.title == "New Chat" || existingSession.title.isBlank()) {
            generateTitle(cleanPrompt)
        } else {
            existingSession.title
        }

        chatDao.insertSession(
            ChatSessionEntity(
                id = sessionId,
                title = sessionTitle,
                previewMessage = cleanPrompt.take(80),
                updatedAt = System.currentTimeMillis()
            )
        )

        // 2. Fetch full conversation history for context
        val allMessages = chatDao.getMessagesForSessionSync(sessionId)
            .filter { it.role == "user" || it.role == "model" }

        // Construct contents payload
        val contents = allMessages.map { msg ->
            val role = if (msg.role == "model") "model" else "user"
            GeminiContent(
                role = role,
                parts = listOf(GeminiPart(text = msg.content))
            )
        }

        val systemPrompt = buildString {
            append("You are MyAI, a helpful, polite, and intelligent AI chatbot created to assist users. ")
            append("You have native, fluent mastery of Bengali (বাংলা) and English. ")
            when (language) {
                ChatLanguage.BANGLA -> append("The user prefers Bengali. Respond in clear, polite, natural Bengali (বাংলা) unless requested otherwise. ")
                ChatLanguage.ENGLISH -> append("The user prefers English. Respond in fluent, clear English unless requested otherwise. ")
                ChatLanguage.AUTO -> append("Detect the user's language automatically: if the query is in Bengali, reply in Bengali; if in English, reply in English. ")
            }
            append("Format your responses nicely using Markdown (bullet points, bold text, numbered lists, and code blocks with language identifiers where appropriate). ")
            append("Maintain context of the ongoing conversation accurately.")
        }

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7,
                topK = 40,
                topP = 0.95,
                maxOutputTokens = 2048
            )
        )

        // Call Gemini API (primary model, then fallback if needed)
        try {
            var response: retrofit2.Response<com.example.data.api.GeminiResponse>? = null
            for (model in CANDIDATE_MODELS) {
                val candidateResponse = GeminiApiClient.service.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )
                if (candidateResponse.isSuccessful) {
                    response = candidateResponse
                    break
                } else if (candidateResponse.code() == 404) {
                    Log.w(TAG, "Model $model returned 404, checking next supported model...")
                    response = candidateResponse
                    continue
                } else {
                    response = candidateResponse
                    break
                }
            }

            if (response != null && response.isSuccessful) {
                val body = response.body()
                val candidateText = body?.candidates?.firstOrNull()?.content?.parts?.joinToString("\n") { it.text }

                if (!candidateText.isNullOrBlank()) {
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "model",
                            content = candidateText
                        )
                    )
                    return@withContext Result.success(candidateText)
                } else {
                    val emptyError = "Received an empty response from Gemini model."
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "error",
                            content = emptyError
                        )
                    )
                    return@withContext Result.failure(IllegalStateException(emptyError))
                }
            } else {
                val code = response?.code() ?: 500
                val errorBody = response?.errorBody()?.string() ?: ""
                Log.e(TAG, "API error: $code - $errorBody")
                val parsedMsg = parseErrorMessage(code, errorBody)
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        role = "error",
                        content = parsedMsg
                    )
                )
                return@withContext Result.failure(Exception(parsedMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or unexpected exception", e)
            val friendlyError = when (e) {
                is java.net.SocketTimeoutException -> {
                    "Request timed out (টাইমআউট): সার্ভার থেকে উত্তর আসতে বেশি সময় লেগেছে। দয়া করে ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।"
                }
                is java.net.UnknownHostException -> {
                    "ইন্টারনেট সংযোগ পাওয়া যায়নি (No Internet): দয়া করে ওয়াইফাই বা মোবাইল ডেটা চেক করুন।"
                }
                else -> {
                    "ত্রুটি (Error): ${e.localizedMessage ?: "Failed to generate AI response."}"
                }
            }
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "error",
                    content = friendlyError
                )
            )
            return@withContext Result.failure(e)
        }
    }

    private fun generateTitle(prompt: String): String {
        val clean = prompt.replace("\n", " ").trim()
        return if (clean.length > 36) {
            clean.take(36) + "..."
        } else {
            clean
        }
    }

    private fun parseErrorMessage(code: Int, errorBody: String): String {
        return when (code) {
            400 -> "API request invalid (400). Please check your query or API key format."
            401, 403 -> "Invalid or unauthorized API key (code $code). Please verify your GEMINI_API_KEY in the AI Studio Secrets panel."
            404 -> "Requested Gemini model not found (404)."
            429 -> "Rate limit reached or quota exceeded (429). Please wait a moment before sending another message."
            500, 503 -> "Gemini service temporarily unavailable ($code). Please try again in a few seconds."
            else -> "Gemini API Error ($code): ${errorBody.take(120)}"
        }
    }
}
