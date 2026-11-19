
package org.example.backend_app.controller;

import org.example.backend_app.entity.VaccinationRecord;
import org.example.backend_app.service.VaccinationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VaccinationController {

    private final VaccinationService vaccinationService;

    public VaccinationController(
            VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    // GET: Lấy cảnh báo tiêm phòng
    @GetMapping("/vaccinations/alerts")
    public ResponseEntity<List<VaccinationRecord>> getHealthAlerts(
            @RequestParam("userId") Long userId) {

        return ResponseEntity.ok(
                vaccinationService.getHealthAlerts(userId)
        );
    }

    // GET: Lấy lịch tiêm theo thú cưng và người dùng
    @GetMapping("/vaccinations")
    public ResponseEntity<List<VaccinationRecord>> getVaccinationsByPet(
            @RequestParam("petId") Long petId,
            @RequestParam("userId") Long userId) {

        return ResponseEntity.ok(
                vaccinationService.getVaccinationsByPet(
                        petId, userId
                )
        );
    }

    // POST: Thêm lịch tiêm mới
    @PostMapping(
            value = "/vaccinations",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createVaccination(
            @RequestParam("pet_id") Long petId,
            @RequestParam("user_id") Long userId,
            @RequestParam("vaccine_name") String vaccineName,
            @RequestParam("injection_date") String injectionDate,
            @RequestParam("next_due_date") String nextDueDate,
            @RequestParam(value = "clinic_name", required = false)
            String clinicName,
            @RequestPart(value = "stampImage", required = false)
            MultipartFile stampImage) {

        // 1. Kiểm tra định dạng ngày
        LocalDate injection;
        LocalDate nextDue;

        try {
            injection = LocalDate.parse(injectionDate);
            nextDue = LocalDate.parse(nextDueDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body("Ngày phải đúng định dạng yyyy-MM-dd.");
        }

        // 2. Không cho phép ngày tiêm trong tương lai
        LocalDate today = LocalDate.now();

        if (injection.isAfter(today)) {
            return ResponseEntity.badRequest()
                    .body("Ngày tiêm không được lớn hơn ngày hiện tại.");
        }

        // 3. Ngày tiêm tiếp theo phải sau ngày tiêm hiện tại
        if (!nextDue.isAfter(injection)) {
            return ResponseEntity.badRequest()
                    .body("Ngày tiêm tiếp theo phải sau ngày tiêm hiện tại.");
        }

        // 4. Lưu hồ sơ tiêm chủng
        VaccinationRecord result =
                vaccinationService.createVaccination(
                        petId,
                        userId,
                        vaccineName,
                        injectionDate,
                        nextDueDate,
                        clinicName,
                        stampImage
                );

        return ResponseEntity.ok(result);
    }
}
