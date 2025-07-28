package com.example.vibba;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;

public class CallRequestActivity extends AppCompatActivity {
    private TextView textIncomingCall;
    private FirebaseFirestore firestore;
    private String callId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_request);

        textIncomingCall = findViewById(R.id.textIncomingCall);
        Button btnAccept = findViewById(R.id.btnAccept);

        firestore = FirebaseFirestore.getInstance();

        firestore.collection("calls").whereEqualTo("status", "ringing")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) return;

                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        callId = snapshot.getId();
                        textIncomingCall.setText("Gelen Çağrı!");
                    }
                });

        btnAccept.setOnClickListener(v -> acceptCall());
    }

    private void acceptCall() {
        Intent intent = new Intent(this, VoiceCallActivity.class);
        intent.putExtra("callId", callId);
        startActivity(intent);
    }
}
