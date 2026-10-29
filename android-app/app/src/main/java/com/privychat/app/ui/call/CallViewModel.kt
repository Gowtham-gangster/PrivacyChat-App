package com.privychat.app.ui.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.privychat.app.core.network.CallSignalDto
import com.privychat.app.core.webrtc.WebRtcClient
import com.privychat.app.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    application: Application,
    private val callRepository: CallRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val callId: UUID = UUID.fromString(checkNotNull(savedStateHandle.get<String>("callId")))
    val otherPartyName: String = checkNotNull(savedStateHandle.get<String>("otherPartyName"))
    val callType: String = checkNotNull(savedStateHandle.get<String>("callType"))
    val isIncoming: Boolean = checkNotNull(savedStateHandle.get<String>("isIncoming")).toBoolean()

    private val _callState = MutableStateFlow(if (isIncoming) WebRtcClient.CallState.RINGING else WebRtcClient.CallState.DIALING)
    val callState: StateFlow<WebRtcClient.CallState> = _callState

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isCameraEnabled = MutableStateFlow(callType == "VIDEO")
    val isCameraEnabled: StateFlow<Boolean> = _isCameraEnabled

    private var rtcClient: WebRtcClient? = null

    init {
        initWebRtc()
    }

    private fun initWebRtc() {
        rtcClient = WebRtcClient(
            context = getApplication(),
            onLocalIceCandidate = { candidate ->
                viewModelScope.launch {
                    val signal = CallSignalDto(
                        callId = callId,
                        callerId = null,
                        recipientId = UUID.randomUUID(), // Target contact ID
                        signalType = "ICE_CANDIDATE",
                        callType = callType,
                        sdp = null,
                        candidate = candidate
                    )
                    callRepository.sendCallSignal(signal)
                }
            },
            onCallStateChanged = { state ->
                _callState.value = state
            }
        )

        rtcClient?.startLocalAudioTrack()

        if (!isIncoming) {
            // Caller initiates offer
            rtcClient?.createPeerConnection()
            rtcClient?.createOffer { offerDesc ->
                viewModelScope.launch {
                    val signal = CallSignalDto(
                        callId = callId,
                        callerId = null,
                        recipientId = UUID.randomUUID(),
                        signalType = "OFFER",
                        callType = callType,
                        sdp = offerDesc.description,
                        candidate = null
                    )
                    callRepository.sendCallSignal(signal)
                }
            }
        }
    }

    fun answerCall() {
        _callState.value = WebRtcClient.CallState.CONNECTING
        rtcClient?.createPeerConnection()
        // Respond to offer
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        rtcClient?.toggleMute(next)
    }

    fun toggleCamera() {
        val next = !_isCameraEnabled.value
        _isCameraEnabled.value = next
        rtcClient?.toggleCamera(next)
    }

    fun endCall() {
        viewModelScope.launch {
            val signal = CallSignalDto(
                callId = callId,
                callerId = null,
                recipientId = UUID.randomUUID(),
                signalType = "HANGUP",
                callType = callType,
                sdp = null,
                candidate = null
            )
            callRepository.sendCallSignal(signal)
        }
        rtcClient?.close()
        _callState.value = WebRtcClient.CallState.TERMINATED
    }

    override fun onCleared() {
        super.onCleared()
        rtcClient?.close()
    }
}

