package com.example.healthy_pet.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Thực thể 3: VetClinics (Chi nhánh Phòng khám)
 * Phụ trách chính: Thành viên 4 (NV 5a & NV 8)
 */
public class VetClinic implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("address")
    private String address;

    @SerializedName("phone_number")
    private String phoneNumber;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("open_hours")
    private String openHours;

    @SerializedName("image_url")
    private String imageUrl;

    public VetClinic() {
    }

    public VetClinic(Long id, String name, String address, String phoneNumber, Double latitude, Double longitude, String openHours, String imageUrl) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openHours = openHours;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getOpenHours() { return openHours; }
    public void setOpenHours(String openHours) { this.openHours = openHours; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
