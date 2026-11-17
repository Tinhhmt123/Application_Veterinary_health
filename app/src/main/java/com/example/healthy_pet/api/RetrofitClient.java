package com.example.healthy_pet.api;

import com.example.healthy_pet.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Retrofit Singleton dùng chung cho cả 5 thành viên kết nối tới Spring Boot Web API.
 * Giá trị BASE_URL được đọc từ file .env ở thư mục gốc project qua BuildConfig.BASE_URL
 * (nếu thiếu file .env sẽ mặc định fallback về http://10.0.2.2:8080/api/ cho Android Emulator).
 */
public class RetrofitClient {
    public static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Chỉ log dòng request/response cơ bản để tránh in binary ảnh vào Logcat và không lộ thông tin nhạy cảm
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthUserApi getAuthUserApi() {
        return getClient().create(AuthUserApi.class);
    }

    public static PetWeightApi getPetWeightApi() {
        return getClient().create(PetWeightApi.class);
    }

    public static VaccinationApi getVaccinationApi() {
        return getClient().create(VaccinationApi.class);
    }

    public static ClinicMapApi getClinicMapApi() {
        return getClient().create(ClinicMapApi.class);
    }

    public static AppointmentMedicalApi getAppointmentMedicalApi() {
        return getClient().create(AppointmentMedicalApi.class);
    }
}
