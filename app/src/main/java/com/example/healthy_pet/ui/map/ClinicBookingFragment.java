package com.example.healthy_pet.ui.map;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.R;
import com.example.healthy_pet.adapters.AppointmentAdapter;
import com.example.healthy_pet.adapters.UserClinicCardAdapter;
import com.example.healthy_pet.api.AppointmentMedicalApi;
import com.example.healthy_pet.api.ClinicMapApi;
import com.example.healthy_pet.api.PetWeightApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.databinding.DialogCreateAppointmentBinding;
import com.example.healthy_pet.databinding.FragmentClinicBookingBinding;
import com.example.healthy_pet.models.Appointment;
import com.example.healthy_pet.models.Pet;
import com.example.healthy_pet.models.VetClinic;
import com.example.healthy_pet.repository.AppointmentRepository;
import com.example.healthy_pet.utils.InfoHintHelper;
import com.example.healthy_pet.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Thành viên 4 (NV 5a - Bản đồ Google Maps) & Thành viên 5 (NV 5b - Đặt lịch hẹn Khám/Spa)]
 * Tích hợp Bản đồ chi nhánh VetClinics (Marker, InfoWindow gọi Hotline ACTION_DIAL)
 * và Đặt lịch hẹn (DatePickerDialog, TimePickerDialog, AppointmentAdapter).
 */
public class ClinicBookingFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener, OnMapReadyCallback {

    private FragmentClinicBookingBinding binding;
    private AppointmentAdapter adapter;
    private GoogleMap mMap;
    private final List<Appointment> userAppointments = new ArrayList<>();
    private final List<Pet> availablePets = new ArrayList<>();
    private final List<VetClinic> availableClinics = new ArrayList<>();
    private final List<VetClinic> rankedClinics = new ArrayList<>();
    private final Map<Long, Integer> clinicRankMap = new HashMap<>();
    private final Map<Long, Marker> clinicMarkers = new HashMap<>();
    private LatLngBounds allClinicsBounds;
    private UserClinicCardAdapter topClinicsAdapter;
    private Long selectedClinicId = null;
    private boolean isClinicDropdownExpanded = false;

    private AppointmentMedicalApi appointmentApi;
    private PetWeightApi petApi;
    private ClinicMapApi clinicApi;
    private SessionManager sessionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        appointmentApi = RetrofitClient.getAppointmentMedicalApi();
        petApi = RetrofitClient.getPetWeightApi();
        clinicApi = RetrofitClient.getClinicMapApi();
        AppointmentRepository.getInstance().initContext(requireContext());

        fetchPetsAndClinicsFromApi();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentClinicBookingBinding.inflate(inflater, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Ngăn NestedScrollView cướp thao tác kéo/zoom khi tay người dùng chạm vào bản đồ
        binding.mapTouchOverlay.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                binding.nestedScrollClinicBooking.requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                binding.nestedScrollClinicBooking.requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        topClinicsAdapter = new UserClinicCardAdapter(new UserClinicCardAdapter.OnUserClinicCardListener() {
            @Override
            public void onSelectClinicOnMap(VetClinic clinic) {
                selectClinicAndFocusMap(clinic, null);
            }

            @Override
            public void onCallClinic(VetClinic clinic) {
                dialClinicPhone(clinic);
            }

            @Override
            public void onBookClinic(VetClinic clinic) {
                setClinicDropdownExpanded(false);
                showCreateAppointmentDialog(clinic != null ? clinic.getId() : null);
            }
        });
        binding.rvTopClinicsDropdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTopClinicsDropdown.setNestedScrollingEnabled(false);
        binding.rvTopClinicsDropdown.setItemAnimator(null);
        binding.rvTopClinicsDropdown.setAdapter(topClinicsAdapter);

        binding.layoutClinicDropdownBar.setOnClickListener(v -> setClinicDropdownExpanded(!isClinicDropdownExpanded));
        binding.cardDropdownArrow.setOnClickListener(v -> setClinicDropdownExpanded(!isClinicDropdownExpanded));
        binding.btnResetAllMapPins.setOnClickListener(v -> {
            selectedClinicId = null;
            if (topClinicsAdapter != null) {
                topClinicsAdapter.setSelectedClinicId(null);
            }
            updateDropdownHeaderLabels();
            moveCameraToAllClinics(true);
        });
        binding.cardShowMoreClinics.setOnClickListener(v -> showAllClinicsBottomSheet());

        if (!availableClinics.isEmpty()) {
            updateClinicDropdownData(availableClinics);
        }

        binding.rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppointmentAdapter(userAppointments, false, this);
        binding.rvAppointments.setAdapter(adapter);

        InfoHintHelper.attach(binding.btnInfoBooking, binding.tvModuleDescription.getText(), false);
        binding.btnCreateAppointment.setOnClickListener(v -> showCreateAppointmentDialog(selectedClinicId));

        loadUserAppointments();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserAppointments();
        fetchPetsAndClinicsFromApi();
        if (mMap != null) {
            loadClinicsOnMap();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchPetsAndClinicsFromApi() {
        petApi.getPetsByUser(sessionManager.getUserId()).enqueue(new Callback<List<Pet>>() {
            @Override
            public void onResponse(@NonNull Call<List<Pet>> call, @NonNull Response<List<Pet>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    availablePets.clear();
                    availablePets.addAll(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Pet>> call, @NonNull Throwable t) {}
        });

        clinicApi.getAllClinics().enqueue(new Callback<List<VetClinic>>() {
            @Override
            public void onResponse(@NonNull Call<List<VetClinic>> call, @NonNull Response<List<VetClinic>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    availableClinics.clear();
                    availableClinics.addAll(response.body());
                    if (isAdded() && binding != null) {
                        updateClinicDropdownData(availableClinics);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VetClinic>> call, @NonNull Throwable t) {}
        });
    }

    private void loadUserAppointments() {
        // 1. Tải lập tức từ Shared AppointmentRepository
        userAppointments.clear();
        userAppointments.addAll(AppointmentRepository.getInstance().getUserAppointments(sessionManager.getUserId()));
        updateUiState();

        // 2. Đồng bộ chuẩn từ backend REST API (kể cả khi danh sách rỗng)
        appointmentApi.getUserAppointments(sessionManager.getUserId()).enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppointmentRepository.getInstance().setUserAppointments(sessionManager.getUserId(), response.body());
                    userAppointments.clear();
                    userAppointments.addAll(AppointmentRepository.getInstance().getUserAppointments(sessionManager.getUserId()));
                    updateUiState();
                    if (isAdded() && binding != null && !availableClinics.isEmpty()) {
                        updateClinicDropdownData(availableClinics);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {}
        });
    }

    private void updateUiState() {
        if (binding == null) return;
        if (userAppointments.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvAppointments.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvAppointments.setVisibility(View.VISIBLE);
            adapter.updateData(userAppointments);
        }
    }

    /**
     * Dialog Đặt lịch hẹn mới tích hợp DatePickerDialog & TimePickerDialog (NV 5b)
     */
    private void showCreateAppointmentDialog(@Nullable Long preselectedClinicId) {
        DialogCreateAppointmentBinding dialogBinding = DialogCreateAppointmentBinding.inflate(getLayoutInflater());

        // 1. Setup Spinner Thú cưng (Khớp Seed Data SQL: Milu, Mochi)
        List<String> petNames = new ArrayList<>();
        if (!availablePets.isEmpty()) {
            for (Pet p : availablePets) {
                petNames.add(p.getName() + " (" + p.getSpecies() + ")");
            }
        } else {
            petNames.add("Milu (Chó - Golden Retriever)");
            petNames.add("Mochi (Mèo - Mèo Anh Lông Ngắn)");
        }
        ArrayAdapter<String> petSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, petNames);
        dialogBinding.spnPet.setAdapter(petSpinnerAdapter);

        // 2. Setup Spinner Phòng khám (Khớp Seed Data SQL: Q.1, Q.3, Thủ Đức)
        List<String> clinicNames = new ArrayList<>();
        int preselectedIndex = 0;
        if (!availableClinics.isEmpty()) {
            for (int i = 0; i < availableClinics.size(); i++) {
                VetClinic c = availableClinics.get(i);
                clinicNames.add(c.getName() + " - " + c.getAddress());
                if (preselectedClinicId != null && preselectedClinicId.equals(c.getId())) {
                    preselectedIndex = i;
                }
            }
        } else {
            clinicNames.add("Healthy Pet Quận 1 - 125 Nguyễn Thị Minh Khai, Q.1");
            clinicNames.add("Healthy Pet Quận 3 - 280 Võ Văn Tần, Q.3");
            clinicNames.add("Healthy Pet Thủ Đức - 01 Võ Văn Ngân, TP. Thủ Đức");
        }
        ArrayAdapter<String> clinicSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, clinicNames);
        dialogBinding.spnClinic.setAdapter(clinicSpinnerAdapter);
        if (preselectedIndex > 0 && preselectedIndex < clinicNames.size()) {
            dialogBinding.spnClinic.setSelection(preselectedIndex);
        }

        // 3. Setup Spinner Loại dịch vụ (service_type) & Bảng giá cố định/tùy thuộc
        String[] serviceTypes = {
                "Khám bệnh (150k)",
                "Triệt sản (500k)",
                "Chích ngừa (Giá tùy theo loại mũi)",
                "Xét nghiệm sinh lý máu (Giá tùy pet)",
                "Xét nghiệm sinh hóa máu (Giá tùy pet)",
                "Siêu âm (Giá tùy pet)",
                "Thông tiểu (Giá tùy pet)",
                "Phẫu thuật (Giá tùy pet)",
                "Cạo vôi răng (Giá tùy pet)"
        };
        BigDecimal[] serviceCosts = {
                new BigDecimal("150000"),
                new BigDecimal("500000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        };
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, serviceTypes);
        dialogBinding.spnServiceType.setAdapter(serviceAdapter);

        dialogBinding.spnServiceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        dialogBinding.etEstimatedCost.setText("150.000 đ (Giá cố định)");
                        break;
                    case 1:
                        dialogBinding.etEstimatedCost.setText("500.000 đ (Giá cố định)");
                        break;
                    case 2:
                        dialogBinding.etEstimatedCost.setText("Giá tùy theo loại mũi tiêm (Bác sĩ báo giá)");
                        break;
                    default:
                        dialogBinding.etEstimatedCost.setText("Giá tùy theo thú cưng (Bác sĩ báo giá)");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4. State cho DatePicker & TimePicker: mặc định khung giờ hợp lệ gần nhất trong giờ mở cửa cố định (07:00 - 17:00)
        Calendar defaultSlot = Calendar.getInstance();
        defaultSlot.add(Calendar.HOUR_OF_DAY, 1);
        int roundedMinute = defaultSlot.get(Calendar.MINUTE) <= 30 ? 30 : 60;
        defaultSlot.set(Calendar.MINUTE, 0);
        defaultSlot.set(Calendar.SECOND, 0);
        defaultSlot.set(Calendar.MILLISECOND, 0);
        defaultSlot.add(Calendar.MINUTE, roundedMinute);

        int defaultTotalMins = defaultSlot.get(Calendar.HOUR_OF_DAY) * 60 + defaultSlot.get(Calendar.MINUTE);
        if (defaultTotalMins < 7 * 60) {
            defaultSlot.set(Calendar.HOUR_OF_DAY, 7);
            defaultSlot.set(Calendar.MINUTE, 0);
        } else if (defaultTotalMins > 17 * 60) {
            defaultSlot.add(Calendar.DAY_OF_MONTH, 1);
            defaultSlot.set(Calendar.HOUR_OF_DAY, 7);
            defaultSlot.set(Calendar.MINUTE, 0);
        }

        final String[] selectedDate = {String.format(Locale.getDefault(), "%04d-%02d-%02d",
                defaultSlot.get(Calendar.YEAR), defaultSlot.get(Calendar.MONTH) + 1, defaultSlot.get(Calendar.DAY_OF_MONTH))};
        final int[] selectedHourMinute = {defaultSlot.get(Calendar.HOUR_OF_DAY), defaultSlot.get(Calendar.MINUTE)};
        final String[] selectedTime = {String.format(Locale.getDefault(), "%02d:%02d:00", selectedHourMinute[0], selectedHourMinute[1])};

        dialogBinding.btnPickDate.setText("📅 " + selectedDate[0]);
        dialogBinding.btnPickTime.setText("⏰ " + String.format(Locale.getDefault(), "%02d:%02d", selectedHourMinute[0], selectedHourMinute[1]));

        dialogBinding.btnPickDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDate[0] = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                dialogBinding.btnPickDate.setText("📅 " + selectedDate[0]);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dpd.show();
        });

        dialogBinding.btnPickTime.setOnClickListener(v -> {
            TimePickerDialog tpd = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                if (!isWithinOperatingHours(hourOfDay, minute)) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("🕒 Ngoài giờ mở cửa (07:00 - 17:00)")
                            .setMessage("Mọi dịch vụ của phòng khám hoạt động cố định từ 07:00 đến 17:00 hằng ngày.\n\nKhung giờ bạn vừa chọn ("
                                    + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                    + ") nằm ngoài giờ phục vụ. Vui lòng chọn giờ trong khoảng 07:00 - 17:00!")
                            .setPositiveButton("Chọn lại giờ", (d, w) -> dialogBinding.btnPickTime.performClick())
                            .show();
                    return;
                }
                selectedHourMinute[0] = hourOfDay;
                selectedHourMinute[1] = minute;
                selectedTime[0] = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute);
                dialogBinding.btnPickTime.setText("⏰ " + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, selectedHourMinute[0], selectedHourMinute[1], true);
            tpd.show();
        });

        AlertDialog bookingDialog = new AlertDialog.Builder(requireContext())
                .setTitle("📅 Đặt lịch khám mới")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Xác nhận Đặt lịch", null)
                .setNegativeButton("Hủy", null)
                .create();

        bookingDialog.setOnShowListener(dlg -> {
            android.widget.Button btnConfirm = bookingDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnConfirm.setOnClickListener(v -> {
                String reason = dialogBinding.etReason.getText().toString().trim();
                if (reason.isEmpty()) reason = "Khám sức khỏe tổng quát";

                int servicePos = dialogBinding.spnServiceType.getSelectedItemPosition();
                BigDecimal estimatedCost = (servicePos >= 0 && servicePos < serviceCosts.length)
                        ? serviceCosts[servicePos]
                        : new BigDecimal("150000");

                int petPos = dialogBinding.spnPet.getSelectedItemPosition();
                int clinicPos = dialogBinding.spnClinic.getSelectedItemPosition();
                String serviceType = serviceTypes[servicePos];

                Long petId = !availablePets.isEmpty() && petPos < availablePets.size()
                        ? availablePets.get(petPos).getId()
                        : (long) (petPos + 1);
                String petName = petNames.get(petPos).split(" \\(")[0];

                Long clinicId = !availableClinics.isEmpty() && clinicPos < availableClinics.size()
                        ? availableClinics.get(clinicPos).getId()
                        : (long) (clinicPos + 1);
                String clinicName = clinicNames.get(clinicPos).split(" - ")[0];

                // 1. Chặn khung giờ ngoài giờ mở cửa cố định (07:00 - 17:00)
                if (!isWithinOperatingHours(selectedHourMinute[0], selectedHourMinute[1])) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("🕒 Ngoài giờ mở cửa (07:00 - 17:00)")
                            .setMessage("Mọi dịch vụ của phòng khám hoạt động cố định từ 07:00 đến 17:00.\nVui lòng chọn lại giờ khám trong khung 07:00 - 17:00!")
                            .setPositiveButton("Chọn lại giờ", (d, w) -> dialogBinding.btnPickTime.performClick())
                            .show();
                    return;
                }

                String fullAppointmentTime = selectedDate[0] + " " + selectedTime[0];

                // 2. Chặn chọn thời gian đã qua ngay trên máy (giữ nguyên form mở để chọn lại)
                long selectedMs = parseLocalDateTimeMillis(fullAppointmentTime);
                if (selectedMs > 0 && selectedMs <= System.currentTimeMillis()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("⏰ Thời gian đã qua")
                            .setMessage("Khung giờ " + fullAppointmentTime.substring(0, 16)
                                    + " đã qua. Vui lòng chọn ngày giờ trong tương lai (07:00 - 17:00)!")
                            .setPositiveButton("Chọn lại giờ", null)
                            .show();
                    return;
                }

                // 3. Kiểm tra chặn trùng lịch trong khoảng 30 phút cho cùng 1 thú cưng của chính User này
                Appointment conflictApt = AppointmentRepository.getInstance()
                        .findPetBookingConflict(sessionManager.getUserId(), petId, fullAppointmentTime);
                if (conflictApt != null) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("⚠️ Trùng khung giờ đặt lịch")
                            .setMessage("Thú cưng \"" + petName + "\" đã có lịch hẹn (Ca #"
                                    + conflictApt.getId() + " - " + conflictApt.getServiceType()
                                    + ") vào lúc " + conflictApt.getAppointmentTime()
                                    + ".\n\nMỗi lịch hẹn của cùng một thú cưng phải cách nhau ít nhất 30 phút. Vui lòng chọn giờ khác!")
                            .setPositiveButton("Chọn lại giờ", null)
                            .show();
                    return;
                }

                Appointment newApt = new Appointment(
                        sessionManager.getUserId(),
                        petId,
                        clinicId,
                        serviceType,
                        fullAppointmentTime,
                        estimatedCost,
                        reason
                );
                newApt.setPetName(petName);
                newApt.setClinicName(clinicName);
                newApt.setStatus("PENDING");

                btnConfirm.setEnabled(false);

                // Gửi API Retrofit về Spring Boot server; chỉ lưu & đóng form khi Server xác nhận thành công
                appointmentApi.createAppointment(newApt).enqueue(new Callback<Appointment>() {
                    @Override
                    public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                        if (!isAdded()) return;
                        btnConfirm.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            Appointment created = response.body();
                            if (created.getPetName() == null || created.getPetName().trim().isEmpty()) {
                                created.setPetName(petName);
                            }
                            if (created.getClinicName() == null || created.getClinicName().trim().isEmpty()) {
                                created.setClinicName(clinicName);
                            }
                            AppointmentRepository.getInstance().addAppointment(created);
                            loadUserAppointments();
                            bookingDialog.dismiss();
                            Toast.makeText(getContext(), "✅ Đặt lịch thành công! Đã chuyển sang Admin chờ duyệt.", Toast.LENGTH_LONG).show();
                        } else if (response.code() == 409) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("⚠️ Trùng khung giờ")
                                    .setMessage("Thú cưng \"" + petName + "\" đã có lịch hẹn khác trong vòng 30 phút của khung giờ này. Vui lòng chọn giờ khác!")
                                    .setPositiveButton("Đã hiểu", null)
                                    .show();
                            loadUserAppointments();
                        } else if (response.code() == 400) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("⚠️ Không thể đặt lịch")
                                    .setMessage("Thời gian đặt lịch nằm ngoài giờ mở cửa (07:00 - 17:00), đã qua, hoặc dữ liệu không hợp lệ. Vui lòng kiểm tra lại!")
                                    .setPositiveButton("Đã hiểu", null)
                                    .show();
                        } else {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("⚠️ Đặt lịch thất bại")
                                    .setMessage("Máy chủ không thể tạo lịch hẹn (Mã lỗi: " + response.code() + "). Vui lòng thử lại sau!")
                                    .setPositiveButton("Đã hiểu", null)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                        if (!isAdded()) return;
                        btnConfirm.setEnabled(true);
                        new AlertDialog.Builder(requireContext())
                                .setTitle("📡 Mất kết nối máy chủ")
                                .setMessage("Không thể kết nối tới máy chủ để đặt lịch. Vui lòng kiểm tra mạng và thử lại!")
                                .setPositiveButton("Đã hiểu", null)
                                .show();
                    }
                });
            });
        });

        bookingDialog.show();
    }

    private boolean isWithinOperatingHours(int hourOfDay, int minute) {
        int totalMinutes = hourOfDay * 60 + minute;
        return totalMinutes >= (7 * 60) && totalMinutes <= (17 * 60);
    }

    private String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private long parseLocalDateTimeMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1L;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setLenient(false);
            java.util.Date parsed = sdf.parse(timeStr.trim());
            return parsed != null ? parsed.getTime() : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    @Override
    public void onApprove(Appointment appointment) {}

    @Override
    public void onReject(Appointment appointment) {}

    @Override
    public void onComplete(Appointment appointment) {}

    @Override
    public void onCancel(Appointment appointment) {
        AppointmentRepository.getInstance().updateStatus(appointment.getId(), "CANCELLED");
        loadUserAppointments();

        appointmentApi.updateAppointmentStatus(appointment.getId(), "CANCELLED").enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                Toast.makeText(getContext(), "Đã hủy lịch hẹn #" + appointment.getId(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Đã hủy lịch hẹn #" + appointment.getId(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        // Đồng bộ trạng thái thẻ chi nhánh đang chọn khi người dùng chạm trực tiếp vào ghim trên bản đồ
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof VetClinic) {
                VetClinic clinic = (VetClinic) tag;
                selectedClinicId = clinic.getId();
                if (topClinicsAdapter != null) {
                    topClinicsAdapter.setSelectedClinicId(selectedClinicId);
                }
                updateDropdownHeaderLabels();
            }
            return false;
        });

        // Xử lý sự kiện bấm vào InfoWindow của ghim để gọi Hotline trực tiếp (ACTION_DIAL)
        mMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof VetClinic) {
                dialClinicPhone((VetClinic) tag);
            }
        });

        loadClinicsOnMap();
    }

    private void loadClinicsOnMap() {
        if (mMap == null || clinicApi == null) return;
        clinicApi.getAllClinics().enqueue(new Callback<List<VetClinic>>() {
            @Override
            public void onResponse(@NonNull Call<List<VetClinic>> call, @NonNull Response<List<VetClinic>> response) {
                if (!isAdded() || mMap == null || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<VetClinic> clinics = response.body();
                    availableClinics.clear();
                    availableClinics.addAll(clinics);

                    mMap.clear();
                    clinicMarkers.clear();
                    allClinicsBounds = null;

                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    int validPinCount = 0;
                    LatLng lastValidPos = null;

                    for (VetClinic clinic : clinics) {
                        if (clinic.getLatitude() != null && clinic.getLongitude() != null) {
                            double lat = clinic.getLatitude();
                            double lng = clinic.getLongitude();
                            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0 || (lat == 0.0 && lng == 0.0)) {
                                continue;
                            }
                            LatLng pos = new LatLng(lat, lng);
                            String snippetInfo = "📞 Hotline: " + (clinic.getPhoneNumber() != null ? clinic.getPhoneNumber() : "Chưa có")
                                    + " | 🕒 Giờ mở: " + (clinic.getOpenHours() != null && !clinic.getOpenHours().isEmpty() ? clinic.getOpenHours() : "07:00 - 17:00");

                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(clinic.getName() != null ? clinic.getName() : "Chi nhánh Healthy Pet")
                                    .snippet(snippetInfo)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                            if (marker != null) {
                                marker.setTag(clinic);
                                if (clinic.getId() != null) {
                                    clinicMarkers.put(clinic.getId(), marker);
                                }
                                boundsBuilder.include(pos);
                                lastValidPos = pos;
                                validPinCount++;
                            }
                        }
                    }

                    if (validPinCount > 1) {
                        allClinicsBounds = boundsBuilder.build();
                    }
                    if (selectedClinicId != null && !clinicMarkers.containsKey(selectedClinicId)) {
                        selectedClinicId = null;
                    }

                    updateClinicDropdownData(clinics);

                    if (selectedClinicId != null && clinicMarkers.containsKey(selectedClinicId)) {
                        Marker selectedMarker = clinicMarkers.get(selectedClinicId);
                        if (selectedMarker != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedMarker.getPosition(), 15f));
                            selectedMarker.showInfoWindow();
                        }
                    } else if (validPinCount == 1 && lastValidPos != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastValidPos, 14f));
                    } else if (validPinCount > 1) {
                        moveCameraToAllClinics(false);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<VetClinic>> call, @NonNull Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Không thể kết nối lấy dữ liệu phòng khám!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void moveCameraToAllClinics(boolean animate) {
        if (mMap == null || binding == null) return;
        if (allClinicsBounds == null && clinicMarkers.size() == 1) {
            Marker onlyMarker = clinicMarkers.values().iterator().next();
            if (onlyMarker != null) {
                if (animate) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(onlyMarker.getPosition(), 14f));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(onlyMarker.getPosition(), 14f));
                }
            }
            return;
        }
        if (allClinicsBounds == null) return;
        binding.mapFragment.post(() -> {
            if (!isAdded() || mMap == null || allClinicsBounds == null || binding == null) return;
            try {
                int width = binding.mapFragment.getWidth();
                int height = binding.mapFragment.getHeight();
                if (width > 0 && height > 0) {
                    int padding = (int) (48 * getResources().getDisplayMetrics().density);
                    if (animate) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(allClinicsBounds, width, height, padding));
                    } else {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(allClinicsBounds, width, height, padding));
                    }
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(allClinicsBounds, 96));
                }
            } catch (Exception e) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(allClinicsBounds.getCenter(), 6f));
            }
        });
    }

    /**
     * Xếp hạng chi nhánh được yêu thích nhất (ưu tiên chi nhánh có nhiều lịch đặt khám + có ảnh đại diện),
     * nạp Top 5 vào khung sổ xuống và cập nhật thẻ mở rộng "Xem thêm chi nhánh khác".
     */
    private void updateClinicDropdownData(@Nullable List<VetClinic> sourceClinics) {
        if (binding == null) return;
        rankedClinics.clear();
        clinicRankMap.clear();

        if (sourceClinics != null) {
            for (VetClinic c : sourceClinics) {
                if (c != null) {
                    rankedClinics.add(c);
                }
            }
        }

        final Map<Long, Integer> bookingCountByClinic = new HashMap<>();
        List<Appointment> allApts = AppointmentRepository.getInstance().getAllAppointments();
        for (Appointment apt : allApts) {
            if (apt == null || apt.getClinicId() == null) continue;
            if ("CANCELLED".equalsIgnoreCase(apt.getStatus())) continue;
            Long cid = apt.getClinicId();
            int current = bookingCountByClinic.containsKey(cid) ? bookingCountByClinic.get(cid) : 0;
            bookingCountByClinic.put(cid, current + 1);
        }

        Collections.sort(rankedClinics, (a, b) -> {
            int countA = (a.getId() != null && bookingCountByClinic.containsKey(a.getId()))
                    ? bookingCountByClinic.get(a.getId()) : 0;
            int countB = (b.getId() != null && bookingCountByClinic.containsKey(b.getId()))
                    ? bookingCountByClinic.get(b.getId()) : 0;
            if (countA != countB) {
                return Integer.compare(countB, countA);
            }
            boolean hasImgA = a.getImageUrl() != null && !a.getImageUrl().trim().isEmpty();
            boolean hasImgB = b.getImageUrl() != null && !b.getImageUrl().trim().isEmpty();
            if (hasImgA != hasImgB) {
                return hasImgB ? 1 : -1;
            }
            long idA = a.getId() != null ? a.getId() : Long.MAX_VALUE;
            long idB = b.getId() != null ? b.getId() : Long.MAX_VALUE;
            return Long.compare(idA, idB);
        });

        for (int i = 0; i < rankedClinics.size(); i++) {
            VetClinic c = rankedClinics.get(i);
            if (c.getId() != null) {
                clinicRankMap.put(c.getId(), i + 1);
            }
        }

        int topCount = Math.min(5, rankedClinics.size());
        List<VetClinic> topFive = new ArrayList<>(rankedClinics.subList(0, topCount));
        if (topClinicsAdapter != null) {
            topClinicsAdapter.setData(topFive, clinicRankMap, selectedClinicId);
        }

        int total = rankedClinics.size();
        if (total == 0) {
            binding.tvTopClinicsSectionHeader.setText("🏥 CHƯA CÓ CHI NHÁNH TRÊN HỆ THỐNG");
            binding.cardShowMoreClinics.setVisibility(View.GONE);
        } else {
            binding.cardShowMoreClinics.setVisibility(View.VISIBLE);
            binding.tvTopClinicsSectionHeader.setText(total <= 5
                    ? "⭐ TOP CHI NHÁNH ĐƯỢC YÊU THÍCH NHẤT (" + total + ")"
                    : "⭐ TOP 5 CHI NHÁNH ĐƯỢC YÊU THÍCH NHẤT");
            if (total > 5) {
                int moreCount = total - 5;
                binding.tvShowMoreClinicsTitle.setText("✨ Xem thêm " + moreCount + " chi nhánh khác (Tổng " + total + " cơ sở)");
                binding.tvShowMoreClinicsSub.setText("Mở thẻ lớn dạng trượt chứa tất cả chi nhánh & tìm kiếm nhanh");
            } else {
                binding.tvShowMoreClinicsTitle.setText("✨ Xem thêm nhiều chi nhánh khác (Tất cả " + total + " cơ sở)");
                binding.tvShowMoreClinicsSub.setText("Mở thẻ lớn dạng trượt để tra cứu nhanh tên & địa chỉ chi nhánh");
            }
        }

        updateDropdownHeaderLabels();
    }

    private void updateDropdownHeaderLabels() {
        if (binding == null) return;
        VetClinic selectedClinic = null;
        if (selectedClinicId != null) {
            for (VetClinic c : rankedClinics) {
                if (selectedClinicId.equals(c.getId())) {
                    selectedClinic = c;
                    break;
                }
            }
        }

        if (selectedClinic != null) {
            String name = selectedClinic.getName() != null && !selectedClinic.getName().trim().isEmpty()
                    ? selectedClinic.getName().trim()
                    : "Chi nhánh #" + selectedClinic.getId();
            String addr = selectedClinic.getAddress() != null && !selectedClinic.getAddress().trim().isEmpty()
                    ? selectedClinic.getAddress().trim()
                    : "Đang hiển thị vị trí trên bản đồ";
            binding.tvSelectedClinicTitle.setText("📍 " + name);
            binding.tvSelectedClinicSubtitle.setText(isClinicDropdownExpanded
                    ? "Chạm thẻ khác để đổi chi nhánh • Bấm mũi tên để thu gọn"
                    : addr);
        } else {
            int total = rankedClinics.size();
            binding.tvSelectedClinicTitle.setText(total > 0
                    ? "🏥 Hệ thống chi nhánh (" + total + " cơ sở)"
                    : "🏥 Hệ thống chi nhánh phòng khám");
            binding.tvSelectedClinicSubtitle.setText(isClinicDropdownExpanded
                    ? "Chạm thẻ chi nhánh để bay tới ghim • Bấm mũi tên để đóng"
                    : "Bấm mũi tên ▼ để xem Top 5 chi nhánh yêu thích");
        }
    }

    private void setClinicDropdownExpanded(boolean expanded) {
        if (binding == null) return;
        isClinicDropdownExpanded = expanded;
        binding.layoutClinicDropdownContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.tvDropdownArrow.animate()
                .rotation(expanded ? 180f : 0f)
                .setDuration(200L)
                .start();
        updateDropdownHeaderLabels();
    }

    private void selectClinicAndFocusMap(@Nullable VetClinic clinic, @Nullable BottomSheetDialog sheetToDismiss) {
        if (clinic == null) return;
        selectedClinicId = clinic.getId();
        if (topClinicsAdapter != null) {
            topClinicsAdapter.setSelectedClinicId(selectedClinicId);
        }
        setClinicDropdownExpanded(false);
        if (sheetToDismiss != null && sheetToDismiss.isShowing()) {
            sheetToDismiss.dismiss();
        }
        updateDropdownHeaderLabels();

        if (mMap != null && clinic.getLatitude() != null && clinic.getLongitude() != null) {
            double lat = clinic.getLatitude();
            double lng = clinic.getLongitude();
            if (lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0 && !(lat == 0.0 && lng == 0.0)) {
                LatLng target = new LatLng(lat, lng);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f));
                Marker marker = clinic.getId() != null ? clinicMarkers.get(clinic.getId()) : null;
                if (marker != null) {
                    marker.showInfoWindow();
                }
                if (binding != null) {
                    binding.nestedScrollClinicBooking.smoothScrollTo(0, 0);
                }
                return;
            }
        }
        Toast.makeText(getContext(), "Chi nhánh này chưa cập nhật tọa độ hợp lệ trên bản đồ", Toast.LENGTH_SHORT).show();
    }

    private void dialClinicPhone(@Nullable VetClinic clinic) {
        if (clinic != null && clinic.getPhoneNumber() != null && !clinic.getPhoneNumber().trim().isEmpty()) {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + clinic.getPhoneNumber().trim()));
            startActivity(dialIntent);
        } else {
            Toast.makeText(getContext(), "Chi nhánh này chưa cập nhật số điện thoại Hotline", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Mở Thẻ lớn dạng trượt (BottomSheetDialog) hiển thị TẤT CẢ chi nhánh trên hệ thống
     * kèm thanh tìm kiếm nhanh theo tên hoặc địa chỉ.
     */
    private void showAllClinicsBottomSheet() {
        if (!isAdded() || getContext() == null) return;

        BottomSheetDialog sheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_all_clinics_sheet, null);
        sheetDialog.setContentView(sheetView);

        sheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        TextView tvSheetTitle = sheetView.findViewById(R.id.tvAllClinicsSheetTitle);
        MaterialButton btnClose = sheetView.findViewById(R.id.btnCloseAllClinicsSheet);
        TextInputEditText etSearch = sheetView.findViewById(R.id.etSearchAllClinics);
        TextView tvEmpty = sheetView.findViewById(R.id.tvEmptyAllClinicsSheet);
        RecyclerView rvAll = sheetView.findViewById(R.id.rvAllClinicsSheet);

        tvSheetTitle.setText("🏥 Tất cả chi nhánh (" + rankedClinics.size() + " cơ sở)");
        btnClose.setOnClickListener(v -> sheetDialog.dismiss());

        UserClinicCardAdapter sheetAdapter = new UserClinicCardAdapter(new UserClinicCardAdapter.OnUserClinicCardListener() {
            @Override
            public void onSelectClinicOnMap(VetClinic clinic) {
                selectClinicAndFocusMap(clinic, sheetDialog);
            }

            @Override
            public void onCallClinic(VetClinic clinic) {
                dialClinicPhone(clinic);
            }

            @Override
            public void onBookClinic(VetClinic clinic) {
                sheetDialog.dismiss();
                setClinicDropdownExpanded(false);
                showCreateAppointmentDialog(clinic != null ? clinic.getId() : null);
            }
        });

        rvAll.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAll.setItemAnimator(null);
        rvAll.setAdapter(sheetAdapter);
        sheetAdapter.setData(rankedClinics, clinicRankMap, selectedClinicId);

        if (rankedClinics.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvAll.setVisibility(View.GONE);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                if (query.isEmpty()) {
                    sheetAdapter.setData(rankedClinics, clinicRankMap, selectedClinicId);
                    tvEmpty.setVisibility(rankedClinics.isEmpty() ? View.VISIBLE : View.GONE);
                    rvAll.setVisibility(rankedClinics.isEmpty() ? View.GONE : View.VISIBLE);
                    return;
                }
                List<VetClinic> filtered = new ArrayList<>();
                for (VetClinic c : rankedClinics) {
                    String name = c.getName() != null ? c.getName().toLowerCase(Locale.getDefault()) : "";
                    String address = c.getAddress() != null ? c.getAddress().toLowerCase(Locale.getDefault()) : "";
                    String phone = c.getPhoneNumber() != null ? c.getPhoneNumber().toLowerCase(Locale.getDefault()) : "";
                    if (name.contains(query) || address.contains(query) || phone.contains(query)) {
                        filtered.add(c);
                    }
                }
                sheetAdapter.setData(filtered, clinicRankMap, selectedClinicId);
                tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                rvAll.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sheetDialog.show();
    }
}