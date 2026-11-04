package com.example.healthy_pet.api;

import com.example.healthy_pet.models.Appointment;
import com.example.healthy_pet.models.MedicalHistory;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * [Thành viên 5] API Đặt lịch hẹn (NV 5b), Admin Duyệt lịch & Kê đơn (NV 7), Bệnh án & Thanh toán DECIMAL (NV 4)
 */
public interface AppointmentMedicalApi {

    @GET("appointments")
    Call<List<Appointment>> getAppointmentsByUser(@Query("userId") Long userId);

    @POST("appointments")
    Call<Appointment> createAppointment(@Body Appointment appointment);

    // Admin: Danh sách tất cả lịch hẹn & Duyệt trạng thái (CONFIRMED / CANCELLED)
    @GET("admin/appointments")
    Call<List<Appointment>> getAllAppointments(@Query("status") String status);

    @PUT("admin/appointments/{id}/status")
    Call<Appointment> updateAppointmentStatus(
            @Path("id") Long appointmentId,
            @Query("status") String newStatus
    );

    // Admin: Hoàn thành ca khám (COMPLETED) -> Tạo mới MedicalHistory kèm ảnh toa thuốc Cloudinary
    @Multipart
    @POST("admin/appointments/{id}/complete")
    Call<MedicalHistory> completeAppointmentAndPrescribe(
            @Path("id") Long appointmentId,
            @Part("pet_id") RequestBody petId,
            @Part("symptoms") RequestBody symptoms,
            @Part("diagnosis") RequestBody diagnosis,
            @Part("prescription") RequestBody prescription,
            @Part("cost") RequestBody costDecimal,
            @Part MultipartBody.Part prescriptionImage
    );

    // User: Tra cứu lịch sử bệnh án & Thống kê chi phí DECIMAL(18, 0)
    @GET("medical-histories")
    Call<List<MedicalHistory>> getMedicalHistoriesByPet(@Query("petId") Long petId);

    // User: Thanh toán viện phí (Phương án 1 - Strategy Pattern: "VIETQR" hoặc "CASH")
    @PUT("medical-histories/{id}/pay")
    Call<MedicalHistory> payMedicalHistory(
            @Path("id") Long medicalHistoryId,
            @Query("paymentMethod") String paymentMethod
    );
}
