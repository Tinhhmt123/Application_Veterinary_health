package org.example.backend_app.controller;

import org.example.backend_app.entity.Pet;
import org.example.backend_app.entity.WeightLog;
import org.example.backend_app.service.PetWeightService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * [Thành viên 2] REST Controller Hồ sơ Thú cưng & Nhật ký Cân nặng (NV 2 & NV 6b)
 * Khớp 100% với PetWeightApi.java bên App Android.
 */
@RestController
@RequestMapping("/api")
public class PetWeightController {

    private final PetWeightService petWeightService;

    public PetWeightController(PetWeightService petWeightService) {
        this.petWeightService = petWeightService;
    }

    @GetMapping("/pets")
    public ResponseEntity<List<Pet>> getPetsByUser(
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        return ResponseEntity.ok(petWeightService.getPetsByUser(userId));
    }

    @PostMapping(value = "/pets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Pet> createPet(
            @RequestParam("user_id") Long userId,
            @RequestParam("name") String name,
            @RequestParam("species") String species,
            @RequestParam(value = "breed", required = false) String breed,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam(value = "birth_date", required = false) String birthDate,
            @RequestParam(value = "current_weight", required = false) Double currentWeight,
            @RequestPart(value = "avatarImage", required = false) MultipartFile avatarImage
    ) {
        return ResponseEntity.ok(petWeightService.createPet(
                userId, name, species, breed, gender, birthDate, currentWeight, avatarImage
        ));
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable("id") Long petId) {
        petWeightService.deletePet(petId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/weight-logs")
    public ResponseEntity<List<WeightLog>> getWeightLogsByPet(@RequestParam("petId") Long petId) {
        return ResponseEntity.ok(petWeightService.getWeightLogsByPet(petId));
    }

    @PostMapping("/weight-logs")
    public ResponseEntity<WeightLog> addWeightLog(@RequestBody WeightLog weightLog) {
        return ResponseEntity.ok(petWeightService.addWeightLog(weightLog));
    }

    @GetMapping("/admin/pets")
    public ResponseEntity<List<Pet>> searchAllPets(
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ResponseEntity.ok(petWeightService.searchAllPets(keyword));
    }
}
