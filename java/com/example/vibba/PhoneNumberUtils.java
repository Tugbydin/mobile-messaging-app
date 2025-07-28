package com.example.vibba;

public class PhoneNumberUtils {
    public static String normalize(String rawNumber) {
        if (rawNumber == null) return "";

        // Sadece rakam ve + işareti
        String cleaned = rawNumber.replaceAll("[^\\d+]", "");

        // 0 ile başlıyorsa +90 ile değiştir
        if (cleaned.startsWith("0")) {
            cleaned = "+90" + cleaned.substring(1);
        }

        // Eğer başında ülke kodu yoksa, örneğin 5xx ile başlıyorsa, Türkiye varsayımıyla +90 ekle
        if (cleaned.matches("^5\\d{9}$")) {
            cleaned = "+90" + cleaned;
        }

        return cleaned;
    }
}
