package org.example.backend_app.repository;

import org.example.backend_app.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrderByAppointmentTimeDesc(Long userId);

    List<Appointment> findByStatusOrderByAppointmentTimeDesc(String status);

    List<Appointment> findAllByOrderByAppointmentTimeDesc();
}
