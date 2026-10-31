package com.privychat.app.core.webrtc

import android.content.Context
import android.util.Log
import com.privychat.app.BuildConfig
import com.privychat.app.core.network.CallSignalDto
import com.privychat.app.core.network.IceCandidateDto
import org.webrtc.*
import java.util.UUID

class WebRtcClient(
    private val context: Context,
    private val onLocalIceCandidate: (IceCandidateDto) -> Unit,
    private val onCallStateChanged: (CallState) -> Unit
) {
    private val tag = "WebRtcClient"
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null

    enum class CallState {
        IDLE, DIALING, RINGING, CONNECTING, CONNECTED, TERMINATED
    }

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun startLocalAudioTrack(): AudioTrack {
        val constraints = MediaConstraints()
        audioSource = peerConnectionFactory?.createAudioSource(constraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)
        return localAudioTrack!!
    }

    fun startLocalVideoTrack(surfaceTextureHelper: SurfaceTextureHelper, capturer: VideoCapturer): VideoTrack {
        videoSource = peerConnectionFactory?.createVideoSource(capturer.isScreencast)
        capturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        capturer.startCapture(1280, 720, 30)
        localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
        return localVideoTrack!!
    }

    fun createPeerConnection(
        iceServers: List<PeerConnection.IceServer> = listOf(
            PeerConnection.IceServer.builder(BuildConfig.STUN_SERVER).createIceServer()
        ),
        remoteVideoSink: VideoSink? = null
    ) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                onLocalIceCandidate(
                    IceCandidateDto(
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex,
                        candidate = candidate.sdp
                    )
                )
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(tag, "ICE Connection State: $newState")
                when (newState) {
                    PeerConnection.IceConnectionState.CHECKING -> onCallStateChanged(CallState.CONNECTING)
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> onCallStateChanged(CallState.CONNECTED)
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.CLOSED -> onCallStateChanged(CallState.TERMINATED)
                    else -> {}
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                if (track is VideoTrack && remoteVideoSink != null) {
                    track.addSink(remoteVideoSink)
                }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(dc: DataChannel) {}
            override fun onRenegotiationNeeded() {}
        })

        // Add local tracks to peer connection
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }
    }

    fun createOffer(onSuccess: (SessionDescription) -> Unit) {
        val sdpConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        onSuccess(desc)
                    }
                }, desc)
            }
        }, sdpConstraints)
    }

    fun handleRemoteOffer(sdpOffer: String, onSuccess: (SessionDescription) -> Unit) {
        val desc = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                val sdpConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                peerConnection?.createAnswer(object : SdpObserverAdapter() {
                    override fun onCreateSuccess(answerDesc: SessionDescription) {
                        peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                onSuccess(answerDesc)
                            }
                        }, answerDesc)
                    }
                }, sdpConstraints)
            }
        }, desc)
    }

    fun handleRemoteAnswer(sdpAnswer: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {}, desc)
    }

    fun addRemoteIceCandidate(candidateDto: IceCandidateDto) {
        val candidate = IceCandidate(candidateDto.sdpMid, candidateDto.sdpMLineIndex, candidateDto.candidate)
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleMute(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun close() {
        peerConnection?.close()
        peerConnection = null
        localAudioTrack?.dispose()
        localVideoTrack?.dispose()
        audioSource?.dispose()
        videoSource?.dispose()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        onCallStateChanged(CallState.TERMINATED)
    }

    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(s: String) {}
        override fun onSetFailure(s: String) {}
    }
}

