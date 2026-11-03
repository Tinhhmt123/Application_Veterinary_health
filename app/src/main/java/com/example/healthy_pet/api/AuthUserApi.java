package com.example.healthy_pet.api;

import com.example.healthy_pet.models.User;
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
 * [Thành viên 1] API Quản lý Tài khoản User (NV 1) & Admin Quản lý Users (NV 6a)
 */
public interface AuthUserApi {

    @POST("auth/login")
    Call<User> login(@Body User loginRequest);

    @POST("auth/register")
    Call<User> register(@Body User registerRequest);

    @Multipart
    @PUT("users/{id}")
    Call<User> updateProfile(
            @Path("id") Long userId,
            @Part("full_name") RequestBody fullName,
            @Part("phone_number") RequestBody phoneNumber,
            @Part MultipartBody.Part avatarFile
    );

    // Admin: Tìm kiếm và Khóa/Mở khóa tài khoản (is_active)
    @GET("admin/users")
    Call<List<User>> searchUsers(@Query("keyword") String keyword);

    @PUT("admin/users/{id}/toggle-active")
    Call<User> toggleUserActive(@Path("id") Long userId);
}
