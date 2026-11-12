
package com.example.healthy_pet.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Quản lý phiên đăng nhập và cấu hình hệ thống.
 * Sử dụng SharedPreferences để lưu thông tin người dùng.
 * Phục vụ NV1, NV3 và Menu trượt.
 */
public class SessionManager {

    private static final String PREF_NAME = "HealthyPetPrefs";

    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_ALERT_7_DAYS = "alert_7_days";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    // 1. Lưu thông tin người dùng sau khi đăng nhập
    public void saveUserSession(
            Long userId,
            String fullName,
            String email,
            String role
    ) {
        SharedPreferences.Editor editor = prefs.edit();

        if (userId != null && userId > 0) {
            editor.putLong(KEY_USER_ID, userId);
        } else {
            editor.remove(KEY_USER_ID);
        }

        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(
                KEY_ROLE,
                role != null ? role : "USER"
        );

        editor.apply();
    }

    // 2. Lấy ID người dùng
    // Trả về null nếu chưa đăng nhập hợp lệ
    public Long getUserId() {

        if (!prefs.contains(KEY_USER_ID)) {
            return null;
        }

        long userId = prefs.getLong(KEY_USER_ID, 0L);

        return userId > 0 ? userId : null;
    }

    // 3. Lấy họ tên
    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    // 4. Lấy email
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    // 5. Lấy quyền người dùng
    public String getRole() {
        return prefs.getString(KEY_ROLE, "USER");
    }

    public void setRole(String role) {
        prefs.edit()
                .putString(KEY_ROLE, role)
                .apply();
    }

    // 6. Kiểm tra chế độ tối
    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    // 7. Bật hoặc tắt chế độ tối
    public void setDarkMode(boolean isDark) {

        prefs.edit()
                .putBoolean(KEY_DARK_MODE, isDark)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                isDark
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // 8. Kiểm tra trạng thái cảnh báo 7 ngày
    public boolean isAlert7DaysEnabled() {
        return prefs.getBoolean(KEY_ALERT_7_DAYS, true);
    }

    // 9. Bật hoặc tắt cảnh báo 7 ngày
    public void setAlert7DaysEnabled(boolean enabled) {

        prefs.edit()
                .putBoolean(KEY_ALERT_7_DAYS, enabled)
                .apply();
    }

    // 10. Đăng xuất
    public void logout() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_FULL_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_ROLE)
                .apply();
    }
}
