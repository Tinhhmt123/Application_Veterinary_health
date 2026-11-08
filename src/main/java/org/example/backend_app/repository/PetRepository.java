package org.example.backend_app.repository;

import org.example.backend_app.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByUserIdOrderByIdDesc(Long userId);

    @Query("SELECT p FROM Pet p WHERE :kw IS NULL OR :kw = '' " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(p.species) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<Pet> searchAllByKeyword(@Param("kw") String keyword);
}
