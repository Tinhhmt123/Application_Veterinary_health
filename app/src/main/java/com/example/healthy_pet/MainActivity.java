package com.example.healthy_pet;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
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

/**
 * MainActivity điều phối 8 Luồng Nghiệp Vụ (5 User + 3 Admin)
 * và Thanh Menu Trượt từ trái sang (DrawerLayout Overlay: Theme, Cài đặt, Trợ lý AI).
 */
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
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        topAppBar = findViewById(R.id.topAppBar);
        bottomNavUser = findViewById(R.id.bottomNavUser);
        bottomNavAdmin = findViewById(R.id.bottomNavAdmin);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle);
        tvAppIconEmoji = findViewById(R.id.tvAppIconEmoji);

        // 1. Bấm biểu tượng App (🐾) để trượt Menu từ trái sang (Overlay, không ảnh hưởng UI bên dưới)
        View.OnClickListener openDrawerListener = v -> drawerLayout.openDrawer(GravityCompat.START);
        findViewById(R.id.btnAppIconMenu).setOnClickListener(openDrawerListener);
        findViewById(R.id.layoutTitleClick).setOnClickListener(openDrawerListener);
        findViewById(R.id.btnCloseDrawer).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        // 2. Cấu hình bên trong Menu trượt (Theme Sáng/Tối, Cài đặt 7 ngày, Trợ lý AI)
        setupNavigationDrawer();

        // 3. Nút chuyển đổi nhanh giữa Luồng USER (NV 1->5) và Luồng ADMIN (NV 6->8)
        findViewById(R.id.btnSwitchRole).setOnClickListener(v -> {
            isAdminMode = !isAdminMode;
            updateRoleChrome();
        });

        // 4. Sự kiện chọn 5 mục trên Bottom Navigation của USER
        findViewById(R.id.navUserAlert).setOnClickListener(v ->
                loadScreen(new VaccineAlertFragment(), "NV 3: Cảnh báo y tế 7 ngày & Sổ tiêm"));
        findViewById(R.id.navUserPet).setOnClickListener(v ->
                loadScreen(new PetWeightFragment(), "NV 2: Hồ sơ thú cưng & Cân nặng"));
        findViewById(R.id.navUserBookingCenter).setOnClickListener(v ->
                loadScreen(new ClinicBookingFragment(), "NV 5: Đặt lịch Khám/Spa & Bản đồ"));
        findViewById(R.id.navUserMedical).setOnClickListener(v ->
                loadScreen(new MedicalHistoryFragment(), "NV 4: Bệnh án & Chi phí DECIMAL"));
        findViewById(R.id.navUserAccount).setOnClickListener(v ->
                loadScreen(new AccountFragment(), "NV 1: Quản lý tài khoản cá nhân"));

        // 5. Sự kiện chọn 3 mục trên Bottom Navigation của ADMIN
        findViewById(R.id.navAdminUserPet).setOnClickListener(v ->
                loadScreen(AdminDashboardFragment.newInstance(6), "NV 6: Quản lý Users & Pets toàn hệ thống"));
        findViewById(R.id.navAdminAppointment).setOnClickListener(v ->
                loadScreen(AdminDashboardFragment.newInstance(7), "NV 7: Duyệt lịch hẹn & Kê đơn bệnh án"));
        findViewById(R.id.navAdminClinic).setOnClickListener(v ->
                loadScreen(AdminDashboardFragment.newInstance(8), "NV 8: Quản lý Chi nhánh Phòng khám"));

        // Mặc định mở màn hình Cảnh báo & Sổ tiêm (NV 3) khi khởi động
        if (savedInstanceState == null) {
            loadScreen(new VaccineAlertFragment(), "NV 3: Cảnh báo y tế 7 ngày & Sổ tiêm");
        }
    }

    private void setupNavigationDrawer() {
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchAlert7Days = findViewById(R.id.switchAlert7Days);
        TextView tvAiAnswerBox = findViewById(R.id.tvAiAnswerBox);

        switchDarkMode.setChecked(sessionManager.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                sessionManager.setDarkMode(isChecked)
        );

        switchAlert7Days.setChecked(sessionManager.isAlert7DaysEnabled());
        switchAlert7Days.setOnCheckedChangeListener((buttonView, isChecked) ->
                sessionManager.setAlert7DaysEnabled(isChecked)
        );

        findViewById(R.id.btnAiFaq1).setOnClickListener(v ->
                tvAiAnswerBox.setText("🤖 AI Bác Sĩ Thú Y:\nNôn dịch vàng thường do trào ngược dịch mật hoặc viêm dạ dày. Bạn nên cho bé nhịn ăn 4-6h, uống nước điện giải ít một và bấm nút 'Đặt lịch' ở giữa thanh menu dưới để khám ngay nếu kèm tiêu chảy!")
        );

        findViewById(R.id.btnAiFaq2).setOnClickListener(v ->
                tvAiAnswerBox.setText("🤖 AI Bác Sĩ Thú Y:\nMũi 1 từ 6-8 tuần tuổi, Mũi 2 & 3 cách nhau 3-4 tuần + Tiêm Dại khi đủ 3 tháng. Sau đó tiêm nhắc lại 1 năm/lần (Hệ thống sẽ tự động báo vàng trước 7 ngày ở NV 3).")
        );
    }

    private void updateRoleChrome() {
        if (isAdminMode) {
            topAppBar.setBackgroundColor(ContextCompat.getColor(this, R.color.indigo_600));
            tvAppIconEmoji.setText("🛡️");
            tvToolbarTitle.setText("Healthy Pet Admin (Menu ≡)");
            bottomNavUser.setVisibility(View.GONE);
            bottomNavAdmin.setVisibility(View.VISIBLE);
            loadScreen(AdminDashboardFragment.newInstance(6), "NV 6: Quản lý Users & Pets toàn hệ thống");
        } else {
            topAppBar.setBackgroundColor(ContextCompat.getColor(this, R.color.emerald_600));
            tvAppIconEmoji.setText("🐾");
            tvToolbarTitle.setText("Healthy Pet (Menu ≡)");
            bottomNavAdmin.setVisibility(View.GONE);
            bottomNavUser.setVisibility(View.VISIBLE);
            loadScreen(new VaccineAlertFragment(), "NV 3: Cảnh báo y tế 7 ngày & Sổ tiêm");
        }
    }

    private void loadScreen(Fragment fragment, String subtitle) {
        tvToolbarSubtitle.setText(subtitle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
