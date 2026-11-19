package com.example.healthy_pet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.ui.Page_Admin;
import com.example.healthy_pet.ui.Page_User;
import com.example.healthy_pet.ui.auth.Login_Admin_User;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * MainActivity điều phối:
 * - Nếu chưa đăng nhập -> Mở màn hình Login_Admin_User
 * - Nếu đăng nhập với role = "USER" -> Mở trang Page_User (page_user.xml)
 * - Nếu đăng nhập với role = "ADMIN" -> Mở trang Page_Admin (page_admin.xml)
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private LinearLayout layoutDrawerHeader;
    private ImageView ivDrawerUserAvatar;
    private TextView tvDrawerUserName;
    private TextView tvDrawerUserEmail;
    private MaterialButton btnDrawerLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sessionManager = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(
                sessionManager.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        layoutDrawerHeader = findViewById(R.id.layoutDrawerHeader);
        ivDrawerUserAvatar = findViewById(R.id.ivDrawerUserAvatar);
        tvDrawerUserName = findViewById(R.id.tvDrawerUserName);
        tvDrawerUserEmail = findViewById(R.id.tvDrawerUserEmail);
        btnDrawerLogout = findViewById(R.id.btnDrawerLogout);

        findViewById(R.id.btnCloseDrawer).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        btnDrawerLogout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            sessionManager.logout();
            Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();
            openLoginPage();
        });

        setupNavigationDrawer();
        syncDrawerUserHeader();

        if (savedInstanceState == null) {
            if (!sessionManager.isLoggedIn()) {
                openLoginPage();
            } else {
                openRolePage(sessionManager.getRole());
            }
        }
    }

    /**
     * Kiểm tra quyền của tài khoản để chuyển vào đúng trang page_user hoặc page_admin
     */
    public void openRolePage(String role) {
        syncDrawerUserHeader();
        Fragment targetPage;
        if ("ADMIN".equalsIgnoreCase(role)) {
            targetPage = new Page_Admin();
        } else {
            targetPage = new Page_User();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, targetPage)
                .commit();
    }

    /**
     * Mở trang Đăng nhập (Login_Admin_User) khi chưa đăng nhập hoặc vừa bấm Đăng xuất
     */
    public void openLoginPage() {
        syncDrawerUserHeader();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new Login_Admin_User())
                .commit();
    }

    public void openDrawer() {
        syncDrawerUserHeader();
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void syncDrawerUserHeader() {
        if (tvDrawerUserName == null || tvDrawerUserEmail == null) return;

        if (sessionManager.isLoggedIn()) {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole());
            tvDrawerUserName.setText(sessionManager.getFullName());
            tvDrawerUserEmail.setText(sessionManager.getEmail() + (isAdmin ? " • Quản trị viên" : " • Khách hàng"));

            if (layoutDrawerHeader != null) {
                int headerColor = ContextCompat.getColor(
                        this,
                        isAdmin ? R.color.indigo_600 : R.color.emerald_700
                );
                layoutDrawerHeader.setBackgroundColor(headerColor);
            }

            if (ivDrawerUserAvatar != null) {
                String avatarUrl = sessionManager.getAvatarUrl();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    ImageMultipartUtils.loadCircularAvatar(this, avatarUrl, ivDrawerUserAvatar);
                } else {
                    ivDrawerUserAvatar.setImageResource(R.mipmap.ic_launcher_round);
                }
            }

            if (btnDrawerLogout != null) {
                btnDrawerLogout.setVisibility(View.VISIBLE);
            }
        } else {
            tvDrawerUserName.setText("Healthy Pet");
            tvDrawerUserEmail.setText("Cài đặt • Theme • Trợ lý AI");
            if (layoutDrawerHeader != null) {
                layoutDrawerHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.emerald_700));
            }
            if (ivDrawerUserAvatar != null) {
                ivDrawerUserAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
            if (btnDrawerLogout != null) {
                btnDrawerLogout.setVisibility(View.GONE);
            }
        }
    }

    private void setupNavigationDrawer() {
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchAlert7Days = findViewById(R.id.switchAlert7Days);
        TextView tvAiAnswerBox = findViewById(R.id.tvAiAnswerBox);

        switchDarkMode.setChecked(sessionManager.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked != sessionManager.isDarkMode()) {
                sessionManager.setDarkMode(isChecked);
            }
        });

        switchAlert7Days.setChecked(sessionManager.isAlert7DaysEnabled());
        switchAlert7Days.setOnCheckedChangeListener((buttonView, isChecked) ->
                sessionManager.setAlert7DaysEnabled(isChecked)
        );

        findViewById(R.id.btnAiFaq1).setOnClickListener(v ->
                tvAiAnswerBox.setText("🤖 AI Bác Sĩ Thú Y:\nNôn dịch vàng thường do trào ngược dịch mật hoặc viêm dạ dày. Bạn nên cho bé nhịn ăn 4-6h, uống nước điện giải ít một và đặt lịch khám ngay nếu kèm tiêu chảy!")
        );

        findViewById(R.id.btnAiFaq2).setOnClickListener(v ->
                tvAiAnswerBox.setText("🤖 AI Bác Sĩ Thú Y:\nMũi 1 từ 6-8 tuần tuổi, Mũi 2 & 3 cách nhau 3-4 tuần + Tiêm Dại khi đủ 3 tháng. Sau đó tiêm nhắc lại 1 năm/lần.")
        );
    }
}