package com.example.healthy_pet;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.healthy_pet.ui.Page_Admin;
import com.example.healthy_pet.ui.Page_User;
import com.example.healthy_pet.ui.auth.Login_Admin_User;
import com.example.healthy_pet.ui.vaccine.VaccineAlertFragment;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.example.healthy_pet.ui.ai.AiPetFragment;

import com.example.healthy_pet.repository.AppointmentRepository;
import com.example.healthy_pet.repository.MedicalHistoryRepository;

/**
 * Activity gốc của Healthy Pet.
 * activity_main.xml chỉ cung cấp DrawerLayout, fragmentContainer và các view cài đặt
 * trong drawer. Header/footer điều hướng USER/ADMIN nằm trong Page_User/Page_Admin,
 * vì vậy MainActivity không ánh xạ các ID topAppBar hoặc bottomNavUser/Admin.
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private boolean isAdminMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        sessionManager = new SessionManager(this);
        AppointmentRepository.getInstance().initContext(this);
        MedicalHistoryRepository.getInstance().initContext(this);
        AppCompatDelegate.setDefaultNightMode(
                sessionManager.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        if (drawerLayout == null || findViewById(R.id.fragmentContainer) == null) {
            Toast.makeText(this,
                    "activity_main.xml thiếu drawerLayout hoặc fragmentContainer.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupDrawerSettings();
        setupDrawerCloseButton();
        setupLogoutButton();
        setupDrawerNavigationItems();
        setupAiChatButton(); // Thêm dòng này
        syncDrawerUserHeader();


        if (savedInstanceState == null) {
            if (sessionManager.isLoggedIn()) {
                openRolePage(sessionManager.getRole());
            } else {
                openLoginPage();
            }
        }
    }

    /** Được Login_Admin_User gọi sau khi đăng nhập thành công. */
    public void openRolePage(@Nullable String role) {
        isAdminMode = "ADMIN".equalsIgnoreCase(role);
        syncDrawerUserHeader();

        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        Fragment page = isAdminMode ? new Page_Admin() : new Page_User();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, page)
                .commit();
    }

    /** Được AccountFragment gọi sau khi đăng xuất hoặc khi phiên không còn hợp lệ. */
    public void openLoginPage() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new Login_Admin_User())
                .commit();
    }

    /** Được Page_User và Page_Admin gọi khi người dùng chạm nút mở menu. */
    public void openDrawer() {
        if (drawerLayout != null
                && sessionManager != null
                && sessionManager.isLoggedIn()) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    /** Đồng bộ tên, email, vai trò và ảnh đại diện trong header menu. */
    public void syncDrawerUserHeader() {
        if (sessionManager == null) return;

        TextView tvName = findViewById(R.id.tvDrawerUserName);
        TextView tvEmail = findViewById(R.id.tvDrawerUserEmail);
        ImageView ivAvatar = findViewById(R.id.ivDrawerUserAvatar);

        String fullName = sessionManager.getFullName();
        String email = sessionManager.getEmail();
        String role = sessionManager.getRole();
        isAdminMode = "ADMIN".equalsIgnoreCase(role);

        if (tvName != null) {
            tvName.setText(fullName == null || fullName.trim().isEmpty()
                    ? "Healthy Pet" : fullName);
        }
        if (tvEmail != null) {
            String roleLabel = isAdminMode ? "Quản trị viên" : "Khách hàng";
            tvEmail.setText((email == null ? "" : email) + " • " + roleLabel);
        }
        if (ivAvatar != null) {
            String avatarUrl = sessionManager.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .transform(new CircleCrop())
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        }
    }

    private void setupDrawerSettings() {
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchAlert7Days = findViewById(R.id.switchAlert7Days);
        TextView tvAiAnswerBox = findViewById(R.id.tvAiAnswerBox);

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(sessionManager.isDarkMode());
            switchDarkMode.setOnCheckedChangeListener((button, checked) -> {
                if (checked != sessionManager.isDarkMode()) {
                    sessionManager.setDarkMode(checked);
                    AppCompatDelegate.setDefaultNightMode(
                            checked ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO
                    );
                }
            });
        }

        if (switchAlert7Days != null) {
            switchAlert7Days.setOnCheckedChangeListener(null);
            switchAlert7Days.setChecked(sessionManager.isAlert7DaysEnabled());
            switchAlert7Days.setOnCheckedChangeListener((button, checked) -> {
                sessionManager.setAlert7DaysEnabled(checked);
                Fragment currentRoot = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (currentRoot instanceof Page_User) {
                    Fragment centerFrag = currentRoot.getChildFragmentManager().findFragmentById(R.id.userContentContainer);
                    if (centerFrag instanceof VaccineAlertFragment) {
                        ((VaccineAlertFragment) centerFrag).refreshHealthAlerts();
                    }
                }
                Toast.makeText(this,
                        checked ? "Đã bật cảnh báo tiêm chủng trước 7 ngày."
                                : "Đã tắt cảnh báo tiêm chủng trước 7 ngày.",
                        Toast.LENGTH_SHORT).show();
            });
        }

        View faq1 = findViewById(R.id.btnAiFaq1);
        if (faq1 != null && tvAiAnswerBox != null) {
            faq1.setOnClickListener(v -> tvAiAnswerBox.setText(
                    "🤖 AI Bác Sĩ Thú Y:\nNôn dịch vàng có thể liên quan đến dạ dày hoặc dịch mật. "
                            + "Hãy theo dõi bé và đưa bé đi khám nếu tình trạng kéo dài hoặc có dấu hiệu bất thường."));
        }

        View faq2 = findViewById(R.id.btnAiFaq2);
        if (faq2 != null && tvAiAnswerBox != null) {
            faq2.setOnClickListener(v -> tvAiAnswerBox.setText(
                    "🤖 AI Bác Sĩ Thú Y:\nLịch tiêm phụ thuộc vào tuổi, loại vaccine và sức khỏe của thú cưng. "
                            + "Bạn nên xác nhận lịch cụ thể với bác sĩ thú y."));
        }
    }

    /** Thêm lối tắt lịch sử tiêm chủng vào drawer với thẻ nổi bật và tương phản ở cả Light/Dark Mode. */
    private void setupDrawerNavigationItems() {
        ScrollView scrollView = findDrawerScrollView(drawerLayout);
        if (scrollView == null || scrollView.getChildCount() == 0) return;

        View firstChild = scrollView.getChildAt(0);
        if (!(firstChild instanceof LinearLayout)) return;

        LinearLayout drawerContent = (LinearLayout) firstChild;
        View existingSection = drawerContent.findViewWithTag("vaccination_history_drawer_section");
        if (existingSection != null) {
            drawerContent.removeView(existingSection);
        }

        boolean isDark = sessionManager != null && sessionManager.isDarkMode();

        LinearLayout section = new LinearLayout(this);
        section.setTag("vaccination_history_drawer_section");
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(14), dp(14), dp(14), dp(0));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor(isDark ? "#1E293B" : "#FFFFFF"));
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), Color.parseColor("#3310B981"));
        card.setBackground(cardBg);

        TextView heading = new TextView(this);
        heading.setText("📋 SỔ & LỊCH SỬ TIÊM CHỦNG");
        heading.setTextColor(Color.parseColor("#10B981"));
        heading.setTextSize(12);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setPadding(dp(2), dp(0), dp(2), dp(8));
        card.addView(heading);

        TextView historyItem = new TextView(this);
        historyItem.setTag("vaccination_history_drawer_item");
        historyItem.setText("💉  Xem Lịch sử tiêm chủng   →");
        historyItem.setTextColor(Color.parseColor(isDark ? "#ECFDF5" : "#065F46"));
        historyItem.setTextSize(15);
        historyItem.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        historyItem.setGravity(Gravity.CENTER_VERTICAL);
        historyItem.setPadding(dp(14), dp(12), dp(14), dp(12));

        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(Color.parseColor(isDark ? "#064E3B" : "#ECFDF5"));
        itemBg.setCornerRadius(dp(12));
        itemBg.setStroke(dp(1), Color.parseColor("#10B981"));
        historyItem.setBackground(itemBg);

        historyItem.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                openLoginPage();
                return;
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            v.postDelayed(() -> getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, VaccineAlertFragment.newHistoryInstance())
                    .addToBackStack("vaccination_history")
                    .commit(), 180);
        });
        card.addView(historyItem);
        section.addView(card);

        // Header tài khoản là phần tử đầu tiên trong XML.
        int insertIndex = Math.min(1, drawerContent.getChildCount());
        drawerContent.addView(section, insertIndex);
    }

    private void setupDrawerCloseButton() {
        View close = findViewById(R.id.btnCloseDrawer);
        if (close != null) {
            close.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        }
    }

    private void setupLogoutButton() {
        View logout = findViewById(R.id.btnDrawerLogout);
        if (logout != null) {
            logout.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        sessionManager.logout();
                        openLoginPage();
                    })
                    .show());
        }
    }

    @Nullable
    private ScrollView findDrawerScrollView(View view) {
        if (view instanceof ScrollView) return (ScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView result = findDrawerScrollView(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private GradientDrawable makeSelectableBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }
    private void setupAiChatButton() {
        View btnOpenAiChat = findViewById(R.id.btnOpenAiChat);

        if (btnOpenAiChat == null) {
            Toast.makeText(
                    this,
                    "Không tìm thấy nút mở Chat AI!",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        btnOpenAiChat.setOnClickListener(v -> {

            if (!sessionManager.isLoggedIn()) {
                openLoginPage();
                return;
            }

            // Đóng menu trước khi mở giao diện Chat AI
            drawerLayout.closeDrawer(GravityCompat.START);

            // Chờ menu đóng rồi chuyển sang AiPetFragment
            v.postDelayed(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragmentContainer,
                                new AiPetFragment(),
                                "AI_PET_FRAGMENT"
                        )
                        .addToBackStack("ai_pet")
                        .commit();

            }, 200);
        });
    }
}