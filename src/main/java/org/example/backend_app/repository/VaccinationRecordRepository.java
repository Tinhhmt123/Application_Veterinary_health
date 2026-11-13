package org.example.backend_app.repository;

import org.example.backend_app.entity.VaccinationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, Long> {

    List<VaccinationRecord> findByPetIdOrderByNextDueDateAsc(Long petId);

    /**
     * Quét tất cả mũi tiêm của các thú cưng thuộc userId có next_due_date <= (Hôm nay + 7 ngày)
     * (Bao gồm cả mũi đã QUÁ HẠN và mũi SẮP ĐẾN HẠN trong vòng 7 ngày).
     */
    @Query("SELECT v FROM VaccinationRecord v WHERE v.petId IN " +
           "(SELECT p.id FROM Pet p WHERE p.userId = :userId) " +
           "AND v.nextDueDate <= :thresholdDate " +
           "ORDER BY v.nextDueDate ASC")
    List<VaccinationRecord> findAlertsForUser(@Param("userId") Long userId,
                                              @Param("thresholdDate") LocalDate thresholdDate);
}
