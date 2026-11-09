
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

/**
 * [Thành viên 3]
 * API Sổ tiêm chủng và Cảnh báo y tế 7 ngày.
 */
public interface VaccinationApi {

    // 1. Lấy danh sách mũi tiêm quá hạn
    // hoặc sắp đến hạn trong 7 ngày của người dùng.
    @GET("vaccinations/alerts")
    Call<List<VaccinationRecord>> getHealthAlerts(
            @Query("userId") Long userId
    );

    // 2. Lấy lịch sử tiêm chủng theo thú cưng.
    @GET("vaccinations")
    Call<List<VaccinationRecord>> getVaccinationsByPet(
            @Query("petId") Long petId,
            @Query("userId") Long userId
    );

    // 3. Thêm mũi tiêm mới và upload ảnh tem vaccine.
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
