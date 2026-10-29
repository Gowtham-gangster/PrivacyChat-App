package com.privychat.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privychat.app.data.repository.ConversationRepository
import com.privychat.app.domain.model.AttachmentType
import com.privychat.app.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val conversationId: UUID = UUID.fromString(checkNotNull(savedStateHandle.get<String>("conversationId")))
    val participantName: String = checkNotNull(savedStateHandle.get<String>("participantName"))
    val participantUserId: UUID = UUID.fromString(checkNotNull(savedStateHandle.get<String>("participantUserId")))

    val messages: StateFlow<List<Message>> = conversationRepository.getMessagesFlow(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            conversationRepository.sendMessage(conversationId, participantUserId, text.trim())
        }
    }

    fun sendAttachment(bytes: ByteArray, fileName: String, mimeType: String, type: AttachmentType) {
        viewModelScope.launch {
            conversationRepository.sendAttachment(conversationId, participantUserId, bytes, fileName, mimeType, type)
        }
    }

    fun startVoiceRecording() {
        _isRecordingVoice.value = true
    }

    fun stopVoiceRecordingAndSend(simulatedAudioBytes: ByteArray) {
        _isRecordingVoice.value = false
        sendAttachment(simulatedAudioBytes, "voice_note.m4a", "audio/mp4", AttachmentType.AUDIO)
    }

    fun cancelVoiceRecording() {
        _isRecordingVoice.value = false
    }
}

