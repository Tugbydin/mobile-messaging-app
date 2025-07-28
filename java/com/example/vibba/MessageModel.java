package com.example.vibba;

import java.util.List;

public class MessageModel {

    private String senderId;
    private String receiverId;
    private String messageText;
    private long timestamp;
    private String messageStatus; // 'sent', 'delivered', 'seen'
    private List<String> deletedBy;

    // Constructor
    public MessageModel() {
        // Firebase Firestore için parametresiz constructor gerekli
    }

    public MessageModel(String senderId, String receiverId, String messageText, long timestamp) {
        this.senderId = senderId != null ? senderId : ""; // null değilse atama yap, null ise boş string
        this.receiverId = receiverId != null ? receiverId : ""; // aynı şekilde receiverId için
        this.messageText = messageText != null ? messageText : ""; // mesaja da null kontrolü
        this.timestamp = timestamp;
        this.messageStatus = "sent"; // Varsayılan durum "sent"
    }

    // Getter ve Setter'lar
    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId != null ? senderId : "";
    }

    public String getReceiverId() {
        return receiverId != null ? receiverId : "";
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId != null ? receiverId : "";
    }

    public String getMessageText() {
        return messageText != null ? messageText : "";
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText != null ? messageText : "";
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageStatus() {
        return messageStatus != null ? messageStatus : "sent";
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus != null ? messageStatus : "sent";
    }

    private String documentId;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public List<String> getDeletedBy() { return deletedBy; }
    public void setDeletedBy(List<String> deletedBy) { this.deletedBy = deletedBy; }

}
