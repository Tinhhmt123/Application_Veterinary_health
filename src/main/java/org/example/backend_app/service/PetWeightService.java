package org.example.backend_app.service;

import org.example.backend_app.entity.Pet;
import org.example.backend_app.entity.WeightLog;
import org.example.backend_app.repository.PetRepository;
import org.example.backend_app.repository.WeightLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * [Thành viên 2] Service xử lý Hồ sơ Thú cưng & Nhật ký Cân nặng tự động đồng bộ current_weight (NV 2 & NV 6b)
 */
@Service
public class PetWeightService {

    private final PetRepository petRepository;
    private final WeightLogRepository weightLogRepository;
    private final CloudinaryService cloudinaryService;

    public PetWeightService(PetRepository petRepository,
                            WeightLogRepository weightLogRepository,
                            CloudinaryService cloudinaryService) {
        this.petRepository = petRepository;
        this.weightLogRepository = weightLogRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<Pet> getPetsByUser(Long userId) {
        if (userId == null) {
            return petRepository.findAll();
        }
        return petRepository.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public Pet createPet(Long userId, String name, String species, String breed,
                         String gender, String birthDateStr, Double currentWeight,
                         MultipartFile avatarImage) {
        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setName(name);
        pet.setSpecies(species);
        pet.setBreed(breed);
        pet.setGender(gender);
        if (birthDateStr != null && !birthDateStr.isBlank()) {
            pet.setBirthDate(LocalDate.parse(birthDateStr.trim()));
        }
        pet.setCurrentWeight(currentWeight != null ? currentWeight : 0.0);

        if (avatarImage != null && !avatarImage.isEmpty()) {
            pet.setAvatarUrl(cloudinaryService.uploadImage(avatarImage, "pets"));
        }

        Pet savedPet = petRepository.save(pet);

        // Nếu lúc tạo hồ sơ có nhập cân nặng ban đầu -> ghi luôn mốc đầu tiên vào WeightLogs
        if (savedPet.getCurrentWeight() != null && savedPet.getCurrentWeight() > 0) {
            WeightLog initialLog = new WeightLog();
            initialLog.setPetId(savedPet.getId());
            initialLog.setRecordDate(LocalDate.now());
            initialLog.setWeight(savedPet.getCurrentWeight());
            initialLog.setNote("Cân nặng ban đầu khi tạo hồ sơ");
            weightLogRepository.save(initialLog);
        }

        return savedPet;
    }

    @Transactional
    public void deletePet(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + petId);
        }
        petRepository.deleteById(petId);
    }

    public List<WeightLog> getWeightLogsByPet(Long petId) {
        return weightLogRepository.findByPetIdOrderByRecordDateDescIdDesc(petId);
    }

    /**
     * Ghi nhận chỉ số cân nặng mới vào WeightLogs VÀ tự động đồng bộ sang Pets.current_weight.
     */
    @Transactional
    public WeightLog addWeightLog(WeightLog weightLog) {
        Pet pet = petRepository.findById(weightLog.getPetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + weightLog.getPetId()));

        if (weightLog.getRecordDate() == null) {
            weightLog.setRecordDate(LocalDate.now());
        }
        WeightLog savedLog = weightLogRepository.save(weightLog);

        // Đồng bộ cân nặng hiện tại sang bảng Pets
        pet.setCurrentWeight(savedLog.getWeight());
        petRepository.save(pet);

        return savedLog;
    }

    public List<Pet> searchAllPets(String keyword) {
        return petRepository.searchAllByKeyword(keyword);
    }
}
