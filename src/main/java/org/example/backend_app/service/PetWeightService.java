package org.example.backend_app.service;

import org.example.backend_app.entity.Pet;
import org.example.backend_app.entity.User;
import org.example.backend_app.entity.WeightLog;
import org.example.backend_app.repository.AppointmentRepository;
import org.example.backend_app.repository.MedicalHistoryRepository;
import org.example.backend_app.repository.PetRepository;
import org.example.backend_app.repository.UserRepository;
import org.example.backend_app.repository.WeightLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * [Thành viên 2] Service xử lý Hồ sơ Thú cưng & Nhật ký Cân nặng tự động đồng bộ current_weight (NV 2 & NV 6b)
 */
@Service
public class PetWeightService {

    private final PetRepository petRepository;
    private final WeightLogRepository weightLogRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final CloudinaryService cloudinaryService;

    public PetWeightService(PetRepository petRepository,
                            WeightLogRepository weightLogRepository,
                            UserRepository userRepository,
                            AppointmentRepository appointmentRepository,
                            MedicalHistoryRepository medicalHistoryRepository,
                            CloudinaryService cloudinaryService) {
        this.petRepository = petRepository;
        this.weightLogRepository = weightLogRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<Pet> getPetsByUser(Long userId) {
        List<Pet> pets = (userId == null)
                ? petRepository.searchAllByKeyword(null, null, null)
                : petRepository.findByUserIdOrderByIdDesc(userId);
        return attachOwnerInfo(pets);
    }

    public Pet getPetById(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + petId));
        return attachOwnerInfo(pet);
    }

    @Transactional
    public Pet createPet(Long userId, String name, String species, String breed,
                         String gender, String birthDateStr, Double currentWeight,
                         MultipartFile avatarImage) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chủ nuôi #" + userId);
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thú cưng không được để trống!");
        }
        if (species == null || species.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loài thú cưng không được để trống!");
        }
        if (currentWeight != null && currentWeight < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cân nặng không được âm!");
        }

        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setName(name.trim());
        pet.setSpecies(species.trim());
        pet.setBreed(trimToNull(breed));
        pet.setGender(trimToNull(gender));
        pet.setBirthDate(parseBirthDate(birthDateStr));
        pet.setCurrentWeight(currentWeight != null ? currentWeight : 0.0);

        if (avatarImage != null && !avatarImage.isEmpty()) {
            pet.setAvatarUrl(cloudinaryService.uploadImage(
                    avatarImage, "pets", buildPetPublicId(pet.getName(), pet.getUserId())));
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

        return attachOwnerInfo(savedPet);
    }

    @Transactional
    public void deletePet(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + petId);
        }
        if (appointmentRepository.existsByPetId(petId) || medicalHistoryRepository.existsByPetId(petId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể xóa thú cưng đã có lịch hẹn hoặc hồ sơ bệnh án!");
        }
        weightLogRepository.deleteByPetId(petId);
        petRepository.deleteById(petId);
    }

    public List<WeightLog> getWeightLogsByPet(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + petId);
        }
        return weightLogRepository.findByPetIdOrderByRecordDateDescIdDesc(petId);
    }

    /**
     * Ghi nhận chỉ số cân nặng mới vào WeightLogs VÀ tự động đồng bộ sang Pets.current_weight.
     */
    @Transactional
    public WeightLog addWeightLog(WeightLog weightLog) {
        if (weightLog == null || weightLog.getPetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin thú cưng (pet_id)!");
        }
        if (weightLog.getWeight() == null || weightLog.getWeight() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cân nặng phải lớn hơn 0 kg!");
        }
        Pet pet = petRepository.findById(weightLog.getPetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + weightLog.getPetId()));

        if (weightLog.getRecordDate() == null) {
            weightLog.setRecordDate(LocalDate.now());
        }
        WeightLog savedLog = weightLogRepository.save(weightLog);

        // Đồng bộ cân nặng hiện tại sang bảng Pets theo mốc mới nhất
        syncPetLatestWeight(pet);

        return savedLog;
    }

    @Transactional
    public WeightLog updateWeightLog(Long logId, WeightLog request) {
        WeightLog existing = weightLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi cân nặng #" + logId));

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu cập nhật cân nặng không hợp lệ!");
        }
        if (request.getWeight() != null) {
            if (request.getWeight() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cân nặng phải lớn hơn 0 kg!");
            }
            existing.setWeight(request.getWeight());
        }
        if (request.getRecordDate() != null) {
            existing.setRecordDate(request.getRecordDate());
        }
        if (request.getNote() != null) {
            existing.setNote(request.getNote().trim());
        }

        WeightLog savedLog = weightLogRepository.save(existing);
        petRepository.findById(savedLog.getPetId()).ifPresent(this::syncPetLatestWeight);
        return savedLog;
    }

    @Transactional
    public void deleteWeightLog(Long logId) {
        WeightLog existing = weightLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi cân nặng #" + logId));
        Long petId = existing.getPetId();
        weightLogRepository.deleteById(logId);
        petRepository.findById(petId).ifPresent(this::syncPetLatestWeight);
    }

    public List<Pet> searchAllPets(String keyword, String species, Long userId) {
        String normalizedKeyword = trimToNull(keyword);
        String normalizedSpecies = trimToNull(species);
        boolean filterOtherSpecies = "Khác".equalsIgnoreCase(normalizedSpecies);

        List<Pet> pets = petRepository.searchAllByKeyword(
                normalizedKeyword,
                filterOtherSpecies ? null : normalizedSpecies,
                userId
        );

        if (filterOtherSpecies) {
            pets = pets.stream()
                    .filter(p -> p.getSpecies() == null
                            || (!p.getSpecies().toLowerCase().contains("chó")
                            && !p.getSpecies().toLowerCase().contains("mèo")))
                    .collect(Collectors.toList());
        }
        return attachOwnerInfo(pets);
    }

    @Transactional
    public Pet updatePet(Long petId, Long userId, String name, String species,
                         String breed, String gender, String birthDateStr,
                         Double currentWeight, MultipartFile avatarImage) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thú cưng #" + petId));

        if (userId != null) {
            if (!userRepository.existsById(userId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chủ nuôi #" + userId);
            }
            pet.setUserId(userId);
        }
        if (name != null) {
            if (name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thú cưng không được để trống!");
            }
            pet.setName(name.trim());
        }
        if (species != null) {
            if (species.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loài thú cưng không được để trống!");
            }
            pet.setSpecies(species.trim());
        }
        if (breed != null) {
            pet.setBreed(trimToNull(breed));
        }
        if (gender != null) {
            pet.setGender(trimToNull(gender));
        }
        if (birthDateStr != null) {
            pet.setBirthDate(parseBirthDate(birthDateStr));
        }
        if (currentWeight != null) {
            if (currentWeight < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cân nặng không được âm!");
            }
            pet.setCurrentWeight(currentWeight);
        }
        if (avatarImage != null && !avatarImage.isEmpty()) {
            pet.setAvatarUrl(cloudinaryService.uploadImage(
                    avatarImage, "pets", buildPetPublicId(pet.getName(), pet.getUserId())));
        }

        return attachOwnerInfo(petRepository.save(pet));
    }

    private void syncPetLatestWeight(Pet pet) {
        double latestWeight = weightLogRepository.findTopByPetIdOrderByRecordDateDescIdDesc(pet.getId())
                .map(WeightLog::getWeight)
                .orElse(0.0);
        pet.setCurrentWeight(latestWeight);
        petRepository.save(pet);
    }

    private LocalDate parseBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthDateStr.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày sinh phải đúng định dạng yyyy-MM-dd (VD: 2024-05-10)!");
        }
    }

    private String buildPetPublicId(String petName, Long userId) {
        return (petName != null ? petName.trim() : "pet") + "_User" + userId;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Pet attachOwnerInfo(Pet pet) {
        if (pet == null || pet.getUserId() == null) {
            return pet;
        }
        userRepository.findById(pet.getUserId()).ifPresent(owner -> {
            pet.setOwnerName(owner.getFullName());
            pet.setOwnerPhone(owner.getPhoneNumber());
        });
        return pet;
    }

    private List<Pet> attachOwnerInfo(List<Pet> pets) {
        if (pets == null || pets.isEmpty()) {
            return pets;
        }
        Set<Long> ownerIds = pets.stream()
                .map(Pet::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return pets;
        }
        Map<Long, User> ownerMap = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        for (Pet pet : pets) {
            User owner = ownerMap.get(pet.getUserId());
            if (owner != null) {
                pet.setOwnerName(owner.getFullName());
                pet.setOwnerPhone(owner.getPhoneNumber());
            }
        }
        return pets;
    }
}
