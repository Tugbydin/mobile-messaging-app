package com.example.vibba;

import com.google.firebase.firestore.*;

public class CallManager {
    private final FirebaseFirestore firestore;

    public CallManager() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void startCall(String callerId, String receiverId) {
        String callId = firestore.collection("calls").document().getId();

       // firestore.collection("calls").document(callId).set(new CallModel(callerId, receiverId, "ringing"));
    }
}
