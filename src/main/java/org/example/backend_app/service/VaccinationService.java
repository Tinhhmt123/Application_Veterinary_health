package org.example.backend_app.service;

import org.example.backend_app.entity.VaccinationRecord;
import org.example.backend_app.repository.VaccinationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * [Thành viên 3] Service xử lý Sổ tiêm chủng & Thuật toán Cảnh báo y tế tự động trước 7 ngày (NV 3)
 */
@Service
public class VaccinationService {

    private final VaccinationRecordRepository vaccinationRepository;
    private final CloudinaryService cloudinaryService;

    public VaccinationService(VaccinationRecordRepository vaccinationRepository,
                              CloudinaryService cloudinaryService) {
        this.vaccinationRepository = vaccinationRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Lọc danh sách các mũi tiêm đã quá hạn hoặc sắp đến hạn tái chủng trong vòng <= 7 ngày tới.
     */
    public List<VaccinationRecord> getHealthAlerts(Long userId) {
        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        return vaccinationRepository.findAlertsForUser(userId, sevenDaysFromNow);
    }

    public List<VaccinationRecord> getVaccinationsByPet(Long petId) {
        return vaccinationRepository.findByPetIdOrderByNextDueDateAsc(petId);
    }

    @Transactional
    public VaccinationRecord createVaccination(Long petId, String vaccineName,
                                               String injectionDateStr, String nextDueDateStr,
                                               String clinicName, MultipartFile stampImage) {
        VaccinationRecord record = new VaccinationRecord();
        record.setPetId(petId);
        record.setVaccineName(vaccineName);
        record.setInjectionDate(LocalDate.parse(injectionDateStr.trim()));
        record.setNextDueDate(LocalDate.parse(nextDueDateStr.trim()));
        record.setClinicName(clinicName);

        if (stampImage != null && !stampImage.isEmpty()) {
            record.setStampImageUrl(cloudinaryService.uploadImage(stampImage, "vaccine_stamps"));
        }

        return vaccinationRepository.save(record);
    }
}
