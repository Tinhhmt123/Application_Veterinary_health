package com.example.healthy_pet.api;

import com.example.healthy_pet.models.Appointment;
import com.example.healthy_pet.models.MedicalHistory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface Retrofit cho Lịch hẹn (Appointments - NV 5b & NV 7)
 * và Bệnh án (MedicalHistories - NV 4).
 * BASE_URL = "http://10.0.2.2:8080/api/" nên các endpoint không chứa tiền tố "api/".
 */
public interface AppointmentMedicalApi {

    // NV 5b: User đặt lịch hẹn mới (POST /api/appointments)
    @POST("appointments")
    Call<Appointment> createAppointment(@Body Appointment appointment);

    // Lấy danh sách lịch hẹn của User (GET /api/appointments/user/{userId})
    @GET("appointments/user/{userId}")
    Call<List<Appointment>> getUserAppointments(@Path("userId") Long userId);

    // NV 7: Admin lấy tất cả lịch hẹn để duyệt (GET /api/appointments)
    @GET("appointments")
    Call<List<Appointment>> getAllAppointments();

    // NV 7: Admin duyệt hoặc hủy lịch (status = CONFIRMED / CANCELLED) (PUT /api/appointments/{id}/status?status=...)
    @PUT("appointments/{id}/status")
    Call<Appointment> updateAppointmentStatus(
            @Path("id") Long appointmentId,
            @Query("status") String status
    );

    // NV 7 (Ăn điểm @Transactional): Hoàn thành ca khám & tự động tạo bệnh án + đơn thuốc (POST /api/appointments/{id}/complete)
    @POST("appointments/{id}/complete")
    Call<MedicalHistory> completeAppointmentAndPrescribe(
            @Path("id") Long appointmentId,
            @Body MedicalHistory medicalRequest
    );

    // NV 4: Xem lịch sử bệnh án theo user (GET /api/medical-histories/user/{userId})
    @GET("medical-histories/user/{userId}")
    Call<List<MedicalHistory>> getMedicalHistoriesByUser(@Path("userId") Long userId);

    // NV 4: Cập nhật thanh toán viện phí (Strategy Pattern: VIETQR / CASH) (PUT /api/medical-histories/{id}/pay?paymentMethod=...)
    @PUT("medical-histories/{id}/pay")
    Call<MedicalHistory> payMedicalHistory(
            @Path("id") Long medicalHistoryId,
            @Query("paymentMethod") String paymentMethod
    );

    // NV 4: Thống kê tổng chi phí khám chữa bệnh DECIMAL(18,0) (GET /api/medical-histories/user/{userId}/total-cost)
    @GET("medical-histories/user/{userId}/total-cost")
    Call<Map<String, BigDecimal>> getTotalMedicalCost(@Path("userId") Long userId);
}
