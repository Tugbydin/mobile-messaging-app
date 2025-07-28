package com.example.vibba;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.*;
import org.webrtc.*;
import java.util.*;

public class VoiceCallActivity extends AppCompatActivity {

    private TextView textCaller;
    private Button btnEndCall;
    private PeerConnection peerConnection;
    private PeerConnectionFactory factory;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private String callId;
    private String currentUserId;
    private String receiverUserId;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        textCaller = findViewById(R.id.textCaller);
        btnEndCall = findViewById(R.id.btnEndCall);
        firestore = FirebaseFirestore.getInstance();

        // Alınan callId ve kullanıcı bilgileri
        callId = getIntent().getStringExtra("callId");
        currentUserId = getIntent().getStringExtra("currentUserId");
        receiverUserId = getIntent().getStringExtra("receiverUserId");

        // Ses kaydı izni kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        if (callId == null || currentUserId == null || receiverUserId == null) {
            finish();
            return;
        }

        setupWebRTC();
        listenForRemoteDescription();
        listenForIncomingIceCandidates();

        btnEndCall.setOnClickListener(v -> endCall());
    }

    private void setupWebRTC() {
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        factory = PeerConnectionFactory.builder().createPeerConnectionFactory();

        audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("101", audioSource);

        List<PeerConnection.IceServer> iceServers = Collections.singletonList(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        );

        peerConnection = factory.createPeerConnection(new PeerConnection.RTCConfiguration(iceServers), new CustomPeerConnectionObserver());

        MediaStream stream = factory.createLocalMediaStream("localStream");
        stream.addTrack(localAudioTrack);
        peerConnection.addStream(stream);
    }

    private void startCall() {
        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", currentUserId);
        callData.put("receiverId", receiverUserId);
        callData.put("status", "ringing");

        callId = firestore.collection("calls").document().getId();
        firestore.collection("calls").document(callId).set(callData);

        MediaConstraints constraints = new MediaConstraints();
        peerConnection.createOffer(new SdpObserverAdapter() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(this, sdp);
                firestore.collection("calls").document(callId)
                        .update("sdp", sdp.description, "type", sdp.type.canonicalForm());
            }
        }, constraints);
    }

    private void listenForRemoteDescription() {
        firestore.collection("calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.contains("answer")) {
                        String sdp = snapshot.getString("answer");
                        String type = snapshot.getString("answerType");
                        SessionDescription remoteDesc = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), sdp);
                        peerConnection.setRemoteDescription(new SdpObserverAdapter(), remoteDesc);
                    }
                });
    }

    private void listenForIncomingIceCandidates() {
        firestore.collection("calls").document(callId).collection("iceCandidates")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            IceCandidate candidate = new IceCandidate(
                                    doc.getString("sdpMid"),
                                    doc.getLong("sdpMLineIndex").intValue(),
                                    doc.getString("candidate")
                            );
                            peerConnection.addIceCandidate(candidate);
                        }
                    }
                });
    }

    private void sendIceCandidateToFirebase(IceCandidate candidate) {
        Map<String, Object> data = new HashMap<>();
        data.put("candidate", candidate.sdp);
        data.put("sdpMid", candidate.sdpMid);
        data.put("sdpMLineIndex", candidate.sdpMLineIndex);

        firestore.collection("calls").document(callId).collection("iceCandidates").add(data);
    }

    private void endCall() {
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        firestore.collection("calls").document(callId)
                .update("status", "ended");

        finish();
    }

    // ICE adayları için callback metodu
    private class CustomPeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onIceCandidate(IceCandidate candidate) {
            sendIceCandidateToFirebase(candidate);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
            // Durumu logla
        }

        @Override
        public void onAddStream(MediaStream stream) {
            // Yeni stream eklendiğinde yapılacaklar
        }

        @Override
        public void onRemoveStream(MediaStream stream) {
            // Stream kaldırıldığında yapılacaklar
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {}
        public void onIceConnectionReceivingChange(boolean b) {}
        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}
        public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
        public void onDataChannel(DataChannel channel) {}
        public void onRenegotiationNeeded() {}
        public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {}
    }

    // SDP Observer Adapter
    private class SdpObserverAdapter implements SdpObserver {
        public void onCreateSuccess(SessionDescription sessionDescription) {}
        public void onSetSuccess() {}
        public void onCreateFailure(String s) {}
        public void onSetFailure(String s) {}
    }
}
