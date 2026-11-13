package org.example.backend_app.repository;

import org.example.backend_app.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    List<WeightLog> findByPetIdOrderByRecordDateDescIdDesc(Long petId);
}
