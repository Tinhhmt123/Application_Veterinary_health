package com.example.healthy_pet.ui.admin;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.AppointmentAdapter;
import com.example.healthy_pet.adapters.ClinicAdapter;
import com.example.healthy_pet.adapters.UserAdminAdapter;
import com.example.healthy_pet.api.AppointmentMedicalApi;
import com.example.healthy_pet.api.ClinicMapApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.databinding.DialogPrescriptionBinding;
import com.example.healthy_pet.models.Appointment;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.models.User;
import com.example.healthy_pet.models.VetClinic;
import com.example.healthy_pet.repository.AppointmentRepository;
import com.example.healthy_pet.repository.MedicalHistoryRepository;
import com.example.healthy_pet.utils.CurrencyUtils;
import com.example.healthy_pet.utils.ImageMultipartUtils;
import com.example.healthy_pet.utils.InfoHintHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Luồng ADMIN] Quản trị hệ thống:
 * - Luồng 6: Quản lý Khách hàng (manage_user_admin.xml), tìm kiếm & Khóa/Mở khóa tài khoản.
 * - Luồng 7: Duyệt lịch & Kê đơn ca khám.
 * - Luồng 8: Quản lý Chi nhánh Phòng khám trực tiếp ngay trong tab kèm chọn ảnh cơ sở.
 */
public class AdminDashboardFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener, ClinicAdapter.OnClinicActionListener {

    private static final String ARG_FLOW = "admin_flow";
    private final List<Appointment> adminAppointments = new ArrayList<>();
    private AppointmentAdapter adapter;
    private AppointmentMedicalApi appointmentApi;
    private boolean showingApprovalHistory = false;
    private TextView tvAppointmentListHeader;
    private MaterialButton btnAppointmentToggleHistory;

    private final List<VetClinic> adminClinics = new ArrayList<>();
    private ClinicAdapter clinicAdapter;
    private ClinicMapApi clinicApi;
    private TextView tvClinicListHeader;
    private Uri selectedClinicImageUri = null;
    private ImageView ivDialogClinicPreview = null;
    private Uri selectedPrescriptionImageUri = null;
    private ImageView ivDialogPrescriptionPreview = null;

    private final ActivityResultLauncher<String> pickClinicImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && isAdded()) {
                    selectedClinicImageUri = uri;
                    if (ivDialogClinicPreview != null) {
                        Glide.with(requireContext())
                                .load(uri)
                                .placeholder(R.drawable.ic_clinic_building)
                                .into(ivDialogClinicPreview);
                    }
                }
            });

    private final ActivityResultLauncher<String> pickPrescriptionImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && isAdded()) {
                    selectedPrescriptionImageUri = uri;
                    if (ivDialogPrescriptionPreview != null) {
                        ivDialogPrescriptionPreview.setVisibility(View.VISIBLE);
                        Glide.with(requireContext())
                                .load(uri)
                                .into(ivDialogPrescriptionPreview);
                    }
                }
            });

    private TextInputEditText etSearchUserKeyword;
    private TextView tvAdminUsersCount;
    private ProgressBar progressBarAdminUsers;
    private TextView tvEmptyAdminUsers;
    private MaterialButton btnFilterAllUsers;
    private MaterialButton btnFilterActiveUsers;
    private MaterialButton btnFilterLockedUsers;
    private UserAdminAdapter userAdminAdapter;
    private Boolean currentActiveFilter = null;

    public static AdminDashboardFragment newInstance(int flowNumber) {
        AdminDashboardFragment fragment = new AdminDashboardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FLOW, flowNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appointmentApi = RetrofitClient.getAppointmentMedicalApi();
        clinicApi = RetrofitClient.getClinicMapApi();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;

        if (flow == 6) {
            return setupManageUsersView(inflater, container);
        }

        View view = inflater.inflate(R.layout.fragment_module_card, container, false);
        TextView badge = view.findViewById(R.id.tvModuleBadge);
        TextView techInfo = view.findViewById(R.id.tvModuleTechInfo);
        TextView title = view.findViewById(R.id.tvModuleTitle);
        TextView desc = view.findViewById(R.id.tvModuleDescription);
        View btnInfo = view.findViewById(R.id.btnModuleInfo);
        MaterialButton btn = view.findViewById(R.id.btnModuleAction);

        if (badge != null) badge.setVisibility(View.GONE);
        if (techInfo != null) techInfo.setVisibility(View.GONE);

        boolean isNight = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int sectionTitleColor = Color.parseColor(isNight ? "#F8FAFC" : "#0F172A");

        if (flow == 7) {
            showingApprovalHistory = false;
            title.setText("🩺 Quản Lý & Duyệt Lịch Khám");
            desc.setText(
                    "Theo dõi các lịch hẹn khách hàng đã đặt, xác nhận hoặc từ chối lịch hẹn, và kê đơn tạo bệnh án điện tử sau khi khám xong."
            );
            InfoHintHelper.attach(btnInfo, desc.getText(), true);
            btnAppointmentToggleHistory = btn;
            btnAppointmentToggleHistory.setText("📜 Xem lịch sử duyệt");

            ViewGroup rootContainer = (ViewGroup) ((ViewGroup) view).getChildAt(0);

            tvAppointmentListHeader = new TextView(requireContext());
            tvAppointmentListHeader.setText("⏳ Danh sách ca khám đang chờ duyệt:");
            tvAppointmentListHeader.setTextColor(sectionTitleColor);
            tvAppointmentListHeader.setTextSize(16f);
            tvAppointmentListHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAppointmentListHeader.setPadding(4, 32, 4, 16);
            rootContainer.addView(tvAppointmentListHeader);

            RecyclerView recyclerView = new RecyclerView(requireContext());
            recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            recyclerView.setNestedScrollingEnabled(false);
            recyclerView.setHasFixedSize(false);
            recyclerView.setItemAnimator(null);
            recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new AppointmentAdapter(adminAppointments, true, this);
            recyclerView.setAdapter(adapter);
            rootContainer.addView(recyclerView);

            btnAppointmentToggleHistory.setOnClickListener(v -> {
                showingApprovalHistory = !showingApprovalHistory;
                if (showingApprovalHistory) {
                    btnAppointmentToggleHistory.setText("⏳ Xem ca chờ duyệt");
                    tvAppointmentListHeader.setText("📜 Lịch sử các ca khám đã duyệt / từ chối:");
                } else {
                    btnAppointmentToggleHistory.setText("📜 Xem lịch sử duyệt");
                    tvAppointmentListHeader.setText("⏳ Danh sách ca khám đang chờ duyệt:");
                }
                applyAdminAppointmentFilter();
                fetchAdminAppointments();
            });

            fetchAdminAppointments();
        } else {
            title.setText("🏥 Hệ Thống Chi Nhánh Phòng Khám");
            desc.setText(
                    "Quản lý thông tin địa chỉ, số điện thoại hotline, giờ mở cửa, hình ảnh cơ sở và tọa độ bản đồ của từng chi nhánh."
            );
            InfoHintHelper.attach(btnInfo, desc.getText(), true);
            btn.setText("＋ Thêm chi nhánh mới");
            btn.setOnClickListener(v -> showAddEditClinicDialog(null));

            ViewGroup rootContainer = (ViewGroup) ((ViewGroup) view).getChildAt(0);

            tvClinicListHeader = new TextView(requireContext());
            tvClinicListHeader.setText("📍 Danh sách các chi nhánh hiện có:");
            tvClinicListHeader.setTextColor(sectionTitleColor);
            tvClinicListHeader.setTextSize(16f);
            tvClinicListHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            tvClinicListHeader.setPadding(4, 32, 4, 16);
            rootContainer.addView(tvClinicListHeader);

            RecyclerView rvClinics = new RecyclerView(requireContext());
            rvClinics.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            rvClinics.setNestedScrollingEnabled(false);
            rvClinics.setHasFixedSize(false);
            rvClinics.setItemAnimator(null);
            rvClinics.setOverScrollMode(View.OVER_SCROLL_NEVER);
            rvClinics.setLayoutManager(new LinearLayoutManager(requireContext()));
            clinicAdapter = new ClinicAdapter(adminClinics, this);
            rvClinics.setAdapter(clinicAdapter);
            rootContainer.addView(rvClinics);

            fetchAdminClinics();
        }

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

        btnSearchUser.setOnClickListener(v -> loadUsersFromServer());
        etSearchUserKeyword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUsersFromServer();
                return true;
            }
            return false;
        });

        btnFilterAllUsers.setOnClickListener(v -> selectActiveFilter(null, btnFilterAllUsers));
        btnFilterActiveUsers.setOnClickListener(v -> selectActiveFilter(true, btnFilterActiveUsers));
        btnFilterLockedUsers.setOnClickListener(v -> selectActiveFilter(false, btnFilterLockedUsers));

        updateFilterButtonStyles(btnFilterAllUsers);
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

    @Override
    public void onResume() {
        super.onResume();
        int flow = getArguments() != null ? getArguments().getInt(ARG_FLOW, 6) : 6;
        if (flow == 7) {
            fetchAdminAppointments();
        } else if (flow == 8) {
            fetchAdminClinics();
        }
    }

    private void fetchAdminClinics() {
        if (clinicApi == null) return;
        clinicApi.getAllClinics().enqueue(new Callback<List<VetClinic>>() {
            @Override
            public void onResponse(@NonNull Call<List<VetClinic>> call, @NonNull Response<List<VetClinic>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    adminClinics.clear();
                    adminClinics.addAll(response.body());
                    if (clinicAdapter != null) {
                        clinicAdapter.notifyDataSetChanged();
                    }
                    if (tvClinicListHeader != null) {
                        tvClinicListHeader.setText("📍 Danh sách các chi nhánh hiện có (" + adminClinics.size() + "):");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VetClinic>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Không thể tải danh sách chi nhánh", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private RequestBody toTextPart(String value) {
        return RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), value != null ? value : "");
    }

    private void showAddEditClinicDialog(@Nullable VetClinic clinicToEdit) {
        if (!isAdded()) return;
        selectedClinicImageUri = null;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_clinic, null);
        ivDialogClinicPreview = view.findViewById(R.id.ivClinicPreview);
        View btnPickClinicImage = view.findViewById(R.id.btnPickClinicImage);
        EditText edtName = view.findViewById(R.id.edtName);
        EditText edtAddress = view.findViewById(R.id.edtAddress);
        EditText edtPhone = view.findViewById(R.id.edtPhone);
        EditText edtLat = view.findViewById(R.id.edtLatitude);
        EditText edtLng = view.findViewById(R.id.edtLongitude);
        EditText edtHours = view.findViewById(R.id.edtOpenHours);

        View.OnClickListener pickImageClick = v -> pickClinicImageLauncher.launch("image/*");
        if (btnPickClinicImage != null) {
            btnPickClinicImage.setOnClickListener(pickImageClick);
        }
        if (ivDialogClinicPreview != null) {
            ivDialogClinicPreview.setOnClickListener(pickImageClick);
        }

        boolean isEdit = (clinicToEdit != null);
        if (isEdit) {
            edtName.setText(clinicToEdit.getName());
            edtAddress.setText(clinicToEdit.getAddress());
            edtPhone.setText(clinicToEdit.getPhoneNumber());
            edtLat.setText(String.valueOf(clinicToEdit.getLatitude()));
            edtLng.setText(String.valueOf(clinicToEdit.getLongitude()));
            edtHours.setText(clinicToEdit.getOpenHours());
            if (ivDialogClinicPreview != null
                    && clinicToEdit.getImageUrl() != null
                    && !clinicToEdit.getImageUrl().trim().isEmpty()) {
                Glide.with(requireContext())
                        .load(clinicToEdit.getImageUrl().trim())
                        .placeholder(R.drawable.ic_clinic_building)
                        .error(R.drawable.ic_clinic_building)
                        .into(ivDialogClinicPreview);
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "🏥 Cập nhật Chi nhánh" : "🏥 Thêm Chi nhánh mới")
                .setView(view)
                .setPositiveButton("Lưu chi nhánh", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String address = edtAddress.getText().toString().trim();
                    String phone = edtPhone.getText().toString().trim();
                    String latStr = edtLat.getText().toString().trim();
                    String lngStr = edtLng.getText().toString().trim();
                    String hours = edtHours.getText().toString().trim();

                    if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tên chi nhánh và tọa độ!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RequestBody reqName = toTextPart(name);
                    RequestBody reqAddress = toTextPart(address);
                    RequestBody reqPhone = toTextPart(phone);
                    RequestBody reqLat = toTextPart(latStr);
                    RequestBody reqLng = toTextPart(lngStr);
                    RequestBody reqHours = toTextPart(hours);
                    MultipartBody.Part imagePart = null;

                    if (selectedClinicImageUri != null) {
                        try {
                            imagePart = ImageMultipartUtils.uriToMultipart(requireContext(), selectedClinicImageUri, "clinicImage");
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Không thể đọc ảnh đã chọn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    if (isEdit) {
                        clinicApi.updateClinic(clinicToEdit.getId(), reqName, reqAddress, reqPhone, reqLat, reqLng, reqHours, imagePart)
                                .enqueue(new Callback<VetClinic>() {
                                    @Override
                                    public void onResponse(@NonNull Call<VetClinic> call, @NonNull Response<VetClinic> response) {
                                        if (!isAdded()) return;
                                        if (response.isSuccessful()) {
                                            Toast.makeText(getContext(), "✅ Cập nhật chi nhánh thành công!", Toast.LENGTH_SHORT).show();
                                            fetchAdminClinics();
                                        } else {
                                            Toast.makeText(getContext(), "Cập nhật thất bại (Mã: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<VetClinic> call, @NonNull Throwable t) {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        clinicApi.createClinic(reqName, reqAddress, reqPhone, reqLat, reqLng, reqHours, imagePart)
                                .enqueue(new Callback<VetClinic>() {
                                    @Override
                                    public void onResponse(@NonNull Call<VetClinic> call, @NonNull Response<VetClinic> response) {
                                        if (!isAdded()) return;
                                        if (response.isSuccessful()) {
                                            Toast.makeText(getContext(), "✅ Thêm chi nhánh mới thành công!", Toast.LENGTH_SHORT).show();
                                            fetchAdminClinics();
                                        } else {
                                            Toast.makeText(getContext(), "Thêm thất bại (Mã: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NonNull Call<VetClinic> call, @NonNull Throwable t) {
                                        if (isAdded()) {
                                            Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onEdit(VetClinic clinic) {
        showAddEditClinicDialog(clinic);
    }

    @Override
    public void onDelete(VetClinic clinic) {
        if (!isAdded() || clinic == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Xóa Chi nhánh")
                .setMessage("Bạn chắc chắn muốn xóa chi nhánh \"" + clinic.getName() + "\"?\n(Các lịch hẹn gắn với chi nhánh này cũng sẽ được gỡ bỏ)")
                .setPositiveButton("Xóa ngay", (dialog, which) -> {
                    clinicApi.deleteClinic(clinic.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "✅ Đã xóa chi nhánh " + clinic.getName(), Toast.LENGTH_SHORT).show();
                                fetchAdminClinics();
                            } else {
                                Toast.makeText(getContext(), "Không thể xóa chi nhánh (Mã lỗi: " + response.code() + ")", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Lỗi kết nối khi xóa chi nhánh: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyAdminAppointmentFilter() {
        List<Appointment> all = AppointmentRepository.getInstance().getAllAppointments();
        adminAppointments.clear();
        for (Appointment apt : all) {
            String st = apt.getStatus() != null ? apt.getStatus().trim().toUpperCase() : "PENDING";
            boolean isHistoryItem = "CONFIRMED".equals(st) || "CANCELLED".equals(st) || "COMPLETED".equals(st);
            if (showingApprovalHistory) {
                if (isHistoryItem) {
                    adminAppointments.add(apt);
                }
            } else {
                if ("PENDING".equals(st)) {
                    adminAppointments.add(apt);
                }
            }
        }
        if (adapter != null) {
            adapter.updateData(adminAppointments);
        }
    }

    private void fetchAdminAppointments() {
        applyAdminAppointmentFilter();

        appointmentApi.getAllAppointments().enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppointmentRepository.getInstance().setAppointments(response.body());
                    applyAdminAppointmentFilter();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {}
        });
    }

    @Override
    public void onApprove(Appointment appointment) {
        String conflictMessage = AppointmentRepository.getInstance().findAdminApprovalConflictMessage(appointment);
        if (conflictMessage != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ Không thể duyệt trùng giờ")
                    .setMessage(conflictMessage)
                    .setPositiveButton("Đã hiểu", null)
                    .show();
            return;
        }
        updateAppointmentStatus(appointment, "CONFIRMED");
    }

    @Override
    public void onReject(Appointment appointment) {
        updateAppointmentStatus(appointment, "CANCELLED");
    }

    @Override
    public void onCancel(Appointment appointment) {
        updateAppointmentStatus(appointment, "CANCELLED");
    }

    @Override
    public void onComplete(Appointment appointment) {
        showPrescriptionDialog(appointment);
    }

    private void updateAppointmentStatus(Appointment apt, String newStatus) {
        final String oldStatus = apt.getStatus();
        apt.setStatus(newStatus);
        AppointmentRepository.getInstance().updateStatus(apt.getId(), newStatus);
        fetchAdminAppointments();

        appointmentApi.updateAppointmentStatus(apt.getId(), newStatus).enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                if (!isAdded()) return;
                if (response.code() == 409) {
                    apt.setStatus(oldStatus);
                    AppointmentRepository.getInstance().updateStatus(apt.getId(), oldStatus);
                    fetchAdminAppointments();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("⚠️ Trùng khung giờ duyệt lịch")
                            .setMessage("Thú cưng hoặc chi nhánh này đã có ca khám ĐÃ DUYỆT trong vòng 30 phút của khung giờ này!")
                            .setPositiveButton("Đã hiểu", null)
                            .show();
                } else {
                    Toast.makeText(getContext(), "Đã cập nhật trạng thái lịch hẹn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Đã cập nhật trạng thái lịch hẹn", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Dialog Kê Đơn Bệnh Án & Hoàn Thành Ca Khám (Chọn ảnh đơn thuốc thật từ máy hoặc bỏ trống)
     */
    private void showPrescriptionDialog(Appointment apt) {
        selectedPrescriptionImageUri = null;
        DialogPrescriptionBinding dialogBinding = DialogPrescriptionBinding.inflate(getLayoutInflater());
        ivDialogPrescriptionPreview = dialogBinding.ivPrescriptionPreview;

        dialogBinding.tvHeader.setText("Ca khám #" + apt.getId() + " - " + apt.getPetName() + "\nNhập kết quả khám & đơn thuốc:");
        dialogBinding.etSymptoms.setText(apt.getNote());
        dialogBinding.etDiagnosis.setText("Viêm đường tiêu hóa nhẹ do vi khuẩn");
        dialogBinding.etPrescription.setText("Thuốc Smecta 3g + Men vi sinh Bio-Flora (Uống 5 ngày)");
        BigDecimal defaultCost = (apt.getEstimatedCost() != null && apt.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0)
                ? apt.getEstimatedCost()
                : new BigDecimal("350000");
        dialogBinding.etCost.setText(defaultCost.toPlainString());

        dialogBinding.btnPickPrescriptionImage.setOnClickListener(v -> pickPrescriptionImageLauncher.launch("image/*"));
        dialogBinding.ivPrescriptionPreview.setOnClickListener(v -> pickPrescriptionImageLauncher.launch("image/*"));

        new AlertDialog.Builder(requireContext())
                .setTitle("🩺 Kê đơn & Tạo bệnh án")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("💾 Hoàn tất ca khám", (dialog, which) -> {
                    String symptoms = dialogBinding.etSymptoms.getText().toString().trim();
                    String diagnosis = dialogBinding.etDiagnosis.getText().toString().trim();
                    String prescription = dialogBinding.etPrescription.getText().toString().trim();
                    String costStr = dialogBinding.etCost.getText().toString().trim();
                    BigDecimal totalCost;
                    try {
                        totalCost = new BigDecimal(costStr.isEmpty() ? "350000" : costStr);
                    } catch (NumberFormatException e) {
                        totalCost = new BigDecimal("350000");
                    }
                    final BigDecimal finalCostVal = totalCost;

                    MedicalHistory medReq = new MedicalHistory(
                            null,
                            apt.getId(),
                            apt.getPetId(),
                            null,
                            symptoms,
                            diagnosis,
                            prescription,
                            finalCostVal,
                            null,
                            "UNPAID",
                            null
                    );
                    medReq.setPetName(apt.getPetName());
                    medReq.setUserId(apt.getUserId());

                    Callback<MedicalHistory> prescribeCallback = new Callback<MedicalHistory>() {
                        @Override
                        public void onResponse(@NonNull Call<MedicalHistory> call, @NonNull Response<MedicalHistory> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                MedicalHistory saved = response.body();
                                if (saved.getPetName() == null || saved.getPetName().trim().isEmpty()) {
                                    saved.setPetName(apt.getPetName());
                                }
                                if (saved.getUserId() == null) {
                                    saved.setUserId(apt.getUserId());
                                }
                                apt.setStatus("COMPLETED");
                                AppointmentRepository.getInstance().updateStatus(apt.getId(), "COMPLETED");
                                MedicalHistoryRepository.getInstance().addMedicalHistory(saved);
                                fetchAdminAppointments();
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "✅ Hoàn tất ca khám & tạo bệnh án thành công (" + CurrencyUtils.formatVND(finalCostVal) + ")", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "⚠️ Không thể lưu bệnh án lên máy chủ (Mã lỗi: " + response.code() + ")", Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<MedicalHistory> call, @NonNull Throwable t) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "⚠️ Lỗi kết nối máy chủ khi lưu bệnh án: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    if (selectedPrescriptionImageUri != null) {
                        try {
                            MultipartBody.Part imagePart = ImageMultipartUtils.uriToMultipart(
                                    requireContext(), selectedPrescriptionImageUri, "prescriptionImage");
                            appointmentApi.completeAppointmentAndPrescribeMultipart(
                                    apt.getId(),
                                    toTextPart(String.valueOf(apt.getPetId())),
                                    toTextPart(symptoms),
                                    toTextPart(diagnosis),
                                    toTextPart(prescription),
                                    toTextPart(finalCostVal.toPlainString()),
                                    imagePart
                            ).enqueue(prescribeCallback);
                            return;
                        } catch (IOException ignored) {
                        }
                    }

                    appointmentApi.completeAppointmentAndPrescribe(apt.getId(), medReq).enqueue(prescribeCallback);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
