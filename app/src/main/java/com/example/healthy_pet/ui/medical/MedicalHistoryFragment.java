package com.example.healthy_pet.ui.medical;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.healthy_pet.adapters.MedicalHistoryAdapter;
import com.example.healthy_pet.api.AppointmentMedicalApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.databinding.FragmentMedicalHistoryBinding;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.repository.MedicalHistoryRepository;
import com.example.healthy_pet.utils.CurrencyUtils;
import com.example.healthy_pet.utils.InfoHintHelper;
import com.example.healthy_pet.utils.SessionManager;
import com.example.healthy_pet.utils.payment.CashPaymentStrategy;
import com.example.healthy_pet.utils.payment.PaymentContext;
import com.example.healthy_pet.utils.payment.PaymentStrategy;
import com.example.healthy_pet.utils.payment.VietQrPaymentStrategy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [Thành viên 5] Nghiệp vụ 4: Xem Bệnh án, Thống kê Chi phí
 * & Thanh toán Viện phí áp dụng Strategy Pattern (VietQR & Tiền mặt).
 * Đồng bộ lập tức với MedicalHistoryRepository.
 */
public class MedicalHistoryFragment extends Fragment implements MedicalHistoryAdapter.OnPayClickListener {

    private FragmentMedicalHistoryBinding binding;
    private MedicalHistoryAdapter adapter;
    private final PaymentContext paymentContext = new PaymentContext();
    private final List<MedicalHistory> medicalHistories = new ArrayList<>();
    private AppointmentMedicalApi appointmentApi;
    private SessionManager sessionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        appointmentApi = RetrofitClient.getAppointmentMedicalApi();
        MedicalHistoryRepository.getInstance().initContext(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMedicalHistoryBinding.inflate(inflater, container, false);

        binding.rvMedicalHistories.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MedicalHistoryAdapter(medicalHistories, this);
        binding.rvMedicalHistories.setAdapter(adapter);

        boolean isAdminRole = "ADMIN".equalsIgnoreCase(sessionManager.getRole());
        InfoHintHelper.attach(binding.btnInfoMedical, binding.tvModuleDescription.getText(), isAdminRole);
        binding.btnRefresh.setOnClickListener(v -> loadMedicalHistoriesAndStats());

        loadMedicalHistoriesAndStats();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMedicalHistoriesAndStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadMedicalHistoriesAndStats() {
        Long userId = sessionManager.getUserId();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole());

        // 1. Tải lập tức từ Shared MedicalHistoryRepository
        medicalHistories.clear();
        if (isAdmin) {
            medicalHistories.addAll(MedicalHistoryRepository.getInstance().getMedicalHistoriesByUser(null));
        } else {
            medicalHistories.addAll(MedicalHistoryRepository.getInstance().getMedicalHistoriesByUser(userId));
        }
        updateUiState();

        // 2. Đồng bộ chuẩn 100% từ Backend REST API (một nguồn dữ liệu duy nhất, không còn xung đột 2 API)
        appointmentApi.getMedicalHistoriesByUser(userId).enqueue(new Callback<List<MedicalHistory>>() {
            @Override
            public void onResponse(@NonNull Call<List<MedicalHistory>> call, @NonNull Response<List<MedicalHistory>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    if (isAdmin) {
                        MedicalHistoryRepository.getInstance().setHistoriesForUser(null, response.body());
                        medicalHistories.clear();
                        medicalHistories.addAll(MedicalHistoryRepository.getInstance().getMedicalHistoriesByUser(null));
                    } else {
                        MedicalHistoryRepository.getInstance().setHistoriesForUser(userId, response.body());
                        medicalHistories.clear();
                        medicalHistories.addAll(MedicalHistoryRepository.getInstance().getMedicalHistoriesByUser(userId));
                    }
                    updateUiState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<MedicalHistory>> call, @NonNull Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Không thể đồng bộ bệnh án từ máy chủ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUiState() {
        if (binding == null) return;

        BigDecimal totalAll = BigDecimal.ZERO;
        BigDecimal totalUnpaid = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;

        for (MedicalHistory h : medicalHistories) {
            if (h == null || h.getCost() == null) continue;
            totalAll = totalAll.add(h.getCost());
            String st = h.getPaymentStatus() != null ? h.getPaymentStatus().trim().toUpperCase() : "UNPAID";
            if ("PAID".equals(st)) {
                totalPaid = totalPaid.add(h.getCost());
            } else {
                totalUnpaid = totalUnpaid.add(h.getCost());
            }
        }

        if (!medicalHistories.isEmpty() && totalUnpaid.compareTo(BigDecimal.ZERO) == 0) {
            binding.tvTotalStats.setText("0 đ (Đã thanh toán hết)");
        } else {
            binding.tvTotalStats.setText("Cần thanh toán: " + CurrencyUtils.formatVND(totalUnpaid));
        }

        binding.tvModuleDescription.setText(
                "✅ Đã thanh toán: " + CurrencyUtils.formatVND(totalPaid)
                        + "  |  📊 Tổng chi phí tích lũy: " + CurrencyUtils.formatVND(totalAll)
                        + "\nTra cứu lịch sử bệnh án, đơn thuốc của bác sĩ và thanh toán viện phí qua VietQR hoặc Tiền mặt."
        );

        if (medicalHistories.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvMedicalHistories.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvMedicalHistories.setVisibility(View.VISIBLE);
            adapter.updateData(medicalHistories);
        }
    }

    @Override
    public void onPay(MedicalHistory history) {
        showPaymentStrategyDialog(history);
    }

    /**
     * Mở Dialog Chọn phương thức thanh toán áp dụng Strategy Pattern (VietQR & Tiền mặt)
     */
    private void showPaymentStrategyDialog(MedicalHistory history) {
        PaymentStrategy[] strategies = new PaymentStrategy[]{
                new VietQrPaymentStrategy(),
                new CashPaymentStrategy()
        };
        String[] titles = new String[]{
                "🏦 " + strategies[0].getDisplayName(),
                "💵 " + strategies[1].getDisplayName()
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("💳 Phương thức thanh toán")
                .setItems(titles, (dialog, which) -> {
                    paymentContext.setStrategy(strategies[which]);
                    showPaymentPreviewAndConfirmDialog(history);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    /**
     * [Strategy Pattern] Delegate hoàn toàn việc build UI preview cho Strategy.
     * Fragment KHÔNG còn if/else kiểm tra methodCode — đúng chuẩn Strategy Pattern.
     */
    private void showPaymentPreviewAndConfirmDialog(MedicalHistory history) {
        PaymentStrategy currentStrategy = paymentContext.getStrategy();

        // Mỗi Strategy tự build View preview riêng (QR code / hướng dẫn tiền mặt)
        View previewView = currentStrategy.buildPreviewView(this, history);

        new AlertDialog.Builder(requireContext())
                .setTitle(currentStrategy.getDisplayName())
                .setView(previewView)
                .setPositiveButton("✓ Xác nhận đã thanh toán", (d, w) -> {
                    String resultMsg = paymentContext.processPayment(requireContext(), history);
                    MedicalHistoryRepository.getInstance().payMedicalHistory(history.getId(), history.getPaymentMethod());
                    updateUiState();
                    Toast.makeText(getContext(), resultMsg, Toast.LENGTH_LONG).show();

                    appointmentApi.payMedicalHistory(history.getId(), history.getPaymentMethod()).enqueue(new Callback<MedicalHistory>() {
                        @Override
                        public void onResponse(@NonNull Call<MedicalHistory> call, @NonNull Response<MedicalHistory> response) {
                            if (isAdded() && binding != null) {
                                loadMedicalHistoriesAndStats();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<MedicalHistory> call, @NonNull Throwable t) {
                            if (isAdded() && binding != null) {
                                updateUiState();
                            }
                        }
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }
}
