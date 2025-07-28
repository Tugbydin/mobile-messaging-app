package com.example.vibba;

public class ChatModel {
    private String userId;
    private String name; // Firebase'deki ad
    private String status;
    private String profileImageUrl;
    private String phoneNumber;
    private String contactName; // 📱 Rehberden gelen ad (yeni)


    public ChatModel() {}

    public ChatModel(String userId, String name, String status, String profileImageUrl, String phoneNumber, String contactName) {
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
    }

    // Getter ve Setter'lar

    public String getUserId() {
        return userId != null ? userId : "";  // Null kontrolü ekledik
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status != null ? status : "offline"; } // Default "offline" durumu ekledik
    public void setStatus(String status) { this.status = status; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
}
