
package com.example.healthy_pet.api;

import com.example.healthy_pet.models.VaccinationRecord;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface VaccinationApi {

    /**
     * Lấy danh sách cảnh báo tiêm chủng của người dùng.
     * Bao gồm các mũi tiêm quá hạn và sắp đến hạn trong 7 ngày.
     *
     * GET /api/vaccinations/alerts?userId=1
     */
    @GET("vaccinations/alerts")
    Call<List<VaccinationRecord>> getHealthAlerts(
            @Query("userId") Long userId
    );


    /**
     * Lấy lịch sử tiêm chủng của một thú cưng.
     *
     * GET /api/vaccinations?petId=1&userId=1
     */
    @GET("vaccinations")
    Call<List<VaccinationRecord>> getVaccinationsByPet(
            @Query("petId") Long petId,
            @Query("userId") Long userId
    );


    /**
     * Thêm hồ sơ tiêm chủng mới.
     * Hỗ trợ gửi ảnh dấu mộc phòng khám.
     *
     * POST /api/vaccinations
     * Content-Type: multipart/form-data
     */
    @Multipart
    @POST("vaccinations")
    Call<VaccinationRecord> createVaccination(

            @Part("pet_id") RequestBody petId,

            @Part("user_id") RequestBody userId,

            @Part("vaccine_name") RequestBody vaccineName,

            @Part("injection_date") RequestBody injectionDate,

            @Part("next_due_date") RequestBody nextDueDate,

            @Part("clinic_name") RequestBody clinicName,

            @Part MultipartBody.Part stampImage
    );
}
