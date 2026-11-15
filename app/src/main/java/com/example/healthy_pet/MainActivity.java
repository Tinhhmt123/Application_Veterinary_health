
package com.example.healthy_pet;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.healthy_pet.ui.ai.AiPetFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.healthy_pet.ui.admin.AdminDashboardFragment;
import com.example.healthy_pet.ui.auth.AccountFragment;
import com.example.healthy_pet.ui.map.ClinicBookingFragment;
import com.example.healthy_pet.ui.medical.MedicalHistoryFragment;
import com.example.healthy_pet.ui.pet.PetWeightFragment;
import com.example.healthy_pet.ui.vaccine.VaccineAlertFragment;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private LinearLayout topAppBar;
    private LinearLayout bottomNavUser;
    private LinearLayout bottomNavAdmin;

    private TextView tvToolbarTitle;
    private TextView tvToolbarSubtitle;
    private TextView tvAppIconEmoji;

    private SessionManager sessionManager;

    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sessionManager = new SessionManager(this);

        // Áp dụng giao diện sáng/tối đã lưu
        AppCompatDelegate.setDefaultNightMode(
                sessionManager.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        // 1. Kiểm tra đăng nhập
//        if (!sessionManager.isLoggedIn()) {
//            Toast.makeText(
//                    this,
//                    "Vui lòng đăng nhập để sử dụng ứng dụng!",
//                    Toast.LENGTH_LONG
//            ).show();
//
//            finish();
//            return;
//        }
        sessionManager = new SessionManager(this);

// TẠM THỜI BỎ QUA ĐĂNG NHẬP ĐỂ TEST
// Sử dụng tài khoản test có userId = 2
        sessionManager.saveUserSession(
                2L,
                "Test User",
                "test@gmail.com",
                "USER"
        );

        // 2. Đọc quyền người dùng
        String role = sessionManager.getRole();

        isAdminMode = "ADMIN".equalsIgnoreCase(role);

        // 3. Ánh xạ giao diện
        drawerLayout = findViewById(R.id.drawerLayout);
        topAppBar = findViewById(R.id.topAppBar);
        bottomNavUser = findViewById(R.id.bottomNavUser);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle);
        tvAppIconEmoji = findViewById(R.id.tvAppIconEmoji);

        // 4. Cấu hình menu trượt
        setupDrawerButtons();
        setupNavigationDrawer();

        // 5. Nút chuyển đổi USER / ADMIN
        View btnSwitchRole = findViewById(R.id.btnSwitchRole);

        if (btnSwitchRole != null) {

            btnSwitchRole.setVisibility(
                    isAdminMode ? View.VISIBLE : View.GONE
            );

            btnSwitchRole.setOnClickListener(v -> {

                if (!"ADMIN".equalsIgnoreCase(sessionManager.getRole())) {
                    Toast.makeText(
                            this,
                            "Bạn không có quyền quản trị!",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                isAdminMode = !isAdminMode;
                updateRoleChrome();
            });
        }

        // 6. Cấu hình điều hướng USER
        setupUserNavigation();

        // 7. Cấu hình điều hướng ADMIN
        setupAdminNavigation();

        // 8. Mở màn hình mặc định
        if (savedInstanceState == null) {

            if (isAdminMode) {

                updateRoleChrome();

            } else {

                bottomNavAdmin.setVisibility(View.GONE);
                bottomNavUser.setVisibility(View.VISIBLE);

                highlightUserTab(R.id.navUserAlert);

                loadScreen(
                        new VaccineAlertFragment(),
                        "NV 3: Cảnh báo y tế 7 ngày & Sổ tiêm"
                );
            }
        }
    }

    // ==============================
    // DRAWER
    // ==============================

    private void setupDrawerButtons() {

        View btnAppIconMenu = findViewById(R.id.btnAppIconMenu);
        View layoutTitleClick = findViewById(R.id.layoutTitleClick);
        View btnCloseDrawer = findViewById(R.id.btnCloseDrawer);

        View.OnClickListener openDrawerListener =
                v -> drawerLayout.openDrawer(GravityCompat.START);

        btnAppIconMenu.setOnClickListener(openDrawerListener);
        layoutTitleClick.setOnClickListener(openDrawerListener);

        btnCloseDrawer.setOnClickListener(
                v -> drawerLayout.closeDrawer(GravityCompat.START)
        );
    }

    private void setupNavigationDrawer() {

        SwitchMaterial switchDarkMode =
                findViewById(R.id.switchDarkMode);

        SwitchMaterial switchAlert7Days =
                findViewById(R.id.switchAlert7Days);

        TextView tvAiAnswerBox =
                findViewById(R.id.tvAiAnswerBox);
        // Mở màn hình trò chuyện AI Thú Y
        View btnOpenAiChat = findViewById(R.id.btnOpenAiChat);

        btnOpenAiChat.setOnClickListener(v -> {

            Toast.makeText(
                    MainActivity.this,
                    "Đang mở AI Thú Y...",
                    Toast.LENGTH_SHORT
            ).show();

            // Hiển thị màn hình AI
            loadScreen(
                    new AiPetFragment(),
                    "Trợ lý AI Thú Y"
            );

            // Đóng Menu
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // ==============================
        // DARK MODE
        // ==============================

        switchDarkMode.setChecked(sessionManager.isDarkMode());

        switchDarkMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked != sessionManager.isDarkMode()) {
                        sessionManager.setDarkMode(isChecked);
                    }
                }
        );

        // ==============================
        // VACCINATION ALERT
        // ==============================

        switchAlert7Days.setChecked(
                sessionManager.isAlert7DaysEnabled()
        );

        switchAlert7Days.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    // Lưu trạng thái bật/tắt cảnh báo
                    sessionManager.setAlert7DaysEnabled(isChecked);

                    // Lấy Fragment hiện đang hiển thị
                    Fragment currentFragment =
                            getSupportFragmentManager()
                                    .findFragmentById(R.id.fragmentContainer);

                    // Nếu đang ở màn hình cảnh báo tiêm chủng
                    if (currentFragment instanceof VaccineAlertFragment) {

                        ((VaccineAlertFragment) currentFragment)
                                .refreshHealthAlerts();
                    }

                    Toast.makeText(
                            MainActivity.this,
                            isChecked
                                    ? "Đã bật cảnh báo tiêm chủng."
                                    : "Đã tắt cảnh báo tiêm chủng.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        // ==============================
        // AI FAQ 1
        // ==============================

        findViewById(R.id.btnAiFaq1).setOnClickListener(v ->
                tvAiAnswerBox.setText(
                        "🤖 AI Bác Sĩ Thú Y:\n" +
                                "Nôn dịch vàng thường do trào ngược dịch mật " +
                                "hoặc viêm dạ dày. Bạn nên theo dõi tình trạng " +
                                "của bé và đưa bé đi khám nếu có dấu hiệu bất thường."
                )
        );

        // ==============================
        // AI FAQ 2
        // ==============================

        findViewById(R.id.btnAiFaq2).setOnClickListener(v ->
                tvAiAnswerBox.setText(
                        "🤖 AI Bác Sĩ Thú Y:\n" +
                                "Lịch tiêm vaccine phụ thuộc vào độ tuổi, " +
                                "loại vaccine và tình trạng sức khỏe của thú cưng. " +
                                "Hãy tham khảo bác sĩ thú y để được tư vấn."
                )
        );
    }

    // ==============================
    // USER NAVIGATION
    // ==============================

    private void setupUserNavigation() {

        findViewById(R.id.navUserAlert).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserAlert);
            loadScreen(
                    new VaccineAlertFragment(),
                    "NV 3: Cảnh báo y tế 7 ngày & Sổ tiêm"
            );
        });

        findViewById(R.id.navUserPet).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserPet);
            loadScreen(
                    new PetWeightFragment(),
                    "NV 2: Hồ sơ thú cưng & Cân nặng"
            );
        });

        findViewById(R.id.navUserBookingCenter).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserBookingCenter);
            loadScreen(
                    new ClinicBookingFragment(),
                    "NV 5: Đặt lịch Khám/Spa & Bản đồ"
            );
        });

        findViewById(R.id.navUserMedical).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserMedical);
            loadScreen(
                    new MedicalHistoryFragment(),
                    "NV 4: Bệnh án & Chi phí"
            );
        });

        findViewById(R.id.navUserAccount).setOnClickListener(v -> {
            highlightUserTab(R.id.navUserAccount);
            loadScreen(
                    new AccountFragment(),
                    "NV 1: Quản lý tài khoản cá nhân"
            );
        });
    }

    // ==============================
    // ADMIN NAVIGATION
    // ==============================

    private void setupAdminNavigation() {

        findViewById(R.id.navAdminUserPet).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminUserPet);
            loadScreen(
                    AdminDashboardFragment.newInstance(6),
                    "NV 6: Quản lý Users & Pets toàn hệ thống"
            );
        });

        findViewById(R.id.navAdminAppointment).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminAppointment);
            loadScreen(
                    AdminDashboardFragment.newInstance(7),
                    "NV 7: Duyệt lịch hẹn & Kê đơn bệnh án"
            );
        });

        findViewById(R.id.navAdminClinic).setOnClickListener(v -> {
            highlightAdminTab(R.id.navAdminClinic);
            loadScreen(
                    AdminDashboardFragment.newInstance(8),
                    "NV 8: Quản lý Chi nhánh Phòng khám"
            );
        });
    }

    // ==============================
    // HIGHLIGHT USER TAB
    // ==============================

    private void highlightUserTab(int activeTabId) {

        int[] tabIds = {
                R.id.navUserAlert,
                R.id.navUserPet,
                R.id.navUserBookingCenter,
                R.id.navUserMedical,
                R.id.navUserAccount
        };

        int[] labelIds = {
                R.id.tvNavUserAlert,
                R.id.tvNavUserPet,
                R.id.tvNavUserBooking,
                R.id.tvNavUserMedical,
                R.id.tvNavUserAccount
        };

        int activeColor = ContextCompat.getColor(
                this, R.color.emerald_500
        );

        int inactiveColor = ContextCompat.getColor(
                this, R.color.slate_500
        );

        for (int i = 0; i < tabIds.length; i++) {

            View tabView = findViewById(tabIds[i]);
            TextView tvLabel = findViewById(labelIds[i]);

            boolean isSelected = tabIds[i] == activeTabId;

            if (tvLabel != null) {
                tvLabel.setTextColor(
                        isSelected ? activeColor : inactiveColor
                );

                tvLabel.setTypeface(
                        null,
                        isSelected ? Typeface.BOLD : Typeface.NORMAL
                );
            }

            if (tabView != null) {
                tabView.animate()
                        .scaleX(isSelected ? 1.08f : 1.0f)
                        .scaleY(isSelected ? 1.08f : 1.0f)
                        .alpha(
                                isSelected ||
                                        tabIds[i] == R.id.navUserBookingCenter
                                        ? 1.0f : 0.65f
                        )
                        .setDuration(160)
                        .start();
            }
        }
    }

    // ==============================
    // HIGHLIGHT ADMIN TAB
    // ==============================

    private void highlightAdminTab(int activeTabId) {

        int[] tabIds = {
                R.id.navAdminUserPet,
                R.id.navAdminAppointment,
                R.id.navAdminClinic
        };

        int[] labelIds = {
                R.id.tvNavAdminUserPet,
                R.id.tvNavAdminAppointment,
                R.id.tvNavAdminClinic
        };

        int activeColor = ContextCompat.getColor(
                this, R.color.emerald_500
        );

        int inactiveColor = ContextCompat.getColor(
                this, R.color.slate_500
        );

        for (int i = 0; i < tabIds.length; i++) {

            View tabView = findViewById(tabIds[i]);
            TextView tvLabel = findViewById(labelIds[i]);

            boolean isSelected = tabIds[i] == activeTabId;

            if (tvLabel != null) {
                tvLabel.setTextColor(
                        isSelected ? activeColor : inactiveColor
                );

                tvLabel.setTypeface(
                        null,
                        isSelected ? Typeface.BOLD : Typeface.NORMAL
                );
            }

            if (tabView != null) {
                tabView.animate()
                        .scaleX(isSelected ? 1.08f : 1.0f)
                        .scaleY(isSelected ? 1.08f : 1.0f)
                        .alpha(isSelected ? 1.0f : 0.65f)
                        .setDuration(160)
                        .start();
            }
        }
    }

    // ==============================
    // CHANGE ROLE INTERFACE
    // ==============================

    private void updateRoleChrome() {

        if (isAdminMode) {

            topAppBar.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.indigo_600)
            );

            tvAppIconEmoji.setText("🛡️");
            tvToolbarTitle.setText("Healthy Pet Admin (Menu ≡)");

            bottomNavUser.setVisibility(View.GONE);
            bottomNavAdmin.setVisibility(View.VISIBLE);

            highlightAdminTab(R.id.navAdminUserPet);

            loadScreen(
                    AdminDashboardFragment.newInstance(6),
                    "NV 6: Quản lý Users & Pets toàn hệ thống"
            );

        } else {

            topAppBar.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.emerald_600)
            );

            tvAppIconEmoji.setText("🐾");
            tvToolbarTitle.setText("Healthy Pet (Menu ≡)");

            bottomNavAdmin.setVisibility(View.GONE);
            bottomNavUser.setVisibility(View.VISIBLE);

            highlightUserTab(R.id.navUserAlert);

            loadScreen(
                    new VaccineAlertFragment(),
                    "NV 3: Cảnh báo y tế 7 ngày & Sổ tiêm"
            );
        }
    }

    // ==============================
    // LOAD FRAGMENT
    // ==============================

    private void loadScreen(Fragment fragment, String subtitle) {

        tvToolbarSubtitle.setText(subtitle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
