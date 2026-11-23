package com.example.healthy_pet.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.healthy_pet.R;
import com.example.healthy_pet.databinding.ItemMedicalHistoryBinding;
import com.example.healthy_pet.models.MedicalHistory;
import com.example.healthy_pet.utils.CurrencyUtils;
import java.util.List;

/**
 * Adapter hiển thị danh sách Bệnh án & Lịch sử khám (MedicalHistory) cho User (NV 4).
 * Phụ trách: Thành viên 5.
 */
public class MedicalHistoryAdapter extends RecyclerView.Adapter<MedicalHistoryAdapter.MedicalHistoryViewHolder> {

    public interface OnPayClickListener {
        void onPay(MedicalHistory history);
    }

    private List<MedicalHistory> historyList;
    private final OnPayClickListener payListener;

    public MedicalHistoryAdapter(List<MedicalHistory> historyList, OnPayClickListener payListener) {
        this.historyList = historyList;
        this.payListener = payListener;
    }

    public void updateData(List<MedicalHistory> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicalHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMedicalHistoryBinding binding = ItemMedicalHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new MedicalHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicalHistoryViewHolder holder, int position) {
        MedicalHistory item = historyList.get(position);
        holder.bind(item, payListener);
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    public static class MedicalHistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemMedicalHistoryBinding binding;

        public MedicalHistoryViewHolder(@NonNull ItemMedicalHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MedicalHistory item, OnPayClickListener payListener) {
            String petDisplayName = item.getPetName() != null && !item.getPetName().isEmpty()
                    ? item.getPetName()
                    : "Thú cưng #" + item.getPetId();
            String dateStr = item.getVisitDate() != null ? item.getVisitDate() : "";
            binding.tvPetNameAndDate.setText("🐶 " + petDisplayName + " • " + dateStr);

            binding.tvSymptoms.setText("🤒 Triệu chứng: " + item.getSymptoms());
            binding.tvDiagnosis.setText("🩺 Chẩn đoán: " + item.getDiagnosis());
            binding.tvPrescription.setText("💊 Đơn thuốc: " + item.getPrescription());
            binding.tvCost.setText("💰 Viện phí DECIMAL(18,0): " + CurrencyUtils.formatVND(item.getCost()));

            configurePaymentBadge(binding.tvPaymentBadge, item.getPaymentStatus(), item.getPaymentMethod());

            if (item.getPrescriptionUrl() != null && !item.getPrescriptionUrl().trim().isEmpty()) {
                binding.ivPrescription.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(item.getPrescriptionUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(binding.ivPrescription);
            } else {
                binding.ivPrescription.setVisibility(View.GONE);
            }

            if ("UNPAID".equals(item.getPaymentStatus())) {
                binding.btnPay.setVisibility(View.VISIBLE);
                binding.btnPay.setText("💳 Thanh toán Viện phí (" + CurrencyUtils.formatVND(item.getCost()) + ")");
                binding.btnPay.setOnClickListener(v -> {
                    if (payListener != null) payListener.onPay(item);
                });
            } else {
                binding.btnPay.setVisibility(View.GONE);
            }
        }

        private void configurePaymentBadge(TextView badge, String status, String method) {
            badge.setPadding(16, 6, 16, 6);
            badge.setTextSize(12f);
            if ("PAID".equals(status)) {
                String methodText = "VIETQR".equals(method) ? "MÃ VIETQR" : "TIỀN MẶT";
                badge.setText("ĐÃ TT (" + methodText + ")");
                badge.setTextColor(Color.parseColor("#15803D"));
                badge.setBackgroundColor(Color.parseColor("#DCFCE7"));
            } else {
                badge.setText("CHƯA THANH TOÁN");
                badge.setTextColor(Color.parseColor("#B91C1C"));
                badge.setBackgroundColor(Color.parseColor("#FEE2E2"));
            }
        }
    }
}
