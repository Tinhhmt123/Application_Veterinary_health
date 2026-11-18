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

    @Query("SELECT p FROM Pet p WHERE " +
           "(:userId IS NULL OR p.userId = :userId) " +
           "AND (:species IS NULL OR :species = '' OR LOWER(p.species) LIKE LOWER(CONCAT('%', :species, '%'))) " +
           "AND (:kw IS NULL OR :kw = '' " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(p.species) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(COALESCE(p.breed, '')) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR EXISTS (SELECT u.id FROM User u WHERE u.id = p.userId " +
           "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR COALESCE(u.phoneNumber, '') LIKE CONCAT('%', :kw, '%')))) " +
           "ORDER BY p.id DESC")
    List<Pet> searchAllByKeyword(@Param("kw") String keyword,
                                 @Param("species") String species,
                                 @Param("userId") Long userId);
}
