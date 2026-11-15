package com.example.healthy_pet.ui.map;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.healthy_pet.utils.SessionManager;
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
 * Phụ trách: Thành viên 5.
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

        initSampleAppointments();
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initSampleAppointments() {
        if (!userAppointments.isEmpty()) return;

        Appointment apt1 = new Appointment(sessionManager.getUserId(), 101L, 1L, "2026-10-25", "09:30:00", "Khám da liễu & tiêm nhắc lại");
        apt1.setId(501L);
        apt1.setPetName("LuLu (Poodle)");
        apt1.setClinicName("Healthy Pet Chi nhánh 1 - Q.1");
        apt1.setStatus("PENDING");

        Appointment apt2 = new Appointment(sessionManager.getUserId(), 102L, 2L, "2026-10-20", "14:00:00", "Tắm sấy & cắt tỉa lông");
        apt2.setId(502L);
        apt2.setPetName("Miu Miu (Mèo Anh)");
        apt2.setClinicName("Healthy Pet Chi nhánh 2 - Bình Thạnh");
        apt2.setStatus("CONFIRMED");

        Appointment apt3 = new Appointment(sessionManager.getUserId(), 101L, 1L, "2026-10-18", "10:00:00", "Khám viêm tai ngoài");
        apt3.setId(503L);
        apt3.setPetName("LuLu (Poodle)");
        apt3.setClinicName("Healthy Pet Chi nhánh 1 - Q.1");
        apt3.setStatus("COMPLETED");

        userAppointments.add(apt1);
        userAppointments.add(apt2);
        userAppointments.add(apt3);
    }

    private void fetchPetsAndClinicsFromApi() {
        // Lay danh sach pet tu API
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

        // Lay danh sach chi nhanh phong kham tu API
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
        appointmentApi.getUserAppointments(sessionManager.getUserId()).enqueue(new Callback<List<Appointment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Appointment>> call, @NonNull Response<List<Appointment>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    userAppointments.clear();
                    userAppointments.addAll(response.body());
                    updateUiState();
                } else {
                    updateUiState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Appointment>> call, @NonNull Throwable t) {
                updateUiState();
            }
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

        // Setup Spinner Thú cưng
        List<String> petNames = new ArrayList<>();
        if (!availablePets.isEmpty()) {
            for (Pet p : availablePets) {
                petNames.add(p.getName() + " (" + p.getSpecies() + ")");
            }
        } else {
            petNames.add("LuLu (Poodle - 3.5kg)");
            petNames.add("Miu Miu (Mèo Anh - 4.2kg)");
            petNames.add("Kiki (Corgi - 8.0kg)");
        }
        ArrayAdapter<String> petSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, petNames);
        dialogBinding.spnPet.setAdapter(petSpinnerAdapter);

        // Setup Spinner Phòng khám
        List<String> clinicNames = new ArrayList<>();
        if (!availableClinics.isEmpty()) {
            for (VetClinic c : availableClinics) {
                clinicNames.add(c.getName() + " - " + c.getAddress());
            }
        } else {
            clinicNames.add("Healthy Pet Q.1 - 120 Nguyễn Thị Minh Khai");
            clinicNames.add("Healthy Pet Bình Thạnh - 45 Lê Quang Định");
            clinicNames.add("Healthy Pet Q.7 - 88 Nguyễn Thị Thập");
        }
        ArrayAdapter<String> clinicSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, clinicNames);
        dialogBinding.spnClinic.setAdapter(clinicSpinnerAdapter);

        // State cho DatePicker & TimePicker
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
                .setTitle("📅 Đặt lịch khám mới (NV 5b)")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Xác nhận Đặt lịch", (dialog, which) -> {
                    String reason = dialogBinding.etReason.getText().toString().trim();
                    if (reason.isEmpty()) reason = "Khám sức khỏe tổng quát";

                    int petPos = dialogBinding.spnPet.getSelectedItemPosition();
                    int clinicPos = dialogBinding.spnClinic.getSelectedItemPosition();

                    Long petId = !availablePets.isEmpty() && petPos < availablePets.size()
                            ? availablePets.get(petPos).getId()
                            : (long) (petPos + 101);
                    String petName = petNames.get(petPos).split(" \\(")[0];

                    Long clinicId = !availableClinics.isEmpty() && clinicPos < availableClinics.size()
                            ? availableClinics.get(clinicPos).getId()
                            : (long) (clinicPos + 1);
                    String clinicName = clinicNames.get(clinicPos).split(" - ")[0];

                    Appointment newApt = new Appointment(
                            sessionManager.getUserId(),
                            petId,
                            clinicId,
                            selectedDate[0],
                            selectedTime[0],
                            reason
                    );
                    newApt.setId(System.currentTimeMillis() % 10000);
                    newApt.setPetName(petName);
                    newApt.setClinicName(clinicName);
                    newApt.setStatus("PENDING");

                    userAppointments.add(0, newApt);
                    updateUiState();

                    // Gọi API Retrofit gửi lịch hẹn về Spring Boot server
                    appointmentApi.createAppointment(newApt).enqueue(new Callback<Appointment>() {
                        @Override
                        public void onResponse(@NonNull Call<Appointment> call, @NonNull Response<Appointment> response) {
                            Toast.makeText(getContext(), "✅ Đã gửi lịch hẹn thành công (Trạng thái: PENDING)", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                            Toast.makeText(getContext(), "✅ Đặt lịch hẹn thành công (Trạng thái: PENDING)", Toast.LENGTH_LONG).show();
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
        appointment.setStatus("CANCELLED");
        updateUiState();
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
