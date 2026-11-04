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
import com.bumptech.glide.Glide;
import com.example.healthy_pet.R;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;
import com.example.healthy_pet.utils.payment.CashPaymentStrategy;
import com.example.healthy_pet.utils.payment.PaymentContext;
import com.example.healthy_pet.utils.payment.PaymentStrategy;
import com.example.healthy_pet.utils.payment.VietQrPaymentStrategy;
import com.google.android.material.button.MaterialButton;
import java.math.BigDecimal;

/**
 * [Thành viên 5] Nghiệp vụ 4: Quản lý Bệnh án, Thanh toán Viện phí (Strategy Pattern) & Thống kê DECIMAL(18, 0)
 */
public class MedicalHistoryFragment extends Fragment {

    private final PaymentContext paymentContext = new PaymentContext();
    private MedicalHistory sampleHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_module_card, container, false);

        sampleHistory = new MedicalHistory(
                101L, 1L, 501L, "2026-10-18",
                "Ngứa tai, lắc đầu liên tục",
                "Viêm ống tai ngoài do nấm Otodectes",
                "Thuốc nhỏ tai Oridermyl 10g (2 lần/ngày)",
                new BigDecimal("350000"),
                "https://res.cloudinary.com/demo/image/upload/prescription_101.jpg",
                "UNPAID",
                ""
        );

        String sampleTotal = CurrencyUtils.formatVND(sampleHistory.getCost());

        ((TextView) view.findViewById(R.id.tvModuleBadge)).setText("NGHIỆP VỤ 4 • STRATEGY PATTERN");
        TextView tvTitle = view.findViewById(R.id.tvModuleTitle);
        tvTitle.setText("📋 Bệnh án & Thanh toán (" + sampleTotal + " - UNPAID)");

        ((TextView) view.findViewById(R.id.tvModuleDescription)).setText(
                "• Tra cứu lịch sử khám chữa bệnh, đơn thuốc và xem ảnh chụp toa thuốc trên Cloudinary.\n" +
                "• Thanh toán viện phí thực tế (DECIMAL 18,0) áp dụng Strategy Pattern (VietQrPaymentStrategy & CashPaymentStrategy)."
        );
        ((TextView) view.findViewById(R.id.tvModuleTechInfo)).setText(
                "Thực thể: MedicalHistories (payment_status, payment_method)\n" +
                "Strategy Pattern: PaymentStrategy -> VietQrPaymentStrategy | CashPaymentStrategy\n" +
                "API: PUT /api/medical-histories/{id}/pay"
        );

        MaterialButton btn = view.findViewById(R.id.btnModuleAction);
        btn.setText("💳 Thanh toán Viện phí (" + sampleTotal + ")");
        btn.setOnClickListener(v -> showPaymentStrategyDialog(tvTitle, btn));

        return view;
    }

    private void showPaymentStrategyDialog(TextView tvTitle, MaterialButton btnAction) {
        PaymentStrategy[] strategies = new PaymentStrategy[]{
                new VietQrPaymentStrategy(),
                new CashPaymentStrategy()
        };
        String[] titles = new String[]{
                "🏦 " + strategies[0].getDisplayName(),
                "💵 " + strategies[1].getDisplayName()
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("💳 Chọn phương thức thanh toán")
                .setItems(titles, (dialog, which) -> {
                    paymentContext.setStrategy(strategies[which]);
                    showPaymentPreviewAndConfirmDialog(tvTitle, btnAction);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showPaymentPreviewAndConfirmDialog(TextView tvTitle, MaterialButton btnAction) {
        PaymentStrategy currentStrategy = paymentContext.getStrategy();
        String previewInfo = paymentContext.getPreviewInfo(sampleHistory);
        String amountFormatted = CurrencyUtils.formatVND(sampleHistory.getCost());

        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        if ("VIETQR".equals(currentStrategy.getMethodCode())) {
            TextView tvGuide = new TextView(requireContext());
            tvGuide.setText("Quét mã VietQR bên dưới bằng ứng dụng Ngân hàng:\n• Số tiền: "
                    + amountFormatted + "\n• Nội dung CK: HPET" + sampleHistory.getId());
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

            // Tạo sẵn Bitmap QR sắc nét làm placeholder ngay lập tức và tải ảnh thật từ img.vietqr.io bằng Glide
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
            tvCashGuide.setText("💵 " + previewInfo + "\n\n• Mã hóa đơn: #HPET" + sampleHistory.getId()
                    + "\n• Tổng thu: " + amountFormatted);
            tvCashGuide.setTextSize(14f);
            container.addView(tvCashGuide);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(currentStrategy.getDisplayName())
                .setView(container)
                .setPositiveButton("✓ Xác nhận đã thanh toán", (d, w) -> {
                    String resultMsg = paymentContext.processPayment(requireContext(), sampleHistory);
                    tvTitle.setText("📋 Bệnh án & Thanh toán (ĐÃ TT - " + sampleHistory.getPaymentMethod() + ")");
                    btnAction.setText("✅ Đã thanh toán (" + sampleHistory.getPaymentMethod() + ")");
                    Toast.makeText(getContext(), resultMsg, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    /**
     * Vẽ mã QR Bitmap dự phòng sắc nét để hiển thị ngay lập tức cả khi đang tải hoặc không có mạng.
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
