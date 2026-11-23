
    package com.example.healthy_pet.api;

    import com.example.healthy_pet.BuildConfig;

    import java.util.concurrent.TimeUnit;

    import okhttp3.OkHttpClient;
    import okhttp3.logging.HttpLoggingInterceptor;

    import retrofit2.Retrofit;
    import retrofit2.converter.gson.GsonConverterFactory;

    /**
     * Cấu hình Retrofit Singleton dùng chung cho ứng dụng HealthyPet.
     * Kết nối tới Spring Boot Web API.
     */
    public class RetrofitClient {

        // Chỉ khai báo BASE_URL một lần
        // Giá trị được lấy từ BuildConfig
        public static final String BASE_URL = BuildConfig.BASE_URL;

        private static Retrofit retrofit = null;

        public static Retrofit getClient() {

            if (retrofit == null) {

                // Chỉ tạo một HttpLoggingInterceptor
                HttpLoggingInterceptor logging =
                        new HttpLoggingInterceptor();

                // BASIC: chỉ ghi log request/response cơ bản
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                // Cấu hình kết nối HTTP
                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(150, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build();

                // Khởi tạo Retrofit
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(
                                GsonConverterFactory.create()
                        )
                        .build();
            }

            return retrofit;
        }

        // API đăng nhập và tài khoản
        public static AuthUserApi getAuthUserApi() {
            return getClient().create(AuthUserApi.class);
        }

        // API hồ sơ thú cưng và cân nặng
        public static PetWeightApi getPetWeightApi() {
            return getClient().create(PetWeightApi.class);
        }

        // API tiêm phòng
        public static VaccinationApi getVaccinationApi() {
            return getClient().create(VaccinationApi.class);
        }

        // API bản đồ phòng khám
        public static ClinicMapApi getClinicMapApi() {
            return getClient().create(ClinicMapApi.class);
        }

        // API đặt lịch và hồ sơ y tế
        public static AppointmentMedicalApi getAppointmentMedicalApi() {
            return getClient().create(AppointmentMedicalApi.class);
        }

        // API trợ lý AI thú y
        public static AiPetApi getAiPetApi() {
            return getClient().create(AiPetApi.class);
        }
    }
