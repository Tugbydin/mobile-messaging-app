package com.example.vibba;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ChatListActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACTS_PERMISSION = 100;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatModel> chatList;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentPhoneNumber;
    //private Set<String> contactPhoneNumbers = new HashSet<>();
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri imageUri;
    private List<String> contactPhoneNumbers = new ArrayList<>();


    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    uploadProfilePhotoToFirebase();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerViewChatList);
        chatList = new ArrayList<>();
        adapter = new ChatAdapter(this, chatList);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        ImageButton menuIcon = findViewById(R.id.menuIcon);

        menuIcon.setOnClickListener(view -> {

            PopupMenu popupMenu = new PopupMenu(ChatListActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

            try {
                Field[] fields = popupMenu.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popupMenu);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_add_photo) {
                    selectImageFromGallery();
                    return true;

                } else if (id == R.id.action_logout) {
                    new AlertDialog.Builder(ChatListActivity.this)
                            .setTitle("Çıkış Yap")
                            .setMessage("Çıkış yapmak istediğinize emin misiniz?")
                            .setPositiveButton("Evet", (dialog, which) -> {
                                FirebaseAuth.getInstance().signOut();
                                Intent intent = new Intent(ChatListActivity.this, RegisterActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .setNegativeButton("İptal", null)
                            .show();


                } else if (id == R.id.action_delete_account) {
                    new AlertDialog.Builder(ChatListActivity.this)
                            .setTitle("Hesabı Sil")
                            .setMessage("Hesabınızı kalıcı olarak silmek istediğinizden emin misiniz?")
                            .setPositiveButton("Evet", (dialog, which) -> {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null) {
                                    String userId = user.getPhoneNumber();
                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(userId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                user.delete().addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Intent intent = new Intent(ChatListActivity.this, RegisterActivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                        startActivity(intent);
                                                    } else {
                                                        Log.e("HesapSil", "Auth silinemedi", task.getException());
                                                    }
                                                });
                                            })
                                            .addOnFailureListener(e -> Log.e("HesapSil", "Firestore silinemedi", e));
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

        if (currentUser == null) {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();
        currentPhoneNumber = currentUser.getPhoneNumber();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Token alınamadı", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM", "Token: " + token);

                    if (currentPhoneNumber != null) {
                        firestore.collection("users")
                                .document(currentPhoneNumber)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("FCM", "Token Firestore’a kaydedildi"))
                                .addOnFailureListener(e -> Log.w("FCM", "Token kaydedilemedi", e));
                    }
                });

        checkContactsPermission();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d("ImagePicker", "Seçilen URI: " + imageUri.toString());
                            // Görseli ön izle
                            ImageView imageProfile = findViewById(R.id.imageProfile); // kendi ImageView ID’ni yaz
                            imageProfile.setImageURI(imageUri);
                        }
                    } else {
                        Toast.makeText(this, "Resim seçilmedi", Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfilePhotoToFirebase() {
        Log.d("Upload", "imageUri: " + imageUri);

        if (imageUri == null || currentPhoneNumber == null || currentPhoneNumber.trim().isEmpty()) {
            Log.e("Upload", "imageUri veya currentPhoneNumber null/boş");
            Toast.makeText(ChatListActivity.this, "Fotoğraf veya telefon numarası eksik", Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanedPhoneNumber = currentPhoneNumber.replaceAll("[^\\d]", ""); // Sadece rakamlar
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images/" + "+" +  cleanedPhoneNumber + ".jpg");

        try {
            InputStream stream = getContentResolver().openInputStream(imageUri);
            if (stream == null) {
                Toast.makeText(this, "Dosya açılamadı", Toast.LENGTH_SHORT).show();
                return;
            }

            UploadTask uploadTask = storageRef.putStream(stream);
            uploadTask
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d("Upload", "Fotoğraf yüklendi, URL alınıyor...");

                        // Fotoğraf URL'yi al
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String photoUrl = uri.toString();
                                    Log.d("Upload", "URL alındı: " + photoUrl);

                                    // Firestore'da güncelleme
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("profileImageUrl", photoUrl);

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document("+"+ cleanedPhoneNumber)  // Firestore'daki kullanıcı doküman adı
                                            .set(updates, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("Upload", "Firestore'a güncellendi");
                                                Toast.makeText(ChatListActivity.this, "Profil fotoğrafı güncellendi", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("Upload", "Firestore güncelleme hatası: " + e.getMessage());
                                                Toast.makeText(ChatListActivity.this, "Firestore güncelleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Upload", "URL alma hatası: " + e.getMessage());
                                    Toast.makeText(ChatListActivity.this, "Fotoğraf URL alınamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Upload", "Yükleme hatası: " + e.getMessage());
                        Toast.makeText(ChatListActivity.this, "Fotoğraf yüklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (FileNotFoundException e) {
            Log.e("Upload", "Dosya bulunamadı: " + e.getMessage());
            Toast.makeText(ChatListActivity.this, "Dosya bulunamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_CONTACTS_PERMISSION);
        } else {
            fetchContacts();
        }
    }

    private void fetchContacts() {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String rawNumber = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String cleaned = PhoneNumberUtils.normalizeNumber(rawNumber);

                if (!cleaned.isEmpty()) {
                    contactPhoneNumbers.add(cleaned);
                }
            }
            cursor.close();
        }
        loadChatList();
    }

    private void loadChatList() {
        firestore.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        Log.e("ChatListActivity", "Snapshot error: " + error);
                        return;
                    }

                    chatList.clear();
                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        ChatModel chat = snapshot.toObject(ChatModel.class);

                        if (chat != null) {
                            chat.setPhoneNumber(snapshot.getId());
                            chat.setStatus(snapshot.getString("status"));

                            if (!chat.getPhoneNumber().equals(currentPhoneNumber) && (contactPhoneNumbers.contains(chat.getPhoneNumber()))) {
                                String contactName = getContactName(chat.getPhoneNumber());
                                chat.setName(contactName);
                                chatList.add(chat);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    @SuppressLint("Range")
    private String getContactName(String phoneNumber) {
        String contactName = "Bilinmeyen";

        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        Cursor cursor = contentResolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            cursor.close();
        }

        return contactName;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CONTACTS_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchContacts();
        } else {
            Log.e("Permissions", "Rehber izni reddedildi.");
        }
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
        String userId = currentUser.getPhoneNumber();
        if (userId != null) {
            firestore.collection("users")
                    .document(userId)
                    .update("status", status)
                    .addOnSuccessListener(aVoid -> Log.d("Status", "Durum güncellendi: " + status))
                    .addOnFailureListener(e -> Log.e("Status", "Durum güncellenemedi", e));
        }
    }
}
