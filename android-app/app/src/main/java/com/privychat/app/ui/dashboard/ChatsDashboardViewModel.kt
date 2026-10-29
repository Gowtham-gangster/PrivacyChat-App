package com.privychat.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privychat.app.core.network.StompWebSocketClient
import com.privychat.app.data.repository.ConversationRepository
import com.privychat.app.domain.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsDashboardViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val stompClient: StompWebSocketClient
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val connectionState = stompClient.connectionState

    val conversations: StateFlow<List<Conversation>> = combine(
        conversationRepository.getConversationsFlow(),
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { it.participantName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Connect STOMP WebSocket for real-time delivery
        stompClient.connect()
        refreshConversations()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun refreshConversations() {
        viewModelScope.launch {
            conversationRepository.syncConversations()
        }
    }
}

