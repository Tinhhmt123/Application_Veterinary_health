package org.example.backend_app.controller;

import org.example.backend_app.entity.VetClinic;
import org.example.backend_app.service.ClinicMapService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [Thành viên 4] REST Controller Bản đồ Phòng khám (NV 5a) & Quản lý Chi nhánh VetClinics (NV 8)
 * Khớp 100% với ClinicMapApi.java bên App Android.
 */
@RestController
@RequestMapping("/api")
public class ClinicMapController {

    private final ClinicMapService clinicMapService;

    public ClinicMapController(ClinicMapService clinicMapService) {
        this.clinicMapService = clinicMapService;
    }

    @GetMapping("/clinics")
    public ResponseEntity<List<VetClinic>> getAllClinics() {
        return ResponseEntity.ok(clinicMapService.getAllClinics());
    }

    @PostMapping(value = "/admin/clinics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VetClinic> createClinic(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("phone_number") String phoneNumber,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "open_hours", required = false) String openHours,
            @RequestPart(value = "clinicImage", required = false) MultipartFile clinicImage
    ) {
        return ResponseEntity.ok(clinicMapService.createClinic(
                name, address, phoneNumber, latitude, longitude, openHours, clinicImage
        ));
    }

    @PutMapping(value = "/admin/clinics/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VetClinic> updateClinic(
            @PathVariable("id") Long clinicId,
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("phone_number") String phoneNumber,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "open_hours", required = false) String openHours,
            @RequestPart(value = "clinicImage", required = false) MultipartFile clinicImage
    ) {
        return ResponseEntity.ok(clinicMapService.updateClinic(
                clinicId, name, address, phoneNumber, latitude, longitude, openHours, clinicImage
        ));
    }

    @DeleteMapping("/admin/clinics/{id}")
    public ResponseEntity<Void> deleteClinic(@PathVariable("id") Long clinicId) {
        clinicMapService.deleteClinic(clinicId);
        return ResponseEntity.noContent().build();
    }
}
