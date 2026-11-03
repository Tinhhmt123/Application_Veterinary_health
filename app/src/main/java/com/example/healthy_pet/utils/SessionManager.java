package com.example.healthy_pet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Quản lý phiên đăng nhập và cấu hình hệ thống bằng SharedPreferences (NV 1 & Menu trượt).
 */
public class SessionManager {
    private static final String PREF_NAME = "HealthyPetPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role"; // "USER" hoặc "ADMIN"
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ALERT_7_DAYS = "alert_7_days";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUserSession(Long userId, String fullName, String email, String role) {
        editor.putLong(KEY_USER_ID, userId != null ? userId : 1L);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role != null ? role : "USER");
        editor.apply();
    }

    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, 1L);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "Nguyễn Minh Tinh");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "minhtinh@gmail.com");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "USER");
    }

    public void setRole(String role) {
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean isDark) {
        editor.putBoolean(KEY_DARK_MODE, isDark);
        editor.apply();
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public boolean isAlert7DaysEnabled() {
        return prefs.getBoolean(KEY_ALERT_7_DAYS, true);
    }

    public void setAlert7DaysEnabled(boolean enabled) {
        editor.putBoolean(KEY_ALERT_7_DAYS, enabled);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
