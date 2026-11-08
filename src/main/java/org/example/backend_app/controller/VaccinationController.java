package org.example.backend_app.controller;

import org.example.backend_app.entity.VaccinationRecord;
import org.example.backend_app.service.VaccinationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [Thành viên 3] REST Controller Sổ tiêm chủng & Cảnh báo y tế 7 ngày (NV 3)
 * Khớp 100% với VaccinationApi.java bên App Android.
 */
@RestController
@RequestMapping("/api")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @GetMapping("/vaccinations/alerts")
    public ResponseEntity<List<VaccinationRecord>> getHealthAlerts(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(vaccinationService.getHealthAlerts(userId));
    }

    @GetMapping("/vaccinations")
    public ResponseEntity<List<VaccinationRecord>> getVaccinationsByPet(@RequestParam("petId") Long petId) {
        return ResponseEntity.ok(vaccinationService.getVaccinationsByPet(petId));
    }

    @PostMapping(value = "/vaccinations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VaccinationRecord> createVaccination(
            @RequestParam("pet_id") Long petId,
            @RequestParam("vaccine_name") String vaccineName,
            @RequestParam("injection_date") String injectionDate,
            @RequestParam("next_due_date") String nextDueDate,
            @RequestParam(value = "clinic_name", required = false) String clinicName,
            @RequestPart(value = "stampImage", required = false) MultipartFile stampImage
    ) {
        return ResponseEntity.ok(vaccinationService.createVaccination(
                petId, vaccineName, injectionDate, nextDueDate, clinicName, stampImage
        ));
    }
}
