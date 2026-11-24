package com.example.healthy_pet.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.healthy_pet.databinding.ItemAppointmentBinding;
import com.example.healthy_pet.models.Appointment;
import com.example.healthy_pet.utils.CurrencyUtils;
import java.util.List;

/**
 * Adapter hiển thị danh sách Lịch khám (Appointments) cho cả User (NV 5b) và Admin (NV 7).
 * Phụ trách: Thành viên 5.
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    public interface OnAppointmentActionListener {
        void onApprove(Appointment appointment);
        void onReject(Appointment appointment);
        void onComplete(Appointment appointment);
        void onCancel(Appointment appointment);
    }

    private List<Appointment> appointmentList;
    private final boolean isAdminMode;
    private final OnAppointmentActionListener listener;

    public AppointmentAdapter(List<Appointment> appointmentList, boolean isAdminMode, OnAppointmentActionListener listener) {
        this.appointmentList = appointmentList;
        this.isAdminMode = isAdminMode;
        this.listener = listener;
    }

    public void updateData(List<Appointment> newList) {
        this.appointmentList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppointmentBinding binding = ItemAppointmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new AppointmentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment item = appointmentList.get(position);
        holder.bind(item, isAdminMode, listener);
    }

    @Override
    public int getItemCount() {
        return appointmentList != null ? appointmentList.size() : 0;
    }

    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppointmentBinding binding;

        public AppointmentViewHolder(@NonNull ItemAppointmentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Appointment item, boolean isAdminMode, OnAppointmentActionListener listener) {
            String petDisplayName = item.getPetName() != null && !item.getPetName().isEmpty()
                    ? item.getPetName()
                    : "Thú cưng #" + item.getPetId();
            binding.tvPetName.setText("🐾 Ca #" + item.getId() + " • " + petDisplayName);

            binding.tvServiceType.setText("🏷️ Dịch vụ: " + item.getServiceType());

            String clinicDisplayName = item.getClinicName() != null && !item.getClinicName().isEmpty()
                    ? item.getClinicName()
                    : "Chi nhánh #" + item.getClinicId();
            binding.tvClinicName.setText("🏥 Chi nhánh: " + clinicDisplayName);

            String timeStr = item.getAppointmentTime() != null ? item.getAppointmentTime() : "";
            binding.tvDateTime.setText("📅 Thời gian: " + timeStr);

            String costDisplay;
            if (item.getEstimatedCost() != null && item.getEstimatedCost().compareTo(java.math.BigDecimal.ZERO) > 0) {
                costDisplay = CurrencyUtils.formatVND(item.getEstimatedCost());
            } else if (item.getServiceType() != null && item.getServiceType().toLowerCase().contains("chích ngừa")) {
                costDisplay = "Giá tùy theo loại mũi tiêm";
            } else {
                costDisplay = "Giá tùy theo thú cưng";
            }
            binding.tvEstimatedCost.setText("💰 Chi phí dự kiến: " + costDisplay);

            String noteStr = item.getNote() != null ? item.getNote() : "";
            binding.tvReason.setText("📝 Ghi chú: " + noteStr);

            configureStatusBadge(binding.tvStatusBadge, item.getStatus());

            String status = item.getStatus() != null ? item.getStatus() : "PENDING";

            if (isAdminMode) {
                if ("PENDING".equals(status)) {
                    binding.layoutActions.setVisibility(View.VISIBLE);
                    binding.btnApprove.setVisibility(View.VISIBLE);
                    binding.btnReject.setVisibility(View.VISIBLE);
                    binding.btnReject.setText("❌ Từ chối");
                    binding.btnComplete.setVisibility(View.GONE);

                    binding.btnApprove.setOnClickListener(v -> {
                        if (listener != null) listener.onApprove(item);
                    });
                    binding.btnReject.setOnClickListener(v -> {
                        if (listener != null) listener.onReject(item);
                    });
                } else if ("CONFIRMED".equals(status)) {
                    binding.layoutActions.setVisibility(View.VISIBLE);
                    binding.btnApprove.setVisibility(View.GONE);
                    binding.btnReject.setVisibility(View.GONE);
                    binding.btnComplete.setVisibility(View.VISIBLE);

                    binding.btnComplete.setOnClickListener(v -> {
                        if (listener != null) listener.onComplete(item);
                    });
                } else {
                    binding.layoutActions.setVisibility(View.GONE);
                }
            } else {
                // User mode
                if ("PENDING".equals(status) || "CONFIRMED".equals(status)) {
                    binding.layoutActions.setVisibility(View.VISIBLE);
                    binding.btnApprove.setVisibility(View.GONE);
                    binding.btnComplete.setVisibility(View.GONE);
                    binding.btnReject.setVisibility(View.VISIBLE);
                    binding.btnReject.setText("❌ Hủy lịch hẹn");

                    binding.btnReject.setOnClickListener(v -> {
                        if (listener != null) listener.onCancel(item);
                    });
                } else {
                    binding.layoutActions.setVisibility(View.GONE);
                }
            }
        }

        private void configureStatusBadge(TextView tvStatus, String status) {
            String currentStatus = status != null ? status : "PENDING";
            switch (currentStatus) {
                case "PENDING":
                    tvStatus.setText("CHỜ DUYỆT");
                    tvStatus.setTextColor(Color.parseColor("#B45309"));
                    tvStatus.setBackgroundColor(Color.parseColor("#FEF3C7"));
                    break;
                case "CONFIRMED":
                    tvStatus.setText("ĐÃ DUYỆT");
                    tvStatus.setTextColor(Color.parseColor("#1D4ED8"));
                    tvStatus.setBackgroundColor(Color.parseColor("#DBEAFE"));
                    break;
                case "COMPLETED":
                    tvStatus.setText("ĐÃ KHÁM & KÊ ĐƠN");
                    tvStatus.setTextColor(Color.parseColor("#15803D"));
                    tvStatus.setBackgroundColor(Color.parseColor("#DCFCE7"));
                    break;
                case "CANCELLED":
                    tvStatus.setText("ĐÃ HỦY");
                    tvStatus.setTextColor(Color.parseColor("#B91C1C"));
                    tvStatus.setBackgroundColor(Color.parseColor("#FEE2E2"));
                    break;
                default:
                    tvStatus.setText(currentStatus);
                    break;
            }
        }
    }
}
