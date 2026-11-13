package org.example.backend_app.repository;

import org.example.backend_app.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {

    List<MedicalHistory> findByPetIdOrderByVisitDateDescIdDesc(Long petId);
}
