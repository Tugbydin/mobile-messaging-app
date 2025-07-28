package com.example.vibba;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.squareup.picasso.Picasso;

import org.webrtc.*;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    // UI Bileşenleri
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageView buttonSend, imageReceiverProfile;
    private Button btnAccept, btnReject;
    private TextView textCaller,textCallerName,statusTextView;
    private ProgressBar progressCalling;

    // Firebase ve WebRTC
    private FirebaseFirestore firestore;
    private String currentUserId, receiverUserId, currentCallId;
    private List<MessageModel> messageList;
    private MessageAdapter adapter;
    private FirebaseUser currentUser;

    // WebRTC Bağlantı Elemanları
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private String receiverName;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        firestore = FirebaseFirestore.getInstance();
        initViews();
        initFirebase();


        setupRecyclerView();
        loadMessages();

        buttonSend.setOnClickListener(v -> sendMessage());
        //btnStartVoiceCall.setOnClickListener(v -> startVoiceCall());
        btnAccept.setOnClickListener(v -> acceptCall());
        btnReject.setOnClickListener(v -> rejectCall());
        textCallerName = findViewById(R.id.textCallerName);
        statusTextView = findViewById(R.id.statusTextView);

        // Kullanıcı ismi göster
        textCallerName.setText(receiverName);
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Picasso.get().load(profileImageUrl)
                    .error(R.drawable.placeholder)
                    .into(imageReceiverProfile);
        }
        // Gerçek zamanlı çevrimiçi durumu dinle
        listenForUserStatus();

        ImageView menuIcon = findViewById(R.id.menuIcon);

        menuIcon.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(ChatActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu_2, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.VoiceCall) {
startVoiceCall();
                    return true;

                } else if (id == R.id.video_call) {

                    return true;

                }else if (id == R.id.clear_chat) {
                    new AlertDialog.Builder(ChatActivity.this)
                            .setTitle("Sohbeti Temizle")
                            .setMessage("Tüm mesajları silmek istediğinizden emin misiniz?")
                            .setPositiveButton("Evet", (dialog, which) -> {
                                // Mesajları silmek için Firestore'dan veri silme işlemi
                                clearChatForCurrentUser();  // Burada veritabanındaki mesajları silen fonksiyonu çağırıyoruz.
                                loadMessages();             // chatactivity'i yenilemesi lazım.

                                if (adapter != null) {
                                    adapter.clearMessages();  // Bu, sadece UI'da silme işlemi yapar.
                                }
                            })
                            .setNegativeButton("İptal", null)
                            .show();
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
       // btnStartVoiceCall = findViewById(R.id.btnStartVoiceCall);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        textCaller = findViewById(R.id.textCaller);
        progressCalling = findViewById(R.id.progressCalling);
        imageReceiverProfile = findViewById(R.id.imageReceiverProfile);
    }

    private void initFirebase() {
        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        receiverUserId = getIntent().getStringExtra("userId");
        messageList = new ArrayList<>();
        receiverUserId = getIntent().getStringExtra("userId");
        receiverName = getIntent().getStringExtra("receiverName"); // BUNU EKLE

    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(this, messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void listenForUserStatus() {
        DocumentReference userRef = firestore.collection("users").document(receiverUserId);
        userRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            Boolean isOnline = snapshot.getBoolean("isOnline");
            if (isOnline != null && isOnline) {
                statusTextView.setText("Çevrimiçi");
                statusTextView.setTextColor(Color.parseColor("#A5FFAB"));
            } else {
                statusTextView.setText("Çevrimdışı");
                statusTextView.setTextColor(Color.LTGRAY);
            }
        });
    }

    private void clearChatForCurrentUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Sohbet edilen diğer kişinin ID'si (bu bilgiyi alman gerekiyor)
        String otherUserId = receiverUserId;  // Bu değeri senin uygulamanın mantığına göre alman gerekiyor

        // Mesajlar koleksiyonunda, hem gönderici hem alıcı ID'si eşleşen tüm mesajları buluyoruz
        db.collection("messages")
                .whereIn("senderId", Arrays.asList(currentUserId, otherUserId))
                .whereIn("receiverId", Arrays.asList(currentUserId, otherUserId))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Veritabanından her mesajı güncelle ve silen kullanıcının 'deletedBy' alanına ekle
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().update("deletedBy", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("ClearChat", "Mesaj silindi: " + doc.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ClearChat", "Mesaj silinemedi: " + e.getMessage());
                                });
                    }
                    Toast.makeText(ChatActivity.this, "Sohbet temizlendi", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this, "Silme işlemi başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadMessages() {
        firestore.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    messageList.clear();

                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        MessageModel message = snapshot.toObject(MessageModel.class);
                        if (message != null && isBetweenCurrentUsers(message)) {


                            List<String> deletedBy = (List<String>) snapshot.get("deletedBy");
                            if (deletedBy != null && deletedBy.contains(currentUserId)) {
                                continue;
                            }

                            message.setDocumentId(snapshot.getId());
                            messageList.add(message);

                            // Okunma durumunu güncelle
                            if (message.getReceiverId().equals(currentUserId) &&
                                    "sent".equals(message.getMessageStatus())) {
                                snapshot.getReference().update("messageStatus", "delivered");
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                });
    }


    private boolean isBetweenCurrentUsers(MessageModel message) {
        return (message.getSenderId().equals(currentUserId) && message.getReceiverId().equals(receiverUserId))
                || (message.getSenderId().equals(receiverUserId) && message.getReceiverId().equals(currentUserId));
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        MessageModel message = new MessageModel(currentUserId, receiverUserId, messageText, System.currentTimeMillis());
        message.setMessageStatus("sent");

        firestore.collection("messages").add(message)
                .addOnSuccessListener(doc -> editTextMessage.setText(""))
                .addOnFailureListener(e -> Log.e("SendMessage", "Mesaj gönderilemedi: " + e.getMessage()));
    }

    private void startVoiceCall() {
        currentCallId = generateCallId();

        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", currentUserId);
        callData.put("receiverId", receiverUserId);
        callData.put("status", "ringing");

        firestore.collection("calls").document(currentCallId).set(callData)
                .addOnSuccessListener(aVoid -> {
                    textCaller.setText("Çağrı başlatılıyor...");
                    textCaller.setVisibility(View.VISIBLE);
                    progressCalling.setVisibility(View.VISIBLE);

                    sendCallToReceiver();  // Alıcıya bildirimi tetikle
                    startWebRTCConnection(currentCallId);
                })
                .addOnFailureListener(e -> {
                    textCaller.setText("Çağrı başlatılamadı.");
                    progressCalling.setVisibility(View.GONE);
                    Log.e("VoiceCall", "Başlatma hatası: " + e.getMessage());
                });
    }

    private void createAnswer(String callId) {
        peerConnection.createAnswer(new SdpObserverAdapter() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SdpObserverAdapter(), sdp);

                Map<String, Object> answerMap = new HashMap<>();
                answerMap.put("sdp", sdp.description);
                answerMap.put("type", sdp.type.canonicalForm());

                firestore.collection("calls").document(callId)
                        .update("answer", answerMap)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent(ChatActivity.this, VoiceCallActivity.class);
                            intent.putExtra("callId", callId);
                            startActivity(intent);
                        });
            }
        }, new MediaConstraints());
    }

    private void sendCallToReceiver() {
        // Çağrı statüsünü güncelleyerek alıcı tarafın dinlemesini sağlar
        firestore.collection("calls").document(currentCallId)
                .update("status", "ringing")
                .addOnSuccessListener(aVoid -> Log.d("Call", "Alıcıya çağrı bildirimi gönderildi."))
                .addOnFailureListener(e -> Log.e("Call", "Bildirim hatası: " + e.getMessage()));
    }

    private void acceptCall() {
        firestore.collection("calls").document(currentCallId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    textCaller.setText("Çağrı kabul edildi.");
                    setupWebRTC();
                    listenForOffer(currentCallId);
                    listenForRemoteIceCandidates(currentCallId);
                });
    }

    private void listenForOffer(String callId) {
        firestore.collection("calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    Map<String, Object> offerMap = (Map<String, Object>) snapshot.get("offer");
                    if (offerMap != null) {
                        String sdp = (String) offerMap.get("sdp");
                        String type = (String) offerMap.get("type");

                        SessionDescription offer = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), sdp
                        );

                        peerConnection.setRemoteDescription(new SdpObserverAdapter(), offer);
                        createAnswer(callId);
                    }
                });
    }

    private void rejectCall() {
        if (currentCallId == null) return;

        firestore.collection("calls").document(currentCallId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> {
                    textCaller.setText("Çağrı reddedildi.");
                    finish();
                })
                .addOnFailureListener(e -> Log.e("Call", "Reddetme hatası: " + e.getMessage()));
    }

    private void startWebRTCConnection(String callId) {
        setupWebRTC();
        createOffer(callId);
        listenForRemoteAnswer(callId);
        listenForRemoteIceCandidates(callId);
    }

    private void setupWebRTC() {
        PeerConnectionFactory.InitializationOptions options =
                PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        factory = PeerConnectionFactory.builder().createPeerConnectionFactory();

        audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("101", audioSource);

        List<PeerConnection.IceServer> iceServers = Collections.singletonList(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        );

        peerConnection = factory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("localPeer") {
            @Override
            public void onIceCandidate(IceCandidate candidate) {
                Map<String, Object> candidateData = new HashMap<>();
                candidateData.put("sdpMid", candidate.sdpMid);
                candidateData.put("sdpMLineIndex", candidate.sdpMLineIndex);
                candidateData.put("candidate", candidate.sdp);

                firestore.collection("calls")
                        .document(currentCallId)
                        .collection("candidates")
                        .add(candidateData);
            }
        });

        peerConnection.addTrack(localAudioTrack);
    }

    private void createOffer(String callId) {
        peerConnection.createOffer(new SdpObserverAdapter() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new SdpObserverAdapter(), sdp);

                Map<String, Object> offerData = new HashMap<>();
                offerData.put("sdp", sdp.description);
                offerData.put("type", sdp.type.canonicalForm());

                firestore.collection("calls").document(callId)
                        .update("offer", offerData)
                        .addOnSuccessListener(aVoid -> Log.d("WebRTC", "Teklif gönderildi."))
                        .addOnFailureListener(e -> Log.e("WebRTC", "Teklif hatası: " + e.getMessage()));
            }
        }, new MediaConstraints());
    }

    private void updateUserStatus(String status) {
        if (currentUser != null) {
            String userId = currentUser.getPhoneNumber();
            if (userId != null) {
                firestore.collection("users")
                        .document(userId)
                        .update("status", status)
                        .addOnSuccessListener(aVoid -> {
                            // Durum başarıyla güncellendi
                        })
                        .addOnFailureListener(e -> {
                            // Hata durumunda
                            Toast.makeText(ChatActivity.this, "Durum güncellenemedi.", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void listenForRemoteAnswer(String callId) {
        firestore.collection("calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    Map<String, Object> answerMap = (Map<String, Object>) snapshot.get("answer");
                    if (answerMap != null) {
                        String sdp = (String) answerMap.get("sdp");
                        String type = (String) answerMap.get("type");

                        SessionDescription answer = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), sdp
                        );

                        peerConnection.setRemoteDescription(new SdpObserverAdapter(), answer);
                    }
                });
    }

    private void listenForRemoteIceCandidates(String callId) {
        firestore.collection("calls").document(callId)
                .collection("candidates")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String sdpMid = dc.getDocument().getString("sdpMid");
                            Long sdpMLineIndex = dc.getDocument().getLong("sdpMLineIndex");
                            String candidateStr = dc.getDocument().getString("candidate");

                            if (sdpMid != null && sdpMLineIndex != null && candidateStr != null) {
                                IceCandidate candidate = new IceCandidate(
                                        sdpMid, sdpMLineIndex.intValue(), candidateStr
                                );
                                peerConnection.addIceCandidate(candidate);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        setUserStatus("offline");
    }
    private void setUserStatus(String status) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("StatusUpdate", "Kullanıcı oturumu yok. currentUser null.");
            return;
        }

        String phoneNumber = currentUser.getPhoneNumber();

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.e("StatusUpdate", "Kullanıcının telefon numarası null veya boş.");
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("users")
                .document(phoneNumber)
                .update("status", status)
                .addOnSuccessListener(aVoid ->
                        Log.d("StatusUpdate", "Durum başarıyla güncellendi: " + status))
                .addOnFailureListener(e ->
                        Log.e("StatusUpdate", "Durum güncellenemedi!", e));
    }



    private String generateCallId() {
        return "CALL_" + System.currentTimeMillis();
    }


    static class SdpObserverAdapter implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) {}
        @Override public void onSetFailure(String s) {}
    }

    static class CustomPeerConnectionObserver implements PeerConnection.Observer {
        private final String tag;
        public CustomPeerConnectionObserver(String tag) { this.tag = tag; }

        @Override public void onIceCandidate(IceCandidate candidate) {}
        @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
        @Override public void onAddStream(MediaStream stream) {}
        @Override public void onSignalingChange(PeerConnection.SignalingState state) {
            Log.d(tag, "Signaling State: " + state);
        }
        @Override public void onIceConnectionChange(PeerConnection.IceConnectionState state) {}
        @Override public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}
        @Override public void onDataChannel(DataChannel dataChannel) {}
        @Override public void onRenegotiationNeeded() {}
        @Override public void onTrack(RtpTransceiver transceiver) {}
        @Override public void onConnectionChange(PeerConnection.PeerConnectionState state) {}

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override public void onRemoveStream(MediaStream mediaStream) {}
    }
}
