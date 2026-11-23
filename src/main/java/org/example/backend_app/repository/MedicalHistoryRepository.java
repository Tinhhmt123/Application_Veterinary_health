package org.example.backend_app.repository;

import org.example.backend_app.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {

    List<MedicalHistory> findByPetIdOrderByVisitDateDescIdDesc(Long petId);

    List<MedicalHistory> findAllByOrderByVisitDateDescIdDesc();

    List<MedicalHistory> findByVisitDateBetweenOrderByVisitDateDescIdDesc(LocalDate from, LocalDate to);

    List<MedicalHistory> findByPaymentStatusOrderByVisitDateDescIdDesc(String paymentStatus);

    Optional<MedicalHistory> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    boolean existsByPetId(Long petId);
}
