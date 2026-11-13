package org.example.backend_app.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * [Thành viên 4] Thực thể 3: VetClinics (Chi nhánh Phòng khám & Bản đồ Google Maps)
 */
@Entity
@Table(name = "VetClinics")
public class VetClinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    @JsonProperty("name")
    private String name;

    @Column(name = "address", nullable = false, length = 255)
    @JsonProperty("address")
    private String address;

    @Column(name = "phone_number", nullable = false, length = 20)
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Column(name = "latitude", nullable = false)
    @JsonProperty("latitude")
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    @JsonProperty("longitude")
    private Double longitude;

    @Column(name = "open_hours", length = 100)
    @JsonProperty("open_hours")
    private String openHours;

    @Column(name = "image_url", length = 500)
    @JsonProperty("image_url")
    private String imageUrl;

    public VetClinic() {
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
