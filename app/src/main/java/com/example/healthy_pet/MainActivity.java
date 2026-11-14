package com.example.healthy_pet;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.example.healthy_pet.ui.Page_Admin;
import com.example.healthy_pet.ui.Page_User;
import com.example.healthy_pet.ui.auth.Login_Admin_User;
import com.example.healthy_pet.utils.SessionManager;
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
    private TextView tvDrawerUserName;
    private TextView tvDrawerUserEmail;

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
        tvDrawerUserName = findViewById(R.id.tvDrawerUserName);
        tvDrawerUserEmail = findViewById(R.id.tvDrawerUserEmail);

        findViewById(R.id.btnCloseDrawer).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        setupNavigationDrawer();

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
            tvDrawerUserName.setText("🐾 " + sessionManager.getFullName());
            tvDrawerUserEmail.setText(sessionManager.getEmail() + " • " + sessionManager.getRole());
        } else {
            tvDrawerUserName.setText("🐾 Healthy Pet Menu");
            tvDrawerUserEmail.setText("Cài đặt • Theme • Trợ lý AI Thú Y");
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
