package org.example.backend_app.service;

import org.example.backend_app.entity.VetClinic;
import org.example.backend_app.repository.VetClinicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * [Thành viên 4] Service xử lý Bản đồ Phòng khám (NV 5a) & Quản lý hệ thống Chi nhánh VetClinics (NV 8)
 */
@Service
public class ClinicMapService {

    private final VetClinicRepository clinicRepository;
    private final CloudinaryService cloudinaryService;

    public ClinicMapService(VetClinicRepository clinicRepository, CloudinaryService cloudinaryService) {
        this.clinicRepository = clinicRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<VetClinic> getAllClinics() {
        return clinicRepository.findAll();
    }

    @Transactional
    public VetClinic createClinic(String name, String address, String phoneNumber,
                                  Double latitude, Double longitude, String openHours,
                                  MultipartFile clinicImage) {
        VetClinic clinic = new VetClinic();
        clinic.setName(name);
        clinic.setAddress(address);
        clinic.setPhoneNumber(phoneNumber);
        clinic.setLatitude(latitude);
        clinic.setLongitude(longitude);
        clinic.setOpenHours(openHours);

        if (clinicImage != null && !clinicImage.isEmpty()) {
            clinic.setImageUrl(cloudinaryService.uploadImage(clinicImage, "clinics"));
        }
        return clinicRepository.save(clinic);
    }

    @Transactional
    public VetClinic updateClinic(Long clinicId, String name, String address, String phoneNumber,
                                  Double latitude, Double longitude, String openHours,
                                  MultipartFile clinicImage) {
        VetClinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chi nhánh #" + clinicId));

        clinic.setName(name);
        clinic.setAddress(address);
        clinic.setPhoneNumber(phoneNumber);
        clinic.setLatitude(latitude);
        clinic.setLongitude(longitude);
        clinic.setOpenHours(openHours);

        if (clinicImage != null && !clinicImage.isEmpty()) {
            clinic.setImageUrl(cloudinaryService.uploadImage(clinicImage, "clinics"));
        }
        return clinicRepository.save(clinic);
    }

    @Transactional
    public void deleteClinic(Long clinicId) {
        if (!clinicRepository.existsById(clinicId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chi nhánh #" + clinicId);
        }
        clinicRepository.deleteById(clinicId);
    }
}
