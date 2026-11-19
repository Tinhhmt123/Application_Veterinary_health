
package org.example.backend_app.service;

import org.example.backend_app.entity.Pet;
import org.example.backend_app.entity.VaccinationRecord;
import org.example.backend_app.repository.PetRepository;
import org.example.backend_app.repository.VaccinationRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
public class VaccinationService {

    private final VaccinationRecordRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final CloudinaryService cloudinaryService;

    public VaccinationService(
            VaccinationRecordRepository vaccinationRepository,
            PetRepository petRepository,
            CloudinaryService cloudinaryService) {
        this.vaccinationRepository = vaccinationRepository;
        this.petRepository = petRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // Lấy cảnh báo tiêm phòng của người dùng
    public List<VaccinationRecord> getHealthAlerts(Long userId) {
        LocalDate thresholdDate = LocalDate.now().plusDays(7);

        return vaccinationRepository.findAlertsForUser(
                userId, thresholdDate
        );
    }

    // Lấy lịch tiêm của thú cưng, đồng thời kiểm tra chủ sở hữu
    public List<VaccinationRecord> getVaccinationsByPet(
            Long petId, Long userId) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thú cưng"));

        if (!pet.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "Bạn không có quyền xem lịch tiêm của thú cưng này");
        }

        return vaccinationRepository
                .findByPetIdOrderByNextDueDateAsc(petId);
    }

    // Tạo lịch tiêm mới
    @Transactional
    public VaccinationRecord createVaccination(
            Long petId,
            Long userId,
            String vaccineName,
            String injectionDateStr,
            String nextDueDateStr,
            String clinicName,
            MultipartFile stampImage) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thú cưng"));

        if (!pet.getUserId().equals(userId)) {
            throw new RuntimeException(
                    "Bạn không có quyền thêm lịch tiêm cho thú cưng này");
        }

        LocalDate injectionDate =
                LocalDate.parse(injectionDateStr.trim());

        LocalDate nextDueDate =
                LocalDate.parse(nextDueDateStr.trim());

        if (nextDueDate.isBefore(injectionDate)) {
            throw new IllegalArgumentException(
                    "Ngày tiêm tiếp theo không được trước ngày tiêm hiện tại");
        }

        VaccinationRecord record = new VaccinationRecord();

        record.setPetId(petId);
        record.setVaccineName(vaccineName);
        record.setInjectionDate(injectionDate);
        record.setNextDueDate(nextDueDate);
        record.setClinicName(clinicName);

        if (stampImage != null && !stampImage.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(
                    stampImage,
                    "vaccine_stamps"
            );

            record.setStampImageUrl(imageUrl);

            System.out.println("VACCINE STAMP URL: " + imageUrl);
        } else {
            System.out.println("VACCINE STAMP: Không nhận được ảnh");
        }


        return vaccinationRepository.save(record);
    }
}
