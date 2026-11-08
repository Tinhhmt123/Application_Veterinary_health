package org.example.backend_app.repository;

import org.example.backend_app.entity.VetClinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VetClinicRepository extends JpaRepository<VetClinic, Long> {
}
