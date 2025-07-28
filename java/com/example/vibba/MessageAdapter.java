package com.example.vibba;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final Context context;
    private final List<MessageModel> messageList;
    private final String currentUserId;

    public MessageAdapter(Context context, List<MessageModel> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                viewType == 1 ? R.layout.item_message_sent : R.layout.item_message_received,
                parent,
                false
        );
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        MessageModel message = messageList.get(position);

        if (message.getDeletedBy() != null && message.getDeletedBy().contains(currentUserId)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }

        holder.messageText.setText(message.getMessageText());
        holder.messageStatus.setText(getStatusText(message.getMessageStatus()));
        holder.messageTime.setText(formatTimestamp(message.getTimestamp()));
    }

    private String getStatusText(String status) {
        switch (status) {
            case "sent": return "✔\uFE0F";
            case "delivered": return "✔\uFE0F✔\uFE0F";
            case "seen": return "✔\uFE0F✔\uFE0F";
            default: return "";
        }
    }
    public void clearMessages() {
        messageList.clear();
        notifyDataSetChanged();
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSenderId().equals(currentUserId) ? 1 : 2;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime, messageStatus;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageTime = itemView.findViewById(R.id.messageTime);
            messageStatus = itemView.findViewById(R.id.messageStatus);
        }
    }
}
