package com.example.vibba;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 saniye splash ekranı gösterelim
    private static final int NOTIFICATION_PERMISSION_CODE = 1001; // Bildirim izin kodu
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        checkNotificationPermission(); // Önce bildirim iznini kontrol et
    }

    private void startAppFlow() {
        try {
            new Handler().postDelayed(() -> {
                if (currentUser == null) {
                    // Kullanıcı girmemişse RegisterActivity'e yönlendir
                    startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
                } else {
                    // Kullanıcı giriş yapmışsa ChatListActivity'e yönlendir
                    updateUserStatus("online"); // Durumu "online" olarak güncelle
                    startActivity(new Intent(SplashActivity.this, ChatListActivity.class));
                }
                finish();
            }, SPLASH_DELAY);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gerekli izin verilmediği için işlem yapılamıyor.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 ve üstü
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // İzin yoksa kullanıcıdan iste
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            } else {
                // İzin zaten verilmiş, normal akışa geç
                startAppFlow();
            }
        } else {
            // Android 13 altı ise izin gerekmiyor, normal akışa geç
            startAppFlow();
        }
    }

    // Kullanıcı izin isteğine cevap verince çalışacak
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bildirim izni verildi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bildirim izni reddedildi.", Toast.LENGTH_SHORT).show();
            }
            // Her iki durumda da app akışına geçelim
            startAppFlow();
        }
    }

    // Kullanıcının durumunu güncellemek için Firebase'e yazıyoruz
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
                            Toast.makeText(SplashActivity.this, "Durum güncellenemedi.", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }
}
