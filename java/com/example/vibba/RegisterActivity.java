package com.example.vibba;

import  android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private EditText phoneNumberField, verificationCodeField;
    private Button sendCodeButton, verifyCodeButton;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initFirebase();

        sendCodeButton.setOnClickListener(v -> requestVerificationCode());
        verifyCodeButton.setOnClickListener(v -> verifyCode());

    }

    private void initViews() {
        phoneNumberField = findViewById(R.id.phoneNumberField);
        verificationCodeField = findViewById(R.id.verificationCodeField);
        sendCodeButton = findViewById(R.id.getVerificationCodeButton);
        verifyCodeButton = findViewById(R.id.registerButton);

    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void requestVerificationCode() {
        String number = phoneNumberField.getText().toString().trim();

        if (number.length() != 10) {
            Toast.makeText(this, "Lütfen 10 haneli geçerli bir numara girin", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullPhoneNumber = "+90" + number;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(fullPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.e("RegisterActivity", "Verification failed", e);
                        Toast.makeText(RegisterActivity.this, "Doğrulama başarısız: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        Toast.makeText(RegisterActivity.this, "Kod gönderildi", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }




    private void verifyCode() {
        String code = verificationCodeField.getText().toString().trim();

        if (verificationId == null || code.isEmpty()) {
            Toast.makeText(this, "Doğrulama kodu eksik", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = task.getResult().getUser();
                if (user != null) {
                    saveUserToFirestore(user);
                }
            } else {
                Toast.makeText(this, "Kod doğrulanamadı", Toast.LENGTH_SHORT).show();
                Log.e("RegisterActivity", "Sign in failed", task.getException());
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String phone = user.getPhoneNumber();
        if (phone == null) {
            Toast.makeText(this, "Telefon numarası alınamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid()); // 👈 EKLENDİ
        userData.put("phoneNumber", phone);
        userData.put("name", "Yeni Kullanıcı");
        userData.put("profileImageUrl", "image");
        userData.put("status", "Çevrimiçi");

        firestore.collection("users")
                .document(phone)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Kayıt başarılı", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, ChatListActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Firestore kayıt hatası", e);
                    Toast.makeText(this, "Kayıt başarısız: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

}
