package com.example.vibba;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private Context context;
    private List<ChatModel> chatList;

    public ChatAdapter(Context context, List<ChatModel> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);

        // Rehberdeki adı göster, yoksa Firebase'deki adı göster
        String displayName = chat.getContactName() != null && !chat.getContactName().isEmpty()
                ? chat.getContactName()
                : chat.getName() != null ? chat.getName() : "Bilinmeyen";

        holder.textUsername.setText(displayName);
        holder.textLastMessage.setText(chat.getStatus() != null ? chat.getStatus() : "");



        if (chat.getProfileImageUrl() != null && !chat.getProfileImageUrl().isEmpty()) {
            Picasso.get().load(chat.getProfileImageUrl())
                    .error(R.drawable.placeholder) // Resim yüklenemezse placeholder göster
                    .into(holder.imageProfile);
        } else {
            holder.imageProfile.setImageResource(R.drawable.placeholder);
        }

        // Durum kontrolü ve renk ayarlaması
        if ("online".equals(chat.getStatus())) {
            holder.statusTextView.setText("Çevrimiçi");
            holder.statusTextView.setTextColor(Color.GREEN);
            holder.statusTextView.setVisibility(View.VISIBLE); // Online olanları gösterecek
        } else if ("offline".equals(chat.getStatus())) {
            holder.statusTextView.setText("Çevrimdışı");
            holder.statusTextView.setTextColor(Color.GRAY);
            holder.statusTextView.setVisibility(View.VISIBLE); // Offline olanları gösterecek
        } else {
            holder.statusTextView.setVisibility(View.GONE); // Durum belirtilmemişse gizle
        }

        // UserId'yi doğru gönderdiğinden emin ol
        Log.d("ChatAdapter", "Sending UserId: " + chat.getUserId());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);

            intent.putExtra("userId", chat.getUserId());
            intent.putExtra("receiverName", displayName);
            intent.putExtra("profileImageUrl", chat.getProfileImageUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername, textLastMessage, statusTextView;
        ImageView imageProfile;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.messageText);     // İsim alanı
            textLastMessage = itemView.findViewById(R.id.messageTime);  // Durum alanı
            imageProfile = itemView.findViewById(R.id.imageProfile);    // Profil Fotoğrafı
            statusTextView = itemView.findViewById(R.id.statusTextView);// Çevrimiçi Durumu
        }
    }
}
