package org.example.backend_app.controller;

import org.example.backend_app.entity.VaccinationRecord;
import org.example.backend_app.security.AccessTokenService;
import org.example.backend_app.security.BearerTokenFilter;
import org.example.backend_app.service.VaccinationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/vaccinations")
public class VaccinationController {
    private final VaccinationService service;
    public VaccinationController(VaccinationService service) { this.service = service; }

    private AccessTokenService.AuthenticatedUser current(HttpServletRequest request) {
        return (AccessTokenService.AuthenticatedUser) request.getAttribute(BearerTokenFilter.AUTHENTICATED_USER);
    }
    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(current(request).role())) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Chỉ ADMIN được quản lý hồ sơ tiêm chủng.");
    }

    private void requireUser(HttpServletRequest request) {
        if (!"USER".equals(current(request).role())) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Chỉ USER được tải ảnh dấu mộc.");
    }

    /** USER: chỉ cảnh báo của chính mình. ADMIN: cảnh báo toàn hệ thống. */
    @GetMapping("/alerts")
    public List<VaccinationRecord> alerts(HttpServletRequest request) {
        var user = current(request);
        return "ADMIN".equals(user.role()) ? service.getAllHealthAlerts() : service.getHealthAlerts(user.id());
    }

    /** ADMIN có thể xem toàn bộ bằng /all; USER bắt buộc truyền petId và được kiểm tra chủ sở hữu. */
    @GetMapping
    public List<VaccinationRecord> byPet(@RequestParam("petId") Long petId, HttpServletRequest request) {
        var user = current(request);
        return service.getVaccinationsByPet(petId, user.id(), "ADMIN".equals(user.role()));
    }

    @GetMapping("/all")
    public List<VaccinationRecord> all(HttpServletRequest request) {
        requireAdmin(request);
        return service.getAllVaccinations();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VaccinationRecord> create(
            @RequestParam("pet_id") Long petId,
            @RequestParam("vaccine_name") String vaccineName,
            @RequestParam("injection_date") String injectionDate,
            @RequestParam("next_due_date") String nextDueDate,
            @RequestParam(value = "clinic_name", required = false) String clinicName,
            @RequestPart(value = "stampImage", required = false) MultipartFile stampImage,
            HttpServletRequest request) {
        requireAdmin(request);
        if (stampImage != null && !stampImage.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Ảnh dấu mộc phải do USER tải lên sau khi hồ sơ được tạo.");
        }
        validateDates(injectionDate, nextDueDate);
        return ResponseEntity.ok(service.createVaccination(petId, vaccineName, injectionDate, nextDueDate, clinicName, null));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VaccinationRecord update(
            @PathVariable Long id,
            @RequestParam("pet_id") Long petId,
            @RequestParam("vaccine_name") String vaccineName,
            @RequestParam("injection_date") String injectionDate,
            @RequestParam("next_due_date") String nextDueDate,
            @RequestParam(value = "clinic_name", required = false) String clinicName,
            @RequestPart(value = "stampImage", required = false) MultipartFile stampImage,
            HttpServletRequest request) {
        requireAdmin(request);
        if (stampImage != null && !stampImage.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "ADMIN không được tải ảnh dấu mộc.");
        }
        validateDates(injectionDate, nextDueDate);
        return service.updateVaccination(id, petId, vaccineName, injectionDate, nextDueDate, clinicName, null);
    }

    /** USER chỉ được tải ảnh dấu mộc vào hồ sơ thuộc thú cưng của chính mình. */
    @PostMapping(value = "/{id}/stamp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VaccinationRecord> uploadStamp(
            @PathVariable Long id,
            @RequestPart("stampImage") MultipartFile stampImage,
            HttpServletRequest request) {
        var user = current(request);
        requireUser(request);
        return ResponseEntity.ok(service.uploadStampForOwner(id, user.id(), stampImage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        service.deleteVaccination(id);
        return ResponseEntity.noContent().build();
    }

    private void validateDates(String injectionDate, String nextDueDate) {
        try {
            LocalDate injection = LocalDate.parse(injectionDate);
            LocalDate next = LocalDate.parse(nextDueDate);
            if (injection.isAfter(LocalDate.now())) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Ngày tiêm không được lớn hơn ngày hiện tại.");
            if (!next.isAfter(injection)) throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Ngày tiêm tiếp theo phải sau ngày tiêm hiện tại.");
        } catch (DateTimeParseException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Ngày phải đúng định dạng yyyy-MM-dd.");
        }
    }
}