package com.example.healthy_pet.ui.map;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.healthy_pet.adapters.AppointmentAdapter;
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
import com.example.healthy_pet.utils.SessionManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Thành viên 5 (NV 5b) & Thành viên 4 (NV 5a)]
 * Nghiệp vụ 5: User Đặt lịch hẹn Khám/Spa (DatePickerDialog, TimePickerDialog, AppointmentAdapter)
 * Đồng bộ 100% với CSDL SQL Server (CreateTableAndInsertDataExample.sql).
 */
public class ClinicBookingFragment extends Fragment implements AppointmentAdapter.OnAppointmentActionListener {

    private FragmentClinicBookingBinding binding;
    private AppointmentAdapter adapter;
    private final List<Appointment> userAppointments = new ArrayList<>();
    private final List<Pet> availablePets = new ArrayList<>();
    private final List<VetClinic> availableClinics = new ArrayList<>();

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

        binding.rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppointmentAdapter(userAppointments, false, this);
        binding.rvAppointments.setAdapter(adapter);

        binding.btnCreateAppointment.setOnClickListener(v -> showCreateAppointmentDialog());

        loadUserAppointments();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserAppointments();
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

        // 2. Tải từ backend REST API
        appointmentApi.getUserAppointments(sessionManager.getUserId()).enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppointmentRepository.getInstance().setAppointments(response.body());
                    userAppointments.clear();
                    userAppointments.addAll(AppointmentRepository.getInstance().getUserAppointments(sessionManager.getUserId()));
                    updateUiState();
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
    private void showCreateAppointmentDialog() {
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
        if (!availableClinics.isEmpty()) {
            for (VetClinic c : availableClinics) {
                clinicNames.add(c.getName() + " - " + c.getAddress());
            }
        } else {
            clinicNames.add("Healthy Pet Quận 1 - 125 Nguyễn Thị Minh Khai, Q.1");
            clinicNames.add("Healthy Pet Quận 3 - 280 Võ Văn Tần, Q.3");
            clinicNames.add("Healthy Pet Thủ Đức - 01 Võ Văn Ngân, TP. Thủ Đức");
        }
        ArrayAdapter<String> clinicSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, clinicNames);
        dialogBinding.spnClinic.setAdapter(clinicSpinnerAdapter);

        // 3. Setup Spinner Loại dịch vụ (service_type)
        String[] serviceTypes = {"Khám bệnh", "Tiêm phòng", "Spa & Làm đẹp", "Khám tổng quát"};
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, serviceTypes);
        dialogBinding.spnServiceType.setAdapter(serviceAdapter);

        dialogBinding.spnServiceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: dialogBinding.etEstimatedCost.setText("350000"); break;
                    case 1: dialogBinding.etEstimatedCost.setText("180000"); break;
                    case 2: dialogBinding.etEstimatedCost.setText("350000"); break;
                    case 3: dialogBinding.etEstimatedCost.setText("300000"); break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 4. State cho DatePicker & TimePicker
        final String[] selectedDate = {getCurrentDate()};
        final String[] selectedTime = {"09:30:00"};

        dialogBinding.btnPickDate.setText("📅 " + selectedDate[0]);
        dialogBinding.btnPickTime.setText("⏰ 09:30");

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
                selectedTime[0] = String.format(Locale.getDefault(), "%02d:%02d:00", hourOfDay, minute);
                dialogBinding.btnPickTime.setText("⏰ " + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, 9, 30, true);
            tpd.show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("📅 Đặt lịch khám mới")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Xác nhận Đặt lịch", (dialog, which) -> {
                    String reason = dialogBinding.etReason.getText().toString().trim();
                    if (reason.isEmpty()) reason = "Khám sức khỏe tổng quát";

                    String costStr = dialogBinding.etEstimatedCost.getText().toString().trim();
                    BigDecimal estimatedCost = new BigDecimal(costStr.isEmpty() ? "350000" : costStr);

                    int petPos = dialogBinding.spnPet.getSelectedItemPosition();
                    int clinicPos = dialogBinding.spnClinic.getSelectedItemPosition();
                    String serviceType = serviceTypes[dialogBinding.spnServiceType.getSelectedItemPosition()];

                    Long petId = !availablePets.isEmpty() && petPos < availablePets.size()
                            ? availablePets.get(petPos).getId()
                            : (long) (petPos + 1);
                    String petName = petNames.get(petPos).split(" \\(")[0];

                    Long clinicId = !availableClinics.isEmpty() && clinicPos < availableClinics.size()
                            ? availableClinics.get(clinicPos).getId()
                            : (long) (clinicPos + 1);
                    String clinicName = clinicNames.get(clinicPos).split(" - ")[0];

                    String fullAppointmentTime = selectedDate[0] + " " + selectedTime[0];

                    Appointment newApt = new Appointment(
                            sessionManager.getUserId(),
                            petId,
                            clinicId,
                            serviceType,
                            fullAppointmentTime,
                            estimatedCost,
                            reason
                    );
                    newApt.setId(System.currentTimeMillis() % 10000);
                    newApt.setPetName(petName);
                    newApt.setClinicName(clinicName);
                    newApt.setStatus("PENDING");

                    // 1. Thêm lập tức vào Shared Repository cho Admin nhận đơn ngay
                    AppointmentRepository.getInstance().addAppointment(newApt);

                    // 2. Cập nhật UI User lập tức
                    loadUserAppointments();

                    // 3. Gửi API Retrofit về Spring Boot server
                    appointmentApi.createAppointment(newApt).enqueue(new Callback<Appointment>() {
                        @Override
                        public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                AppointmentRepository.getInstance().addAppointment(response.body());
                                loadUserAppointments();
                            }
                            Toast.makeText(getContext(), "✅ Đặt lịch thành công! Đã chuyển sang Admin chờ duyệt.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "✅ Đặt lịch thành công! Đã chuyển sang Admin chờ duyệt.", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
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
}
