package org.example.backend_app.repository;

import org.example.backend_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE :kw IS NULL OR :kw = '' " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR u.phoneNumber LIKE CONCAT('%', :kw, '%') " +
           "ORDER BY u.id DESC")
    List<User> searchByKeyword(@Param("kw") String keyword);

    List<User> findByIsActiveOrderByIdDesc(Boolean isActive);
}
