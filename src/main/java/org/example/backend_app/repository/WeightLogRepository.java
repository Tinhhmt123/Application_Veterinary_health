package org.example.backend_app.repository;

import org.example.backend_app.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    List<WeightLog> findByPetIdOrderByRecordDateDescIdDesc(Long petId);

    // Xóa toàn bộ nhật ký cân nặng của 1 thú cưng
    @Modifying
    @Query("DELETE FROM WeightLog w WHERE w.petId = :petId")
    void deleteByPetId(@Param("petId") Long petId);

    // Lấy mốc cân nặng mới nhất để đồng bộ lại Pets.current_weight khi sửa/xóa bản ghi
    Optional<WeightLog> findTopByPetIdOrderByRecordDateDescIdDesc(Long petId);
}
