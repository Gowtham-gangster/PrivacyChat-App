package com.privychat.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privychat.app.core.database.ContactEntity
import com.privychat.app.data.repository.ContactRepository
import com.privychat.app.data.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val allContacts: StateFlow<List<ContactEntity>> = contactRepository.getAllContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registeredContacts: StateFlow<List<ContactEntity>> = contactRepository.getRegisteredContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inviteStatus = MutableStateFlow<String?>(null)
    val inviteStatus: StateFlow<String?> = _inviteStatus

    fun discoverContacts(phoneList: List<String>) {
        viewModelScope.launch {
            contactRepository.discoverContacts(phoneList)
        }
    }

    fun sendEmailInvitation(email: String) {
        viewModelScope.launch {
            val result = contactRepository.sendInvitation(email)
            result.onSuccess {
                _inviteStatus.value = "Invitation sent successfully to $email"
            }.onFailure { err ->
                _inviteStatus.value = "Failed to send invitation: ${err.message}"
            }
        }
    }

    fun startConversation(recipientUserId: UUID, onReady: (convId: UUID) -> Unit) {
        viewModelScope.launch {
            // Initiate or load 1-to-1 conversation
            val convId = UUID.randomUUID()
            onReady(convId)
        }
    }

    fun clearInviteStatus() {
        _inviteStatus.value = null
    }
}

