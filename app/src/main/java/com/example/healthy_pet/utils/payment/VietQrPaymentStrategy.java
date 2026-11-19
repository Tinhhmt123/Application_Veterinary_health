package com.example.healthy_pet.utils.payment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;
import java.math.BigDecimal;

/**
 * [Concrete Strategy 1] Chiến lược thanh toán chuyển khoản Ngân hàng qua mã VietQR động.
 * Tự động tạo link ảnh QR từ img.vietqr.io khớp với số tiền cost DECIMAL(18, 0) để tải bằng Glide.
 * Tự build View preview (QR code + thông tin ngân hàng) theo đúng Strategy Pattern.
 */
public class VietQrPaymentStrategy implements PaymentStrategy {

    private static final String BANK_ID = "VCB";
    private static final String ACCOUNT_NO = "1031183832";
    private static final String ACCOUNT_NAME = "HEALTHY PET";

    @Override
    public String getMethodCode() {
        return "VIETQR";
    }

    @Override
    public String getDisplayName() {
        return "Chuyển khoản Ngân hàng (Mã VietQR)";
    }

    @Override
    public String buildPaymentInfoOrQrUrl(MedicalHistory medicalHistory) {
        BigDecimal cost = (medicalHistory != null && medicalHistory.getCost() != null)
                ? medicalHistory.getCost()
                : BigDecimal.ZERO;
        Long id = (medicalHistory != null && medicalHistory.getId() != null)
                ? medicalHistory.getId()
                : 101L;
        String addInfo = "HPET" + id;

        return "https://img.vietqr.io/image/" + BANK_ID + "-" + ACCOUNT_NO
                + "-compact2.png?amount=" + cost.toPlainString()
                + "&addInfo=" + addInfo
                + "&accountName=" + ACCOUNT_NAME;
    }

    @Override
    public View buildPreviewView(Fragment fragment, MedicalHistory medicalHistory) {
        Context context = fragment.requireContext();
        String amountFormatted = CurrencyUtils.formatVND(medicalHistory.getCost());
        String qrUrl = buildPaymentInfoOrQrUrl(medicalHistory);
        float density = context.getResources().getDisplayMetrics().density;
        int pad = (int) (18 * density);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(pad, pad / 2, pad, pad / 2);

        // Hướng dẫn quét mã
        TextView tvGuide = new TextView(context);
        tvGuide.setText("Quét mã VietQR bên dưới bằng ứng dụng Ngân hàng:\n• Số tiền: "
                + amountFormatted + "\n• Nội dung CK: HPET" + medicalHistory.getId());
        tvGuide.setTextSize(14f);
        tvGuide.setPadding(0, 0, 0, pad / 2);
        container.addView(tvGuide);

        // Ảnh QR code
        ImageView ivQr = new ImageView(context);
        int qrSize = (int) (240 * density);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrParams.gravity = Gravity.CENTER_HORIZONTAL;
        ivQr.setLayoutParams(qrParams);
        ivQr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivQr.setBackgroundColor(Color.WHITE);
        int qrPad = (int) (8 * density);
        ivQr.setPadding(qrPad, qrPad, qrPad, qrPad);

        Bitmap fallbackQr = createFallbackQrBitmap(400);
        ivQr.setImageBitmap(fallbackQr);
        Glide.with(fragment)
                .load(qrUrl)
                .placeholder(ivQr.getDrawable())
                .error(ivQr.getDrawable())
                .into(ivQr);
        container.addView(ivQr);

        // Thông tin ngân hàng
        TextView tvBankInfo = new TextView(context);
        tvBankInfo.setText("Vietcombank (VCB) • " + ACCOUNT_NO + " • " + ACCOUNT_NAME);
        tvBankInfo.setTextSize(12f);
        tvBankInfo.setGravity(Gravity.CENTER);
        tvBankInfo.setPadding(0, pad / 2, 0, 0);
        container.addView(tvBankInfo);

        return container;
    }

    @Override
    public String executePayment(Context context, MedicalHistory medicalHistory) {
        if (medicalHistory != null) {
            medicalHistory.setPaymentStatus("PAID");
            medicalHistory.setPaymentMethod(getMethodCode());
            return "Đã thanh toán " + CurrencyUtils.formatVND(medicalHistory.getCost())
                    + " thành công qua Mã VietQR (Nội dung CK: HPET" + medicalHistory.getId() + ")";
        }
        return "Đã hoàn thành thanh toán VietQR!";
    }

    // ── Fallback QR bitmap ──────────────────────────────────────

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

