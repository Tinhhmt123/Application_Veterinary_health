package com.example.healthy_pet.ui.medical;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.example.healthy_pet.adapters.MedicalHistoryAdapter;
import com.example.healthy_pet.api.AppointmentMedicalApi;
import com.example.healthy_pet.api.RetrofitClient;
import com.example.healthy_pet.databinding.FragmentMedicalHistoryBinding;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;
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
 * [Thành viên 5 / Người 5] Nghiệp vụ 4: Xem Bệnh án, Thống kê DECIMAL(18, 0)
 * & Thanh toán Viện phí áp dụng Strategy Pattern (VietQR & Tiền mặt - Không MoMo).
 * Phụ trách: Thành viên 5.
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
        initSampleHistories();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMedicalHistoryBinding.inflate(inflater, container, false);

        binding.rvMedicalHistories.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MedicalHistoryAdapter(medicalHistories, this);
        binding.rvMedicalHistories.setAdapter(adapter);

        binding.btnRefresh.setOnClickListener(v -> loadMedicalHistoriesAndStats());

        loadMedicalHistoriesAndStats();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initSampleHistories() {
        if (!medicalHistories.isEmpty()) return;

        MedicalHistory h1 = new MedicalHistory(
                101L, 503L, 101L, "2026-10-18",
                "Ngứa tai, lắc đầu liên tục",
                "Viêm ống tai ngoài do nấm Otodectes",
                "Thuốc nhỏ tai Oridermyl 10g (2 lần/ngày)",
                new BigDecimal("350000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_101.jpg",
                "UNPAID",
                ""
        );
        h1.setPetName("LuLu (Poodle)");

        MedicalHistory h2 = new MedicalHistory(
                102L, 502L, 102L, "2026-10-12",
                "Biếng ăn, nôn dịch vàng, sốt nhẹ",
                "Viêm đường tiêu hóa do vi khuẩn",
                "Thuốc Smecta 3g + Men vi sinh Bio-Flora + Điện giải",
                new BigDecimal("450000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_102.jpg",
                "PAID",
                "VIETQR"
        );
        h2.setPetName("Miu Miu (Mèo Anh)");

        MedicalHistory h3 = new MedicalHistory(
                103L, 501L, 103L, "2026-10-05",
                "Tiêm ngừa định kỳ & kiểm tra da",
                "Thú cưng hoàn toàn khỏe mạnh",
                "Vắc xin Rabies 1ml + Vitamin tổng hợp",
                new BigDecimal("450000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_103.jpg",
                "PAID",
                "CASH"
        );
        h3.setPetName("Kiki (Corgi)");

        medicalHistories.add(h1);
        medicalHistories.add(h2);
        medicalHistories.add(h3);
    }

    private void loadMedicalHistoriesAndStats() {
        Long userId = sessionManager.getUserId();

        // 1. Fetch histories from API
        appointmentApi.getMedicalHistoriesByUser(userId).enqueue(new Callback<List<MedicalHistory>>() {
            @Override
            public void onResponse(@NonNull Call<List<MedicalHistory>> call, @NonNull Response<List<MedicalHistory>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    medicalHistories.clear();
                    medicalHistories.addAll(response.body());
                }
                updateUiState();
            }

            @Override
            public void onFailure(@NonNull Call<List<MedicalHistory>> call, @NonNull Throwable t) {
                updateUiState();
            }
        });

        // 2. Fetch total cost stat from API
        appointmentApi.getTotalMedicalCost(userId).enqueue(new Callback<Map<String, BigDecimal>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, BigDecimal>> call, @NonNull Response<Map<String, BigDecimal>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().containsKey("totalCost")) {
                    BigDecimal apiTotal = response.body().get("totalCost");
                    if (binding != null && apiTotal != null) {
                        binding.tvTotalStats.setText("📋 Bệnh án & Tổng viện phí (" + CurrencyUtils.formatVND(apiTotal) + ")");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, BigDecimal>> call, @NonNull Throwable t) {}
        });
    }

    private void updateUiState() {
        if (binding == null) return;

        BigDecimal localSum = BigDecimal.ZERO;
        for (MedicalHistory item : medicalHistories) {
            if (item.getCost() != null) {
                localSum = localSum.add(item.getCost());
            }
        }
        binding.tvTotalStats.setText("📋 Bệnh án & Tổng viện phí (" + CurrencyUtils.formatVND(localSum) + ")");

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
     * Mở Dialog Chọn phương thức thanh toán áp dụng Strategy Pattern (VietQR & Tiền mặt - Không MoMo)
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
                .setTitle("💳 Phương thức thanh toán (Strategy Pattern)")
                .setItems(titles, (dialog, which) -> {
                    paymentContext.setStrategy(strategies[which]);
                    showPaymentPreviewAndConfirmDialog(history);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showPaymentPreviewAndConfirmDialog(MedicalHistory history) {
        PaymentStrategy currentStrategy = paymentContext.getStrategy();
        String previewInfo = paymentContext.getPreviewInfo(history);
        String amountFormatted = CurrencyUtils.formatVND(history.getCost());

        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        if ("VIETQR".equals(currentStrategy.getMethodCode())) {
            TextView tvGuide = new TextView(requireContext());
            tvGuide.setText("Quét mã VietQR bên dưới bằng ứng dụng Ngân hàng:\n• Số tiền: "
                    + amountFormatted + "\n• Nội dung CK: HPET" + history.getId());
            tvGuide.setTextSize(14f);
            tvGuide.setPadding(0, 0, 0, pad / 2);
            container.addView(tvGuide);

            ImageView ivQr = new ImageView(requireContext());
            int qrSize = (int) (240 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
            qrParams.gravity = Gravity.CENTER_HORIZONTAL;
            ivQr.setLayoutParams(qrParams);
            ivQr.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivQr.setBackgroundColor(Color.WHITE);
            int qrPad = (int) (8 * getResources().getDisplayMetrics().density);
            ivQr.setPadding(qrPad, qrPad, qrPad, qrPad);

            Bitmap fallbackQr = createFallbackQrBitmap(400);
            ivQr.setImageBitmap(fallbackQr);
            Glide.with(this)
                    .load(previewInfo)
                    .placeholder(ivQr.getDrawable())
                    .error(ivQr.getDrawable())
                    .into(ivQr);

            container.addView(ivQr);

            TextView tvBankInfo = new TextView(requireContext());
            tvBankInfo.setText("Vietcombank (VCB) • 1031183832 • HEALTHY PET");
            tvBankInfo.setTextSize(12f);
            tvBankInfo.setGravity(Gravity.CENTER);
            tvBankInfo.setPadding(0, pad / 2, 0, 0);
            container.addView(tvBankInfo);
        } else {
            TextView tvCashGuide = new TextView(requireContext());
            tvCashGuide.setText("💵 " + previewInfo + "\n\n• Mã hóa đơn: #HPET" + history.getId()
                    + "\n• Số tiền cần nộp: " + amountFormatted);
            tvCashGuide.setTextSize(14f);
            container.addView(tvCashGuide);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(currentStrategy.getDisplayName())
                .setView(container)
                .setPositiveButton("✓ Xác nhận đã thanh toán", (d, w) -> {
                    String resultMsg = paymentContext.processPayment(requireContext(), history);
                    updateUiState();
                    Toast.makeText(getContext(), resultMsg, Toast.LENGTH_LONG).show();

                    // Gọi API Retrofit cập nhật trạng thái thanh toán lên cơ sở dữ liệu
                    appointmentApi.payMedicalHistory(history.getId(), history.getPaymentMethod()).enqueue(new Callback<MedicalHistory>() {
                        @Override
                        public void onResponse(@NonNull Call<MedicalHistory> call, @NonNull Response<MedicalHistory> response) {}

                        @Override
                        public void onFailure(@NonNull Call<MedicalHistory> call, @NonNull Throwable t) {}
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    /**
     * Vẽ mã QR Bitmap dự phòng bằng Canvas API khi offline.
     */
    private Bitmap createFallbackQrBitmap(int sizePx) {
        int grid = 21;
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#0F172A"));
        float cell = (float) sizePx / (grid + 2);

        boolean[][] pattern = new boolean[grid][grid];
        drawFinderPattern(pattern, 0, 0);
        drawFinderPattern(pattern, grid - 7, 0);
        drawFinderPattern(pattern, 0, grid - 7);

        for (int r = 0; r < grid; r++) {
            for (int c = 0; c < grid; c++) {
                boolean inFinder = (r < 8 && c < 8) || (r < 8 && c >= grid - 8) || (r >= grid - 8 && c < 8);
                if (!inFinder) {
                    pattern[r][c] = ((r * 7 + c * 13 + r * c) % 3 == 0) || (r == 6 || c == 6) && ((r + c) % 2 == 0);
                }
                if (pattern[r][c]) {
                    float left = (c + 1) * cell;
                    float top = (r + 1) * cell;
                    canvas.drawRect(left, top, left + cell, top + cell, paint);
                }
            }
        }
        return bmp;
    }

    private void drawFinderPattern(boolean[][] pattern, int startC, int startR) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
                    pattern[startR + r][startC + c] = true;
                }
            }
        }
    }
}
