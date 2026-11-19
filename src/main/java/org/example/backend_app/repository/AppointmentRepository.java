package org.example.backend_app.repository;

import org.example.backend_app.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrderByAppointmentTimeDesc(Long userId);

    List<Appointment> findByStatusOrderByAppointmentTimeDesc(String status);

    List<Appointment> findAllByOrderByAppointmentTimeDesc();

    List<Appointment> findByAppointmentTimeBetweenOrderByAppointmentTimeDesc(LocalDateTime from,
                                                                              LocalDateTime to);

    List<Appointment> findByUserIdAndStatusOrderByAppointmentTimeDesc(Long userId, String status);

    boolean existsByPetIdAndAppointmentTimeAndStatusIn(Long petId,
                                                       LocalDateTime appointmentTime,
                                                       Collection<String> statuses);

    boolean existsByPetId(Long petId);
}
