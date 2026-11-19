package com.example.healthy_pet.ui.admin;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.UserAdminAdapter;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Luồng ADMIN] Nghiệp vụ 6a, 7, 8 dành cho Quản trị viên hệ thống.
 * - Luồng 6 (NV 6a - Thành viên 1): Quản lý danh sách Khách hàng (manage_user_admin.xml), tìm kiếm & Khóa/Mở khóa tài khoản.
 *   (Chuyên mục Thú cưng NV 6b đã tách riêng sang Manage_Pet_Admin).
 */
public class AdminDashboardFragment extends Fragment {

    private static final String ARG_FLOW = "admin_flow";

    private TextInputEditText etSearchUserKeyword;
    private TextView tvAdminUsersCount;
    private ProgressBar progressBarAdminUsers;
    private TextView tvEmptyAdminUsers;
    private MaterialButton btnFilterAllUsers;
    private MaterialButton btnFilterActiveUsers;
    private MaterialButton btnFilterLockedUsers;
    private UserAdminAdapter userAdminAdapter;
    private Boolean currentActiveFilter = null; // null = Tất cả, true = Đang hoạt động, false = Đã khóa

    public static AdminDashboardFragment newInstance(int flowNumber) {
        AdminDashboardFragment fragment = new AdminDashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FLOW, flowNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;

        // Luồng 6: Giao diện Quản lý Users thật dành cho Admin (manage_user_admin.xml)
        if (flow == 6) {
            return setupManageUsersView(inflater, container);
        }

        // Luồng 7 & 8 (của Thành viên 4 & 5): Giữ nguyên khung hiện tại
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);
        TextView badge = view.findViewById(R.id.tvModuleBadge);
        TextView title = view.findViewById(R.id.tvModuleTitle);
        TextView desc = view.findViewById(R.id.tvModuleDescription);
        TextView tech = view.findViewById(R.id.tvModuleTechInfo);
        MaterialButton btn = view.findViewById(R.id.btnModuleAction);

        if (flow == 7) {
            badge.setText("NGHIỆP VỤ 7 (ADMIN) • THÀNH VIÊN 5");
            title.setText("🩺 Vận hành Lịch hẹn & Kê đơn Bệnh án");
            desc.setText(
                    "• Duyệt lịch hẹn của User: Xác nhận (CONFIRMED), Từ chối (CANCELLED) hoặc Hoàn thành (COMPLETED).\n" +
                    "• Khi bấm Hoàn thành: Nhập triệu chứng, chẩn đoán, đơn thuốc, chi phí DECIMAL(18, 0) & ảnh toa thuốc đẩy thẳng sang MedicalHistories."
            );
            tech.setText("Thực thể: Appointments, MedicalHistories\nAPI: AppointmentMedicalApi\n• PUT  /api/admin/appointments/{id}/status\n• POST /api/admin/appointments/{id}/complete (@Multipart)");
            btn.setText("🩺 Hoàn thành ca khám & Đẩy sang Bệnh án");
        } else {
            badge.setText("NGHIỆP VỤ 8 (ADMIN) • THÀNH VIÊN 4");
            title.setText("🏥 Quản lý Hệ thống Chi nhánh Phòng khám");
            desc.setText(
                    "• Thêm mới, chỉnh sửa hoặc xóa các chi nhánh phòng khám (VetClinics).\n" +
                    "• Cập nhật tọa độ chính xác (latitude, longitude) và ảnh cơ sở để đồng bộ điểm ghim lên Google Maps."
            );
            tech.setText("Thực thể: VetClinics\nAPI: ClinicMapApi\n• POST/PUT/DELETE /api/admin/clinics (@Multipart)");
            btn.setText("📍 Thêm Chi nhánh & Đồng bộ Tọa độ Maps");
        }

        btn.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đang thực thi thao tác Quản trị viên (NV " + flow + ")", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private View setupManageUsersView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        View view = inflater.inflate(R.layout.manage_user_admin, container, false);

        etSearchUserKeyword = view.findViewById(R.id.etSearchUserKeyword);
        MaterialButton btnSearchUser = view.findViewById(R.id.btnSearchUser);
        btnFilterAllUsers = view.findViewById(R.id.btnFilterAllUsers);
        btnFilterActiveUsers = view.findViewById(R.id.btnFilterActiveUsers);
        btnFilterLockedUsers = view.findViewById(R.id.btnFilterLockedUsers);
        tvAdminUsersCount = view.findViewById(R.id.tvAdminUsersCount);
        progressBarAdminUsers = view.findViewById(R.id.progressBarAdminUsers);
        tvEmptyAdminUsers = view.findViewById(R.id.tvEmptyAdminUsers);
        RecyclerView rvAdminUsers = view.findViewById(R.id.rvAdminUsers);

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        userAdminAdapter = new UserAdminAdapter(this::handleToggleUserLock);
        rvAdminUsers.setAdapter(userAdminAdapter);

        // Sự kiện bấm nút Tìm hoặc nhấn Enter trên bàn phím
        btnSearchUser.setOnClickListener(v -> loadUsersFromServer());
        etSearchUserKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUsersFromServer();
                return true;
            }
            return false;
        });

        // Sự kiện 3 nút lọc trạng thái
        btnFilterAllUsers.setOnClickListener(v -> selectActiveFilter(null, btnFilterAllUsers));
        btnFilterActiveUsers.setOnClickListener(v -> selectActiveFilter(true, btnFilterActiveUsers));
        btnFilterLockedUsers.setOnClickListener(v -> selectActiveFilter(false, btnFilterLockedUsers));

        updateFilterButtonStyles(btnFilterAllUsers);
        // Tải danh sách khách hàng ngay khi mở trang
        loadUsersFromServer();

        return view;
    }

    private void selectActiveFilter(@Nullable Boolean activeFilter, MaterialButton activeButton) {
        currentActiveFilter = activeFilter;
        updateFilterButtonStyles(activeButton);
        loadUsersFromServer();
    }

    private void updateFilterButtonStyles(MaterialButton activeButton) {
        if (getContext() == null) return;
        MaterialButton[] buttons = {btnFilterAllUsers, btnFilterActiveUsers, btnFilterLockedUsers};
        int activeColor = ContextCompat.getColor(requireContext(), R.color.indigo_600);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.slate_500);
        int activeBg = Color.parseColor("#EEF2FF");

        for (MaterialButton button : buttons) {
            boolean isActive = (button == activeButton);
            button.setTextColor(isActive ? activeColor : inactiveColor);
            button.setStrokeColor(ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
            button.setBackgroundTintList(ColorStateList.valueOf(isActive ? activeBg : Color.TRANSPARENT));
        }
    }

    private void loadUsersFromServer() {
        String keyword = etSearchUserKeyword != null && etSearchUserKeyword.getText() != null
                ? etSearchUserKeyword.getText().toString().trim()
                : "";
        String queryKeyword = keyword.isEmpty() ? null : keyword;

        progressBarAdminUsers.setVisibility(View.VISIBLE);
        tvEmptyAdminUsers.setVisibility(View.GONE);

        RetrofitClient.getAuthUserApi()
                .searchUsers(queryKeyword, currentActiveFilter)
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                        if (!isAdded()) return;
                        progressBarAdminUsers.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<User> users = response.body();
                            userAdminAdapter.setUsers(users);
                            if (tvAdminUsersCount != null) {
                                tvAdminUsersCount.setText("👥 Danh sách khách hàng (" + users.size() + ")");
                            }
                            tvEmptyAdminUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            Toast.makeText(getContext(), "Không thể tải danh sách khách hàng (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBarAdminUsers.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleToggleUserLock(User user, int position) {
        if (user == null || user.getId() == null) return;

        progressBarAdminUsers.setVisibility(View.VISIBLE);
        RetrofitClient.getAuthUserApi()
                .toggleUserActive(user.getId())
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                        if (!isAdded()) return;
                        progressBarAdminUsers.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            User updated = response.body();
                            userAdminAdapter.updateUserAt(position, updated);
                            boolean activeNow = Boolean.TRUE.equals(updated.getIsActive());
                            String msg = activeNow
                                    ? "Đã mở khóa tài khoản " + updated.getFullName()
                                    : "Đã khóa tài khoản " + updated.getFullName();
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Không thể thay đổi trạng thái tài khoản!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBarAdminUsers.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi kết nối máy chủ: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
