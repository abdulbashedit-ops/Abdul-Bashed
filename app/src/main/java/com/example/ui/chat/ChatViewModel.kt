package com.example.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.preferences.ChatLanguage
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ChatRepository(database.chatDao())
    private val preferencesRepository = UserPreferencesRepository(application)

    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val chatLanguage: StateFlow<ChatLanguage> = preferencesRepository.chatLanguageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatLanguage.AUTO
        )

    private val customApiKeyFlow = preferencesRepository.customApiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList())
            else repository.getMessages(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeGenerationJob: Job? = null

    init {
        // Auto-select latest session if one exists, otherwise start fresh
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun createNewChat() {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
        _errorMessage.value = null
        _inputText.value = ""
        viewModelScope.launch {
            val newSession = repository.createNewSession()
            _currentSessionId.value = newSession.id
        }
    }

    fun selectSession(sessionId: String) {
        if (_currentSessionId.value != sessionId) {
            activeGenerationJob?.cancel()
            _isGenerating.value = false
            _errorMessage.value = null
            _inputText.value = ""
            _currentSessionId.value = sessionId
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = allSessions.value.filter { it.id != sessionId }
                _currentSessionId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _currentSessionId.value = null
        }
    }

    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = (customPrompt ?: _inputText.value).trim()
        if (promptToSend.isBlank() || _isGenerating.value) return

        if (customPrompt == null) {
            _inputText.value = ""
        }
        _errorMessage.value = null

        activeGenerationJob = viewModelScope.launch {
            // Ensure session exists
            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                val session = repository.createNewSession()
                sessionId = session.id
                _currentSessionId.value = sessionId
            }

            _isGenerating.value = true
            try {
                val resolvedKey = repository.resolveApiKey(customApiKeyFlow.value)
                val result = repository.sendMessage(
                    sessionId = sessionId,
                    userPrompt = promptToSend,
                    apiKey = resolvedKey,
                    language = chatLanguage.value
                )

                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message
                }
            } catch (e: CancellationException) {
                // Generation stopped by user
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to generate response"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setChatLanguage(lang: ChatLanguage) {
        viewModelScope.launch {
            preferencesRepository.setChatLanguage(lang)
        }
    }

    fun hasValidApiKey(): Boolean {
        val resolved = repository.resolveApiKey(customApiKeyFlow.value)
        return resolved.isNotBlank()
    }
}
