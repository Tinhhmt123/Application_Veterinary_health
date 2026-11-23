
package com.example.healthy_pet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.User;

public class SessionManager {

    private static final String PREF_NAME = "HealthyPetPrefs";

    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_ROLE = "role";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ALERT_7_DAYS = "alert_7_days";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );

        editor = prefs.edit();

        // Khôi phục Access Token khi mở ứng dụng
        String savedToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        RetrofitClient.setAccessToken(savedToken);

        Log.d("AUTH_DEBUG",
                "Session initialized. Token exists: " + hasValidToken(savedToken));
    }

    // Lưu phiên đăng nhập cơ bản
    public void saveUserSession(
            Long userId,
            String fullName,
            String email,
            String role
    ) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId != null ? userId : 1L);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role != null ? role : "USER");
        editor.apply();

        // Không xóa Access Token hiện có tại đây
    }

    // Lưu thông tin người dùng và Access Token
    public void saveUser(User user) {
        if (user == null) {
            Log.e("AUTH_DEBUG", "Cannot save null user");
            return;
        }

        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        editor.putLong(
                KEY_USER_ID,
                user.getId() != null ? user.getId() : 1L
        );

        editor.putString(
                KEY_FULL_NAME,
                user.getFullName() != null ? user.getFullName() : ""
        );

        editor.putString(
                KEY_EMAIL,
                user.getEmail() != null ? user.getEmail() : ""
        );

        editor.putString(
                KEY_PHONE_NUMBER,
                user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
        );

        editor.putString(KEY_AVATAR_URL, user.getAvatarUrl());

        editor.putString(
                KEY_ROLE,
                user.getRole() != null ? user.getRole() : "USER"
        );

        // Chỉ cập nhật token nếu API thực sự trả về token mới
        String token = user.getAccessToken();

        if (hasValidToken(token)) {
            String cleanToken = token.trim();

            editor.putString(KEY_ACCESS_TOKEN, cleanToken);

            Log.d("AUTH_DEBUG", "Received new access token");
        } else {
            // Giữ nguyên token cũ nếu response không chứa access_token
            Log.d("AUTH_DEBUG",
                    "Response has no access_token. Keeping existing token.");
        }

        editor.apply();

        // Đọc token hiện có sau khi lưu thông tin
        String savedToken = prefs.getString(KEY_ACCESS_TOKEN, null);

        // Đồng bộ token sang Retrofit
        RetrofitClient.setAccessToken(savedToken);

        boolean tokenExists = hasValidToken(savedToken);

        Log.d("AUTH_DEBUG", "Saved token exists: " + tokenExists);
        Log.d("AUTH_DEBUG", "Retrofit token initialized: " + tokenExists);
    }

    // Kiểm tra token có tồn tại hay không
    private boolean hasValidToken(String token) {
        return token != null && !token.trim().isEmpty();
    }

    // Lấy Access Token
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    // Kiểm tra đăng nhập
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, 1L);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "Nguyễn Văn Tính");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "tinh@gmail.com");
    }

    public String getPhoneNumber() {
        return prefs.getString(KEY_PHONE_NUMBER, "");
    }

    public String getAvatarUrl() {
        return prefs.getString(KEY_AVATAR_URL, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "USER");
    }

    public void setRole(String role) {
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    // Dark Mode
    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean isDark) {
        editor.putBoolean(KEY_DARK_MODE, isDark);
        editor.apply();

        AppCompatDelegate.setDefaultNightMode(
                isDark
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // Nhắc lịch tiêm trước 7 ngày
    public boolean isAlert7DaysEnabled() {
        return prefs.getBoolean(KEY_ALERT_7_DAYS, true);
    }

    public void setAlert7DaysEnabled(boolean enabled) {
        editor.putBoolean(KEY_ALERT_7_DAYS, enabled);
        editor.apply();
    }

    // Đăng xuất
    public void logout() {
        boolean darkMode = isDarkMode();
        boolean alert7Days = isAlert7DaysEnabled();

        // Xóa thông tin phiên đăng nhập
        editor.clear();

        // Xóa token khỏi Retrofit
        RetrofitClient.setAccessToken(null);

        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.putBoolean(KEY_DARK_MODE, darkMode);
        editor.putBoolean(KEY_ALERT_7_DAYS, alert7Days);

        editor.apply();

        Log.d("AUTH_DEBUG", "User logged out. Access Token cleared.");
    }
}
