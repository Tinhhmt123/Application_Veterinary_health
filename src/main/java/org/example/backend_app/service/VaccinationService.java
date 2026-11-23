package org.example.backend_app.service;

import org.example.backend_app.entity.Pet;
import org.example.backend_app.entity.VaccinationRecord;
import org.example.backend_app.repository.PetRepository;
import org.example.backend_app.repository.VaccinationRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class VaccinationService {
    private final VaccinationRecordRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final CloudinaryService cloudinaryService;

    public VaccinationService(VaccinationRecordRepository vaccinationRepository, PetRepository petRepository, CloudinaryService cloudinaryService) {
        this.vaccinationRepository = vaccinationRepository;
        this.petRepository = petRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<VaccinationRecord> getHealthAlerts(Long userId) {
        return vaccinationRepository.findAlertsForUser(userId, LocalDate.now().plusDays(7));
    }
    public List<VaccinationRecord> getAllHealthAlerts() {
        return vaccinationRepository.findAlertsForAll(LocalDate.now().plusDays(7));
    }
    public List<VaccinationRecord> getAllVaccinations() { return vaccinationRepository.findAllByOrderByNextDueDateAsc(); }

    public List<VaccinationRecord> getVaccinationsByPet(Long petId, Long userId, boolean admin) {
        Pet pet = petRepository.findById(petId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));
        if (!admin && !pet.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem lịch tiêm của thú cưng này");
        return vaccinationRepository.findByPetIdOrderByNextDueDateAsc(petId);
    }

    @Transactional
    public VaccinationRecord createVaccination(Long petId, String vaccineName, String injectionDate, String nextDueDate, String clinicName, MultipartFile stampImage) {
        Pet pet = petRepository.findById(petId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));
        VaccinationRecord record = new VaccinationRecord();
        record.setPetId(pet.getId());
        apply(record, vaccineName, injectionDate, nextDueDate, clinicName, stampImage);
        return vaccinationRepository.save(record);
    }

    @Transactional
    public VaccinationRecord updateVaccination(Long id, Long petId, String vaccineName, String injectionDate, String nextDueDate, String clinicName, MultipartFile stampImage) {
        VaccinationRecord record = vaccinationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ tiêm chủng"));
        Pet pet = petRepository.findById(petId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng"));
        record.setPetId(pet.getId());
        apply(record, vaccineName, injectionDate, nextDueDate, clinicName, stampImage);
        return vaccinationRepository.save(record);
    }

    /** Chỉ chủ sở hữu thú cưng mới được cập nhật ảnh dấu mộc của hồ sơ hiện có. */
    @Transactional
    public VaccinationRecord uploadStampForOwner(Long vaccinationId, Long userId, MultipartFile stampImage) {
        if (stampImage == null || stampImage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh dấu mộc.");
        }
        String contentType = stampImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp tải lên phải là hình ảnh.");
        }
        if (stampImage.getSize() > 10L * 1024L * 1024L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh không được vượt quá 10 MB.");
        }

        VaccinationRecord record = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ tiêm chủng."));
        Pet pet = petRepository.findById(record.getPetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng."));
        if (pet.getUserId() == null || !pet.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật ảnh hồ sơ của thú cưng này.");
        }

        record.setStampImageUrl(cloudinaryService.uploadImage(stampImage, "vaccine_stamps"));
        return vaccinationRepository.save(record);
    }

    @Transactional
    public void deleteVaccination(Long id) {
        if (!vaccinationRepository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ tiêm chủng");
        vaccinationRepository.deleteById(id);
    }

    private void apply(VaccinationRecord record, String vaccineName, String injectionDate, String nextDueDate, String clinicName, MultipartFile stampImage) {
        if (vaccineName == null || vaccineName.isBlank()) throw new IllegalArgumentException("Tên vaccine không được để trống.");
        LocalDate injection = LocalDate.parse(injectionDate);
        LocalDate next = LocalDate.parse(nextDueDate);
        if (injection.isAfter(LocalDate.now())) throw new IllegalArgumentException("Ngày tiêm không được ở tương lai.");
        if (!next.isAfter(injection)) throw new IllegalArgumentException("Ngày tiêm tiếp theo phải sau ngày tiêm hiện tại.");
        record.setVaccineName(vaccineName.trim());
        record.setInjectionDate(injection);
        record.setNextDueDate(next);
        record.setClinicName(clinicName);
        if (stampImage != null && !stampImage.isEmpty()) record.setStampImageUrl(cloudinaryService.uploadImage(stampImage, "vaccine_stamps"));
    }
}